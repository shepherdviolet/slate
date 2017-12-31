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
import sviolet.thistle.entity.Destroyable;
import sviolet.thistle.util.common.ThreadPoolExecutorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 均衡负载--网络状态探测管理器(内置常驻调度线程一个), 可以使用close方法关闭(关闭后不再探测网络状态)
 */
public class LoadBalancedInspectManager implements Destroyable {

    private static final long DEFAULT_INSPECT_INTERVAL = 20000L;

    private Logger logger = LoggerFactory.getLogger(getClass());

    private LoadBalancedHostManager hostManager;
    private List<LoadBalanceInspector> inspectors;

    private boolean closed = false;
    private boolean verboseLog = false;

    private long inspectInterval = DEFAULT_INSPECT_INTERVAL;
    private long inspectTimeout = DEFAULT_INSPECT_INTERVAL / 4;
    private long blockDuration = DEFAULT_INSPECT_INTERVAL * 2;

    private Executor dispatchThreadPool = ThreadPoolExecutorUtils.newInstance(1, 1, 60, "LoadBalancedInspectManager-dispatch-%d");
    private Executor inspectThreadPool = ThreadPoolExecutorUtils.newInstance(0, Integer.MAX_VALUE, 60, "LoadBalancedInspectManager-inspect-%d");

    public LoadBalancedInspectManager() {
        //开始探测
        dispatchStart();
    }

    /**
     * 设置远端管理器(必须)
     * @param hostManager 远端管理器
     */
    public void setHostManager(LoadBalancedHostManager hostManager) {
        this.hostManager = hostManager;
    }

    /**
     * 设置网络状态探测器(必须)
     * @param inspector 探测器
     */
    public void setInspector(LoadBalanceInspector inspector){
        this.inspectors = new ArrayList<>(1);
        this.inspectors.add(inspector);
    }

    /**
     * 设置网络状态探测器(必须)
     * @param inspectors 探测器
     */
    public void setInspectors(List<LoadBalanceInspector> inspectors) {
        this.inspectors = inspectors;
    }

    /**
     * 设置探测间隔
     * @param inspectInterval 检测间隔ms
     */
    public void setInspectInterval(long inspectInterval) {
        //探测间隔
        this.inspectInterval = inspectInterval;
        //探测超时
        this.inspectTimeout = inspectInterval / 4;
        //故障时远端被阻断的时间
        this.blockDuration = inspectInterval * 2;
    }

    /**
     * @param verboseLog true:打印更多的调试日志
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
                if (logger.isDebugEnabled()) {
                    logger.debug("Dispatch: start");
                }
                LoadBalancedHostManager hostManager;
                LoadBalancedHostManager.Host[] hostArray;
                while (!closed){
                    //间隔
                    try {
                        Thread.sleep(inspectInterval);
                    } catch (InterruptedException ignored) {
                    }
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
                    if (verboseLog) {
                        StringBuilder stringBuilder = new StringBuilder("Host status:");
                        Map<String, Boolean> hostsStatus = hostManager.getHostsStatus();
                        for (Map.Entry<String, Boolean> entry : hostsStatus.entrySet()) {
                            stringBuilder.append("\n>>>Host>>>");
                            stringBuilder.append(entry.getKey());
                            stringBuilder.append(":");
                            stringBuilder.append(entry.getValue());
                        }
                        logger.debug(stringBuilder.toString());
                    }
                    //探测所有远端
                    for (LoadBalancedHostManager.Host host : hostArray){
                        inspect(host);
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
                if (verboseLog) {
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
                        logger.info("Inspect: block " + host.getUrl());
                    }
                }
                if (verboseLog) {
                    logger.debug("Inspect: inspected " + host.getUrl());
                }
            }
        });
    }

}
