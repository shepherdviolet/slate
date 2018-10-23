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
 * Project GitHub: https://github.com/shepherdviolet/slate
 * Email: shepherdviolet@163.com
 */

package sviolet.slate.common.x.net.loadbalance.classic;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import sviolet.slate.common.x.net.loadbalance.LoadBalancedHostManager;
import sviolet.slate.common.x.net.loadbalance.LoadBalancedInspectManager;
import sviolet.slate.common.x.net.loadbalance.inspector.HttpGetLoadBalanceInspector;
import sviolet.thistle.entity.common.Destroyable;

import java.io.Closeable;

/**
 * <p>简化版MultiHostOkHttpClient (Spring专用, 依赖spring-beans包)</p>
 *
 * <p>在MultiHostOkHttpClient的基础上, 封装了LoadBalancedHostManager和LoadBalancedInspectManager, 简化了配置, 免去了配置三个Bean的麻烦 <br>
 * 1.配置被简化, 如需高度定制, 请使用LoadBalancedHostManager + LoadBalancedInspectManager + MultiHostOkHttpClient <br>
 * 2.内置的LoadBalancedInspectManager采用TELNET方式探测后端(不可自定义探测方式, 但可以配置为HttpGet探测方式)<br>
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
 *              .setPassiveBlockDuration(6000L)
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
 *  <bean id="simpleOkHttpClient" class="sviolet.slate.common.x.net.loadbalance.classic.SimpleOkHttpClient">
 *      <property name="hosts" value="http://127.0.0.1:8081,http://127.0.0.1:8082"/>
 *      <property name="initiativeInspectInterval" value="10000"/>
 *      <property name="maxThreads" value="200"/>
 *      <property name="maxThreadsPerHost" value="200"/>
 *      <property name="passiveBlockDuration" value="6000"/>
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
    private boolean httpGetInspectorEnabled = false;
    private String httpGetInspectorUrlSuffix;
    private boolean verboseLog = false;
    private String tag = "";

    @Override
    public void afterPropertiesSet() throws Exception {
        //host manager
        super.setHostManager(hostManager);
        //inspect manager
        inspectManager = new LoadBalancedInspectManager()
                .setHostManager(hostManager)
                .setInspectInterval(initiativeInspectInterval)
                .setVerboseLog(verboseLog)
                .setTag(tag);
        if (httpGetInspectorEnabled) {
            inspectManager.setInspector(new HttpGetLoadBalanceInspector(httpGetInspectorUrlSuffix, initiativeInspectInterval / 4));
        }
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

    @Override
    public String toString() {
        return super.toString() + (inspectManager != null ? " Inspect [ " + inspectManager + " ]" : "");
    }

    /**
     * 文本方式输出当前远端列表和状态
     * @param prefix 文本前缀
     * @return 远端列表和状态
     */
    public String printHostsStatus(String prefix){
        return hostManager.printHostsStatus(prefix);
    }

    // Settings ///////////////////////////////////////////////////////////////////////////////////

    /**
     * @deprecated 禁用该方法
     */
    @Override
    @Deprecated
    public MultiHostOkHttpClient setHostManager(LoadBalancedHostManager hostManager) {
        throw new IllegalStateException("setHostManager method can not invoke in SimpleOkHttpClient");
    }

    /**
     * [线程安全/异步生效/可运行时修改]
     * 设置/刷新远端列表, 该方法可以反复调用设置新的后端(但不是同步生效)
     *
     * @param hosts 远端列表, 格式:"http://127.0.0.1:8081/,http://127.0.0.1:8082/"
     */
    public SimpleOkHttpClient setHosts(String hosts) {
        hostManager.setHosts(hosts);
        return this;
    }

    /**
     * [线程安全/异步生效/可运行时修改]
     * 设置/刷新远端列表, 该方法可以反复调用设置新的后端(但不是同步生效)
     *
     * @param hosts 远端列表
     */
    public SimpleOkHttpClient setHostArray(String[] hosts) {
        hostManager.setHostArray(hosts);
        return this;
    }

    /**
     * [线程安全/异步生效/可运行时修改]
     * 设置主动探测间隔 (主动探测器)
     * @param initiativeInspectInterval 检测间隔ms, > 0 , 建议 > 5000
     */
    public SimpleOkHttpClient setInitiativeInspectInterval(long initiativeInspectInterval) {
        this.initiativeInspectInterval = initiativeInspectInterval;
        if (inspectManager != null) {
            inspectManager.setInspectInterval(initiativeInspectInterval);
        }
        return this;
    }

    /**
     * [线程安全/异步生效/可运行时修改]
     * 打印更多的日志, 默认关闭
     * @param verboseLog true:打印更多的调试日志, 默认关闭
     */
    @Override
    public MultiHostOkHttpClient setVerboseLog(boolean verboseLog) {
        super.setVerboseLog(verboseLog);
        this.verboseLog = verboseLog;
        if (inspectManager != null) {
            inspectManager.setVerboseLog(verboseLog);
        }
        return this;
    }

    /**
     * [可运行时修改]
     * 如果设置为false(默认), 当所有远端都被阻断时, nextHost方法返回一个后端.
     * 如果设置为true, 当所有远端都被阻断时, nextHost方法返回null.
     */
    public MultiHostOkHttpClient setReturnNullIfAllBlocked(boolean returnNullIfAllBlocked) {
        hostManager.setReturnNullIfAllBlocked(returnNullIfAllBlocked);
        return this;
    }

    /**
     * 设置客户端的标识
     * @param tag 标识
     */
    @Override
    public MultiHostOkHttpClient setTag(String tag) {
        super.setTag(tag);
        hostManager.setTag(tag);
        this.tag = tag;
        if (inspectManager != null) {
            inspectManager.setTag(tag);
        }
        return this;
    }

    /**
     * 将主动探测器从TELNET型修改为HTTP GET型, 运行时修改该参数无效!
     * @param urlSuffix 探测页面URL(例如:http://127.0.0.1:8080/health, 则在此处设置/health)
     */
    public SimpleOkHttpClient setHttpGetInspector(String urlSuffix) {
        this.httpGetInspectorUrlSuffix = urlSuffix;
        this.httpGetInspectorEnabled = true;
        return this;
    }

}
