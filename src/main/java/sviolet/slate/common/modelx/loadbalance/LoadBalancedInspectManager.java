/*
 * Copyright (C) 2015-2017 S.Violet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project GitHub: https://github.com/shepherdviolet/thistle
 * Email: shepherdviolet@163.com
 */

package sviolet.slate.common.modelx.loadbalance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sviolet.slate.common.modelx.loadbalance.inspector.TelnetLoadBalanceInspector;
import sviolet.thistle.entity.Destroyable;
import sviolet.thistle.util.concurrent.ThreadPoolExecutorUtils;
import sviolet.thistle.util.lifecycle.DestroyableManageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * <p>均衡负载--网络状态探测管理器</p>
 *
 * <p>
 *     注意!!!!!!<br>
 *     1.如果你是Servlet项目, 可以注册sviolet.slate.common.helperx.servlet.SlateServletContextListener监听器, 监听器会帮你
 *     自动销毁本探测器.<br>
 *     2.如果不是Servlet项目或没有注册监听器, 请在服务停止的时候调用close()方法销毁本实例, 以释放线程池.<br>
 * </p>
 *
 * <pre>{@code
 *      //实例化
 *      LoadBalancedInspectManager inspectManager = new LoadBalancedInspectManager();
 *      //设置要探测的远端管理器(必须)
 *      inspectManager.setHostManager(hostManager);
 *      //探测间隔(阻断时长为该值的两倍, 探测超时为该值的1/4)
 *      inspectManager.setInspectInterval(10000L);
 *      //设置探测器
 *      inspectManager.setInspector(new TelnetLoadBalanceInspector());
 *      //允许输出调试日志
 *      inspectManager.setVerboseLog(true);
 * }</pre>
 *
 * <pre>{@code
 *      //重要:关闭探测器(停止线程)
 *      inspectManager.close();
 * }</pre>
 *
 * @author S.Violet
 */
public class LoadBalancedInspectManager implements Destroyable {

    private static final long DEFAULT_INSPECT_INTERVAL = 10000L;

    private Logger logger = LoggerFactory.getLogger(getClass());

    private LoadBalancedHostManager hostManager;
    private List<LoadBalanceInspector> inspectors = new ArrayList<>(1);

    private boolean closed = false;
    private boolean verboseLog = false;

    private long inspectInterval = DEFAULT_INSPECT_INTERVAL;
    private long inspectTimeout = DEFAULT_INSPECT_INTERVAL / 4;
    private long blockDuration = DEFAULT_INSPECT_INTERVAL * 2;

    private ExecutorService dispatchThreadPool = ThreadPoolExecutorUtils.newInstance(1, 1, 60, "LoadBalancedInspectManager-dispatch-%d");
    private ExecutorService inspectThreadPool = ThreadPoolExecutorUtils.newInstance(0, Integer.MAX_VALUE, 60, "LoadBalancedInspectManager-inspect-%d");

    public LoadBalancedInspectManager() {
        //默认telnet探测器
        inspectors.add(new TelnetLoadBalanceInspector());
        //开始探测
        dispatchStart();
        //注册到管理器, 便于集中销毁
        DestroyableManageUtils.register(this);
    }

    /**
     * 设置远端管理器(必须)
     * @param hostManager 远端管理器
     */
    public void setHostManager(LoadBalancedHostManager hostManager) {
        this.hostManager = hostManager;
    }

    /**
     * 设置网络状态探测器, 如果不设置默认为telnet探测器
     * @param inspector 探测器
     */
    public void setInspector(LoadBalanceInspector inspector){
        this.inspectors = new ArrayList<>(1);
        this.inspectors.add(inspector);
    }

    /**
     * 设置网络状态探测器, 如果不设置默认为telnet探测器
     * @param inspectors 探测器
     */
    public void setInspectors(List<LoadBalanceInspector> inspectors) {
        this.inspectors = inspectors;
    }

