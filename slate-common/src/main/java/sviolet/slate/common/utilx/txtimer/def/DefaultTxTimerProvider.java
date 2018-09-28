package sviolet.slate.common.utilx.txtimer.def;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sviolet.slate.common.utilx.txtimer.TxTimerProvider;
import sviolet.thistle.model.concurrent.StringHashLocks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>默认实现了交易耗时的统计, 并通过日志定时输出报告. 可以使用ThistleSpi替换实现.</p>
 *
 * @author S.Violet
 */
public class DefaultTxTimerProvider implements TxTimerProvider {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTxTimerProvider.class);

    /**
     * [基本设置]日志报告输出间隔, 单位:分钟, [3-60]
     */
    static final int REPORT_INTERVAL;

    /**
     * [调优设置]日志每次输出的最大行数, 大于该行数会分页, 默认20
     */
    static final int REPORT_LINES;
    /**
     * [调优设置]内部Map的初始大小, 大于观测点数量为宜
     */
    static final int MAP_INIT_CAP;
    /**
     * [调优设置]StringHashLocks的锁数量
     */
    static final int HASH_LOCK_NUM;
    /**
     * [调优设置]内部一些非锁更新操作的最大尝试次数
     */
    static final int UPDATE_MAX_ATTEMPTS;

    static {
        REPORT_INTERVAL = getIntFromProperty("slate.txtimer.reportinterval", 30);
        REPORT_INTERVAL_MILLIS = REPORT_INTERVAL * 60 * 1000;
        if (REPORT_INTERVAL < 2 || REPORT_INTERVAL > 60) {
            throw new IllegalArgumentException("slate.txtimer.reportinterval must >= 2 and <= 60 (minus)");
        }

        REPORT_LINES = getIntFromProperty("slate.txtimer.reportlines", 20);
        MAP_INIT_CAP = getIntFromProperty("slate.txtimer.mapinitcap", 1024);
        HASH_LOCK_NUM = getIntFromProperty("slate.txtimer.hashlocknum", 32);
        UPDATE_MAX_ATTEMPTS = getIntFromProperty("slate.txtimer.updateattemps", 10);
    }

    private static int getIntFromProperty(String key, int def) {
        try {
            return Integer.parseInt(System.getProperty(key, String.valueOf(def)));
        } catch (Exception e) {
            logger.error("Error while parsing -D" + key + " to int, using " + def + " by default", e);
            return def;
        }
    }

    /* *************************************************************************************************************** */

    //日志报告输出间隔的毫秒数
    static final int REPORT_INTERVAL_MILLIS;
    //每分钟的毫秒数
    static final long MINUTE_MILLIS = 60L * 1000L;

    //用于start和stop之间的上下文传递
    private ThreadLocal<Record> record = new ThreadLocal<>();

    //组Map
    Map<String, Group> groups = new ConcurrentHashMap<>(MAP_INIT_CAP);
    //stop时记录找不到的计数器
    AtomicInteger missingCount = new AtomicInteger(0);

    //锁
    StringHashLocks locks = new StringHashLocks(HASH_LOCK_NUM);
    //日志输出器
    Reporter reporter = new Reporter(this);

    @Override
    public void start(String groupName, String transactionName) {
        //从ThreadLocal获取上下文, 若存在则不正常
        Record record = this.record.get();
        if (record != null) {
            //重复调用start没有做stop, 忘记做stop, 有可能会导致这个问题
            Transaction transaction = getGroup(record.getGroupName()).getTransaction(record.getTransactionName());
            transaction.duplicate();
        }
        //获得交易记录实例
        Transaction transaction = getGroup(groupName).getTransaction(transactionName);
        //标记为正在执行
        transaction.running();
        //在ThreadLocal记录上下文
        record = new Record(groupName, transactionName);
        this.record.set(record);
    }

    @Override
    public void stop() {
        //从ThreadLocal获取上下文, 若不存在则不正常
        Record record = this.record.get();
        if (record == null) {
            //重复调用stop, 没做start直接做stop, 有可能会导致这个问题
            missingCount.incrementAndGet();
            return;
        }
        //置空
        this.record.set(null);
        //计算时长
        long elapse = System.currentTimeMillis() - record.getStartTime();
        //获得交易记录实例
        Transaction transaction = getGroup(record.getGroupName()).getTransaction(record.getTransactionName());
        //标记为完成交易, 并记录时间
        transaction.finish(System.currentTimeMillis(), elapse);
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public boolean canBeGet() {
        return true;
    }

    private Group getGroup(String groupName) {
        Group group = groups.get(groupName);
        if (group == null) {
            //用StringHashLocks分散碰撞的可能性
            ReentrantLock lock = locks.getLock(groupName);
            try {
                lock.lock();
                group = groups.get(groupName);
                if (group == null) {
                    group = new Group(this);
                    groups.put(groupName, group);
                }
            } finally {
                lock.unlock();
            }
        }
        return group;
    }

}
