package sviolet.slate.common.model.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DiscardableSingletonPool使用示例
 *
 * 假设我们要实现一个服务, 维护几个客户端, 用来向不同的后端发送请求.
 * 1.使用DiscardableSingletonPool维护客户端实例.
 * 2.当客户端抛出BadHostException异常时, 丢弃客户端重新创建.
 * 3.设置"强制销毁丢弃超过10分钟的对象"
 * 4.设置"每小时丢弃不怎么用的对象 (50分钟没用的对象)"
 * 5.设置"每天2点丢弃所有对象"
 * 6.定时输出统计日志
 *
 * @author shepherdviolet
 */
@Service
public class DiscardableSingletonPoolSampleService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * [可选: 创建参数]
     */
    private Map<String, ClientCreateParam> clientCreateParams = new HashMap<>();

    /**
     * [可选: 创建参数]
     *
     * 注入所有客户端创建参数ClientCreateParam (注入所有类型为ClientCreateParam的Spring Bean)
     */
    @Autowired
    public void setClientCreateParams(Map<String, ClientCreateParam> clientCreateParams) {
        this.clientCreateParams = clientCreateParams;
    }

    /**
     * [必要: 正确使用对象池]
     *
     * 向指定客户端发送请求
     *
     * @param key 客户端名称
     * @param request 请求
     * @return 响应
     */
    public Response post(String key, Request request) {

        // 获取客户端创建参数
        ClientCreateParam createParam = clientCreateParams.get(key);
        if (createParam == null) {
            throw new RuntimeException("Undefined client " + key);
        }

        // 写法1: 重要!!! 用try-with-resource写法保证在使用完毕后释放引用
        // 从对象池获取客户端, 传入客户端名称和创建参数
        try (DiscardableSingletonPool.InstanceProvider<Client> instanceProvider = clientPool.getInstanceProvider(key, createParam)){
            // 从InstanceProvider获取对象实例使用
            Client client = instanceProvider.getInstance();
            try {
                // 使用客户端发送请求
                return client.post(request);
            } catch (BadHostException e) {
                // 假设BadHostException异常表示后端故障, 必须重新创建客户端实例连接
                // 所以这里丢弃客户端
                instanceProvider.discard();
                // 这里选择继续抛出异常
                throw e;
            }
        }

        // 写法2: 重要!!! 普通写法, 在使用完毕后释放引用
        // 从对象池获取客户端, 传入客户端名称和创建参数
//        DiscardableSingletonPool.InstanceProvider<Client> instanceProvider = clientPool.getInstanceProvider(key, createParam);
//        try {
//            // 从InstanceProvider获取对象实例使用
//            Client client = instanceProvider.getInstance();
//            // 使用客户端发送请求
//            return client.post(request);
//        } catch (BadHostException e) {
//            // 假设BadHostException异常表示后端故障, 必须重新创建客户端实例连接
//            // 所以这里丢弃客户端
//            instanceProvider.discard();
//            // 这里选择继续抛出异常
//            throw e;
//        } finally {
//            instanceProvider.close();
//        }

    }

    /**
     * [必要: 配置对象池]
     */
    private final DiscardableSingletonPool<Client, ClientCreateParam> clientPool = new DiscardableSingletonPool<>(new DiscardableSingletonPool.InstanceManager<Client, ClientCreateParam>() {

        /**
         * [必要: 配置对象池]
         *
         * 创建对象
         *
         * @param key 对象名称
         * @param createParam 创建参数, 可为空, 由DiscardableSingletonPool#getInstanceProvider传入
         * @return 创建出来的对象
         * @throws Exception 这里抛出的异常会被封装为InstanceCreateException抛出
         */
        @Override
        public Client createInstance(String key, ClientCreateParam createParam) throws Exception {
            // 在这里实现客户端的创建
            return new Client(key, createParam);
        }

        /**
         * [必要: 配置对象池]
         *
         * 销毁对象, 如果这里抛出异常, 会回调onDestroyError方法
         *
         * @param info 对象信息
         * @param instance 对象实例(销毁它)
         */
        @Override
        public void destroyInstance(DiscardableSingletonPool.InstanceInfo info, Client instance) throws Exception {
            // 在这里实现客户端的销毁
            instance.destroy();
        }

        /**
         * [必要: 配置对象池]
         *
         * 处理destroyInstance方法抛出的异常, 一般就打印日志
         *
         * @param info 对象信息
         * @param instance 对象实例
         * @param t 异常
         */
        @Override
        public void onDestroyError(DiscardableSingletonPool.InstanceInfo info, Client instance, Throwable t) {
            // 一般就打个日志
            logger.error("Error when destroy Client: " + info, t);
        }

    });

    /**
     * [可选: 强制销毁]
     *
     * 强制销毁丢弃超过10分钟的对象.
     *
     * [温馨提示] 如果你正确地在每次使用完对象后释放引用, 这个参数是没有必要设置的. 如果你担心自己不小心持有对象, 忘记释放引用的话, 那就设置这个参数以防万一.
     * 本使用示例保证了每次使用都释放引用, 所以这个设置在这里没有意义, 供需要的人参考.
     *
     * 正常情况下, 被丢弃的对象由"销毁器"负责, 在它们使用完毕后(引用计数为0)销毁. 设置了这个参数后, "销毁器"在判断对象是否可以销毁时,
     * 追加了一种情况, 若 "当前时间 - 丢弃时间 > forceDestroyDiscardedInstanceAfterMillis" 则强制销毁对象, 无视对象的引用计数情况.
     *
     * 注意!! 仅仅设置这个参数并不能保证对象在"到期"后立刻被销毁, 因为"销毁器"触发有条件(见6.2). 如果对象"到期"后, 没有人触发丢弃(discard),
     * 也没有被丢弃的对象引用计数归0, 那就只能手动触发"销毁器"了.
     *
     * 所以, 这里配套了一个定时器(见notifyDestroyer方法), 定期调用DiscardableSingletonPool#notifyDestroyDiscardedInstances方法,
     * 唤醒"销毁器"将"已到期"的对象强制销毁.
     */
    @Value("${sample-service.force-destroy-discarded-instance-after-millis:600000}")
    public void setForceDestroyDiscardedInstanceAfterMillis(long forceDestroyDiscardedInstanceAfterMillis){
        clientPool.setForceDestroyDiscardedInstanceAfterMillis(forceDestroyDiscardedInstanceAfterMillis);
    }

    /**
     * [可选: 强制销毁]
     *
     * 强制销毁丢弃超过10分钟的对象. 每隔10分钟通知一次"销毁器".
     *
     * [温馨提示] 如果你正确地在每次使用完对象后释放引用, 这个参数是没有必要设置的. 如果你担心自己不小心持有对象, 忘记释放引用的话, 那就设置这个参数以防万一.
     * 本使用示例保证了每次使用都释放引用, 所以这个设置在这里没有意义, 供需要的人参考.
     *
     * 配合setForceDestroyDiscardedInstanceAfterMillis方法实现可靠的"强制销毁"功能.
     */
    @Scheduled(fixedRateString = "${sample-service.notify-destroy-discarded-instances:600000}")
    public void notifyDestroyer(){
        clientPool.notifyDestroyDiscardedInstances();
    }

    /**
     * [可选: 定时丢弃]
     *
     * 每小时丢弃不怎么用的对象 (50分钟没用的对象)
     */
    private long discardUselessExpireTime;

    /**
     * [可选: 定时丢弃]
     *
     * 每小时丢弃不怎么用的对象 (50分钟没用的对象)
     */
    @Value("${sample-service.discard-useless-expire-time:3000000}")
    public void setDiscardUselessExpireTime(long discardUselessExpireTime) {
        this.discardUselessExpireTime = discardUselessExpireTime;
    }

    /**
     * [可选: 定时丢弃]
     *
     * 每小时丢弃不怎么用的对象 (50分钟没用的对象)
     */
    @Scheduled(fixedRateString = "${sample-service.discard-useless-client-rate:3600000}")
    public void discardUselessClient(){
        clientPool.discard((instance, info) -> (System.currentTimeMillis() - info.getLastUsedTimeMillis()) > discardUselessExpireTime);
    }

    /**
     * [可选: 定时全部丢弃]
     *
     * 每天2点丢弃所有对象
     */
    @Scheduled(cron = "${sample-service.discard-all-client-cron:0 0 2 * * ?}")
    public void discardAllClient(){
        clientPool.discardAll();
    }

    /**
     * [可选: 统计信息输出]
     *
     * 定时输出统计信息
     */
    @Scheduled(fixedRateString = "${sample-service.statistic-info:10000}")
    public void printPoolStatisticInfo(){
        if (logger.isInfoEnabled()) {
            logger.info("sample-service | pool statistic: " + clientPool);
        }
    }




    // 以下非使用示例, 仅用于测试, 切勿复制粘贴 ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 以下非使用示例, 仅用于测试, 切勿复制粘贴 ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 以下非使用示例, 仅用于测试, 切勿复制粘贴 ///////////////////////////////////////////////////////////////////////////////////////////////////////////




    // [MOCK] 测试的时候比较客户端是不是真的被销毁了
    public static final AtomicInteger clientCount = new AtomicInteger(0);
    public static final AtomicInteger clientDestroyCount = new AtomicInteger(0);
    // [MOCK] 测试的时候看客户端是不是走对, 没走到别的客户端里
    public static final Map<String, AtomicInteger> clientPostCountMap = new ConcurrentHashMap<>();
    public static final Map<String, AtomicInteger> clientPostSuccessCountMap = new ConcurrentHashMap<>();

    /**
     * [MOCK] 测试的时候比较客户端是不是真的被销毁了, 测试的时候看客户端是不是走对, 没走到别的客户端里
     */
    public void printClientStatistic() {
        logger.info("sample-service | client statistic: {" +
                "clientCount='" + clientCount.get() + '\'' +
                ", clientDestroyCount=" + clientDestroyCount.get() +
                ", clientPostCountMap=" + clientPostCountMap +
                ", clientPostSuccessCountMap=" + clientPostSuccessCountMap +
                '}');
    }

    /**
     * [MOCK]
     *
     * 错误方式!!! 切勿模仿!!!
     * 错误方式!!! 切勿模仿!!!
     * 错误方式!!! 切勿模仿!!!
     *
     * @param key 客户端名称
     * @param request 请求
     * @return 响应
     */
    public Response incorrectWayToPost(String key, Request request) {

        // 获取客户端创建参数
        ClientCreateParam createParam = clientCreateParams.get(key);
        if (createParam == null) {
            throw new RuntimeException("Undefined client " + key);
        }

        /*
         * 错误方式!!! 切勿模仿!!! 不及时释放引用(DiscardableSingletonPool.InstanceProvider#close)会导致对象在被丢弃后无法被销毁!!!
         * 错误方式!!! 切勿模仿!!! 不及时释放引用(DiscardableSingletonPool.InstanceProvider#close)会导致对象在被丢弃后无法被销毁!!!
         * 错误方式!!! 切勿模仿!!! 不及时释放引用(DiscardableSingletonPool.InstanceProvider#close)会导致对象在被丢弃后无法被销毁!!!
         *
         * 这里的代码用于测试, 实际千万别这么写!!!
         */
        DiscardableSingletonPool.InstanceProvider<Client> instanceProvider = clientPool.getInstanceProvider(key, createParam);
        Client client = instanceProvider.getInstance();
        try {
            return client.post(request);
        } catch (BadHostException e) {
            instanceProvider.discard();
            throw e;
        }

    }

    /**
     * [MOCK] 客户端
     */
    public static class Client {

        private final String key;
        private final int postSuccessRate;
        private volatile boolean closed = false;

        public Client(String key, ClientCreateParam createParam) {
            // 没送就报错, 能测试createParam是否真的传入
            if (createParam == null) {
                throw new RuntimeException("createParam == null");
            }

            // 测试创建失败
            if ("error".equals(key)) {
                throw new RuntimeException("Client create failed");
            }

            this.key = key;
            this.postSuccessRate = createParam.getPostSuccessRate();

            clientCount.getAndIncrement();
        }

        /**
         * 模拟发送请求
         */
        public Response post(Request request) {
            if (closed) {
                throw new ClosedClientException("ERROR, invoke a closed client");
            }

            clientPostCountMap.computeIfAbsent(key, k -> new AtomicInteger(0)).getAndIncrement();

            if (request == null) {
                throw new RuntimeException("request == null");
            }

            // 模拟IO
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(200));
            } catch (InterruptedException ignore) {
            }

            if (closed) {
                throw new ClosedClientException("ERROR, invoke a closed client");
            }

            // 模拟后端故障, 这种错误需要丢弃客户端实例(重建)
            if (ThreadLocalRandom.current().nextInt(10000) + 1 > postSuccessRate) {
                throw new BadHostException();
            }

            // POST成功计数, 会少于post次数, 因为有失败率
            clientPostSuccessCountMap.computeIfAbsent(key, k -> new AtomicInteger(0)).getAndIncrement();

            return new Response();
        }

        /**
         * 模拟销毁
         */
        public void destroy() throws IOException {
            clientDestroyCount.getAndIncrement();
            closed = true;
        }

    }

    /**
     * [MOCK] 客户端创建参数
     */
    public static class ClientCreateParam {

        private final int postSuccessRate;

        /**
         * @param postSuccessRate [0, 10000]
         */
        public ClientCreateParam(int postSuccessRate) {
            this.postSuccessRate = postSuccessRate;
        }

        public int getPostSuccessRate() {
            return postSuccessRate;
        }

    }

    /**
     * [MOCK] 请求
     */
    public static class Request {

    }

    /**
     * [MOCK] 响应
     */
    public static class Response {

    }

    /**
     * [MOCK] 假设BadHostException异常表示后端故障, 必须重新创建客户端实例连接
     */
    public static class BadHostException extends RuntimeException {

    }

    /**
     * [MOCK] 测试有没有调用被销毁实例的情况
     */
    public static class ClosedClientException extends RuntimeException {

        public ClosedClientException(String message) {
            super(message);
        }

    }

}