    /**
     * 设置探测间隔
     * @param inspectInterval 检测间隔ms, > 0 , 建议 > 5000
     */
    public void setInspectInterval(long inspectInterval) {
        if (inspectInterval <= 0){
            throw new IllegalArgumentException("inspectInterval must > 0 (usually > 5000)");
        }
        //探测间隔
        this.inspectInterval = inspectInterval;
        //探测超时
        this.inspectTimeout = inspectInterval / 4;
        //故障时远端被阻断的时间
        this.blockDuration = inspectInterval * 2;
    }

    /**
     * @param verboseLog true:打印更多的调试日志, 默认关闭
     */
    public void setVerboseLog(boolean verboseLog) {
        this.verboseLog = verboseLog;
    }

    /**
     * 关闭探测器(关闭调度线程)
     */
    public void close(){
        onDestroy();
    }

    @Override
    public void onDestroy() {
        closed = true;
        try {
            dispatchThreadPool.shutdownNow();
        } catch (Throwable ignore){
        }
        try {
            inspectThreadPool.shutdownNow();
        } catch (Throwable ignore){
        }
        logger.info("Destroyed:" + this);
    }

    protected boolean isBlockIfInspectorError(){
        return true;
    }

    /**
     * 调度线程启动
     */
    private void dispatchStart() {
        dispatchThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(inspectInterval);
                } catch (InterruptedException ignored) {
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Dispatch: start");
                }
                LoadBalancedHostManager hostManager;
                LoadBalancedHostManager.Host[] hostArray;
                while (!closed){
                    //持有当前的hostManager
                    hostManager = LoadBalancedInspectManager.this.hostManager;
                    //检查是否配置
                    if (hostManager == null || inspectors == null){
                        if (logger.isWarnEnabled()) {
                            logger.warn("Dispatch: no hostManager or inspectors, skip inspect");
                        }
                        continue;
                    }
                    //获取远端列表
                    hostArray = hostManager.getHostArray();
                    if (hostArray.length <= 0){
                        if (logger.isWarnEnabled()) {
                            logger.warn("Dispatch: hostArray is empty, skip inspect");
                        }
                        continue;
                    }
                    //打印当前远端状态
                    if (verboseLog && logger.isDebugEnabled()) {
                        logger.debug(hostManager.printHostsStatus("Host status (before inspect):"));
                    }
                    //探测所有远端
                    for (LoadBalancedHostManager.Host host : hostArray){
                        inspect(host);
                    }
                    //间隔
                    try {
                        Thread.sleep(inspectInterval);
                    } catch (InterruptedException ignored) {
                    }
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Dispatch: closed");
                }
            }
        });
    }

    /**
     * 开始异步探测
     */
    private void inspect(final LoadBalancedHostManager.Host host) {
        inspectThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                if (verboseLog && logger.isDebugEnabled()) {
                    logger.debug("Inspect: inspecting " + host.getUrl());
                }
                //持有探测器
                List<LoadBalanceInspector> inspectors = LoadBalancedInspectManager.this.inspectors;
                if (inspectors == null){
                    if (logger.isWarnEnabled()) {
                        logger.warn("Inspect: no inspectors, skip inspect");
                    }
                    return;
                }
                //只要有一个探测器返回false, 就阻断远端
                boolean block = false;
                for (LoadBalanceInspector inspector : inspectors){
                    /*
                     * 注意:探测器必须在指定的timeout时间内探测完毕, 不要过久的占用线程,
                     * 尽量处理掉所有异常, 如果抛出异常, 视为探测失败, 阻断远端
                     */
                    try {
                        if (!inspector.inspect(host.getUrl(), inspectTimeout)) {
                            block = true;
                            break;
                        }
                    } catch (Throwable t) {
                        if (logger.isErrorEnabled()){
                            logger.error("Inspect: error, url " + host.getUrl() + ", in " + inspector.getClass(), t);
                        }
                        if (isBlockIfInspectorError()) {
                            block = true;
                            break;
                        }
                    }
                }
                //阻断
                if (block){
                    host.block(blockDuration);
                    if (logger.isInfoEnabled()) {
                        logger.info("Inspect: block " + host.getUrl() + " " + blockDuration);
                    }
                }
                if (verboseLog && logger.isDebugEnabled()) {
                    logger.debug("Inspect: inspected " + host.getUrl());
                }
            }
        });
    }

}
