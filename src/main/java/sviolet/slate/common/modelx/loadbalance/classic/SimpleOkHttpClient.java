/*
 * Copyright (C) 2015-2018 S.Violet
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
 * Project GitHub: https://github.com/shepherdviolet/slate-common
 * Email: shepherdviolet@163.com
 */

package sviolet.slate.common.modelx.loadbalance.classic;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import sviolet.slate.common.modelx.loadbalance.LoadBalancedHostManager;
import sviolet.slate.common.modelx.loadbalance.LoadBalancedInspectManager;
import sviolet.thistle.entity.Destroyable;

import java.io.Closeable;

/**
 * <p>简化版MultiHostOkHttpClient (Spring专用, 依赖spring-beans包)</p>
 *
 * <p>在MultiHostOkHttpClient的基础上, 封装了LoadBalancedHostManager和LoadBalancedInspectManager, 简化了配置, 免去了配置三个Bean的麻烦 <br>
 * 1.配置被简化, 如需高度定制, 请使用LoadBalancedHostManager + LoadBalancedInspectManager + MultiHostOkHttpClient <br>
 * 2.内置的LoadBalancedInspectManager采用TELNET方式探测后端(不可自定义探测方式)<br>
 * 3.屏蔽了setHostManager()方法, 调用会抛出异常<br>
 * 4.实现了DisposableBean, 在Spring容器中会自动销毁<br>
 * </p>
 *
 * <p>Java:</p>
 *
 * <pre>{@code
 *
 *      SimpleOkHttpClient client = new SimpleOkHttpClient()
 *              .setHosts("http://127.0.0.1:8081,http://127.0.0.1:8082")
 *              .setInitiativeInspectInterval(5000L)
 *              .setMaxThreads(200)
 *              .setMaxThreadsPerHost(200)
 *              .setPassiveBlockDuration(3000L)
 *              .setConnectTimeout(3000L)
 *              .setWriteTimeout(10000L)
 *              .setReadTimeout(10000L);
 *
 * }</pre>
 *
 * <p>Spring MVC:</p>
 *
 * <pre>{@code
 *
 *  <bean id="simpleOkHttpClient" class="sviolet.slate.common.modelx.loadbalance.classic.SimpleOkHttpClient">
 *      <property name="hosts" value="http://127.0.0.1:8081,http://127.0.0.1:8082"/>
 *      <property name="initiativeInspectInterval" value="10000"/>
 *      <property name="maxThreads" value="200"/>
 *      <property name="maxThreadsPerHost" value="200"/>
 *      <property name="passiveBlockDuration" value="3000"/>
 *      <property name="connectTimeout" value="3000"/>
 *      <property name="writeTimeout" value="10000"/>
 *      <property name="readTimeout" value="10000"/>
 *  </bean>
 *
 * }</pre>
 *
 *
 * @author S.Violet
 * @see MultiHostOkHttpClient
 *
 */
public class SimpleOkHttpClient extends MultiHostOkHttpClient implements Closeable, Destroyable, InitializingBean, DisposableBean {

    private LoadBalancedHostManager hostManager = new LoadBalancedHostManager();
    private LoadBalancedInspectManager inspectManager;

    private long initiativeInspectInterval = LoadBalancedInspectManager.DEFAULT_INSPECT_INTERVAL;
    private boolean verboseLog = false;

    public SimpleOkHttpClient() {
        super.setHostManager(hostManager);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        inspectManager = new LoadBalancedInspectManager()
                .setHostManager(hostManager)
                .setInspectInterval(initiativeInspectInterval)
                .setVerboseLog(verboseLog);
    }

    @Override
    public void close() {
        try {
            inspectManager.close();
        } catch (Exception ignore) {
        }
    }

    @Override
    public void onDestroy() {
        close();
    }

    @Override
    public void destroy() {
        close();
    }

    // Settings ///////////////////////////////////////////////////////////////////////////////////

    /**
     * [线程安全的/异步的]
     * 设置/刷新远端列表, 该方法可以反复调用设置新的后端(但不是同步生效)
     *
     * @param hosts 远端列表, 格式:"http://127.0.0.1:8081/,http://127.0.0.1:8082/"
     */
    public SimpleOkHttpClient setHosts(String hosts) {
        hostManager.setHosts(hosts);
        return this;
    }

    /**
     * 设置主动探测间隔 (主动探测器)
     * @param initiativeInspectInterval 检测间隔ms, > 0 , 建议 > 5000
     */
    public SimpleOkHttpClient setInitiativeInspectInterval(long initiativeInspectInterval) {
        this.initiativeInspectInterval = initiativeInspectInterval;
        return this;
    }

    /**
     * @deprecated 禁用该方法
     */
    @Override
    @Deprecated
    public MultiHostOkHttpClient setHostManager(LoadBalancedHostManager hostManager) {
        throw new IllegalStateException("setHostManager method can not invoke in SimpleOkHttpClient");
    }

    /**
     * 打印更多的日志, 默认关闭
     * @param verboseLog true:打印更多的调试日志, 默认关闭
     */
    @Override
    public MultiHostOkHttpClient setVerboseLog(boolean verboseLog) {
        super.setVerboseLog(verboseLog);
        this.verboseLog = verboseLog;
        return this;
    }
}
