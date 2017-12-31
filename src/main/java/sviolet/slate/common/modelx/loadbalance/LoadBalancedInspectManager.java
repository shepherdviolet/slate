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

import java.util.List;
import java.util.concurrent.Executor;

/**
 * 均衡负载--网络状态探测管理器(内置常驻调度线程一个)
 */
public class LoadBalancedInspectManager implements Destroyable {

    private static final long DEFAULT_INSPECT_INTERVAL = 20000L;

    private Logger logger = LoggerFactory.getLogger(getClass());

    private LoadBalancedHostManager hostManager;
    private List<LoadBalanceInspector> inspectors;

    private boolean closed = false;

    private long inspectInterval = DEFAULT_INSPECT_INTERVAL;
    private long inspectTimeout = DEFAULT_INSPECT_INTERVAL / 4;
    private long blockDuration = DEFAULT_INSPECT_INTERVAL * 2;

    private Executor dispatchThreadPool = ThreadPoolExecutorUtils.newInstance(1, 1, 60, "LoadBalancedInspectManager-dispatch-%d");
    private Executor inspectThreadPool = ThreadPoolExecutorUtils.newInstance(0, Integer.MAX_VALUE, 60, "LoadBalancedInspectManager-inspect-%d");

    public LoadBalancedInspectManager() {
        dispatchStart();
    }

    /**
     * 设置远端管理器
     * @param hostManager 远端管理器
     */
    public void setHostManager(LoadBalancedHostManager hostManager) {
        this.hostManager = hostManager;
    }

    /**
     * 设置网络状态探测器
     * @param inspectors 探测器
     */
    public void setInspectors(List<LoadBalanceInspector> inspectors) {
        this.inspectors = inspectors;
    }

    /**
     * 设置检测间隔
     * @param inspectInterval 检测间隔ms
     */
    public void setInspectInterval(long inspectInterval) {
        this.inspectInterval = inspectInterval;
        this.inspectTimeout = inspectInterval / 4;
        this.blockDuration = DEFAULT_INSPECT_INTERVAL * 2;
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

    private void dispatchStart() {
        dispatchThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                LoadBalancedHostManager hostManager;
                LoadBalancedHostManager.Host[] hostArray;
                while (!closed){
                    try {
                        Thread.sleep(inspectInterval);
                    } catch (InterruptedException ignored) {
                    }
                    hostManager = LoadBalancedInspectManager.this.hostManager;
                    if (hostManager == null || inspectors == null){
                        if (logger.isDebugEnabled()) {
                            logger.debug("Dispatch: no hostManager or inspectors, skip inspect");
                        }
                        continue;
                    }
                    hostArray = hostManager.getHostArray();
                    if (hostArray.length <= 0){
                        if (logger.isDebugEnabled()) {
                            logger.debug("Dispatch: hostArray is empty, skip inspect");
                        }
                        continue;
                    }
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

    private void inspect(final LoadBalancedHostManager.Host host) {
        inspectThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                List<LoadBalanceInspector> inspectors = LoadBalancedInspectManager.this.inspectors;
                if (inspectors == null){
                    logger.debug("Inspect: no inspectors, skip inspect");
                    return;
                }
                boolean block = false;
                for (LoadBalanceInspector inspector : inspectors){
                    if (!inspector.inspect(host.getUrl(), inspectTimeout)){
                        block = true;
                        break;
                    }
                }
                if (block){
                    host.block(blockDuration);
                    logger.debug("Inspect: block " + host.getUrl());
                }
            }
        });
    }

}
