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
 * Project GitHub: https://github.com/shepherdviolet/slate-common
 * Email: shepherdviolet@163.com
 */

package sviolet.slate.common.modelx.loadbalance.classic;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sviolet.slate.common.modelx.loadbalance.LoadBalancedHostManager;
import sviolet.thistle.util.conversion.ByteUtils;
import sviolet.thistle.util.judge.CheckUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>支持均衡负载的OkHttpClient(简单的示例模板, 建议自行实现)</p>
 *
 * <p>Java:</p>
 *
 * <pre>{@code
 *
 *      LoadBalancedHostManager hostManager = new LoadBalancedHostManager();
 *      hostManager.setHostArray(new String[]{
 *          "http://127.0.0.1:8080",
 *          "http://127.0.0.1:8081"
 *      });
 *
 *      LoadBalancedInspectManager inspectManager = new LoadBalancedInspectManager();
 *      inspectManager.setHostManager(hostManager);
 *      inspectManager.setInspectInterval(5000L);
 *      inspectManager.setInspector(new TelnetLoadBalanceInspector());
 *
 *      LoadBalancedOkHttpClient client = new LoadBalancedOkHttpClient();
 *      client.setHostManager(hostManager);
 *      client.setMaxThreads(200);
 *      client.setMaxThreadsPerHost(200);
 *      client.setPassiveBlockDuration(3000L);
 *      client.setConnectTimeout(3000L);
 *      client.setWriteTimeout(10000L);
 *      client.setReadTimeout(10000L);
 *      //client.setProxy("127.0.0.1:17711");
 *
 * }</pre>
 *
 * <p>Spring MVC: 注册了SlateServletContextListener的场合</p>
 *
 * <pre>{@code
 *
 *  <bean id="loadBalancedHostManager" class="sviolet.slate.common.modelx.loadbalance.LoadBalancedHostManager">
 *      <property name="hosts" value="http://127.0.0.1:8081,http://127.0.0.1:8082"/>
 *  </bean>
 *
 *  <bean id="loadBalancedInspector" class="sviolet.slate.common.modelx.loadbalance.LoadBalancedInspectManager">
 *      <property name="hostManager" ref="loadBalancedHostManager"/>
 *      <property name="inspectInterval" value="10000"/>
 *  </bean>
 *
 *  <bean id="loadBalancedOkHttpClient" class="sviolet.slate.common.modelx.loadbalance.classic.LoadBalancedOkHttpClient">
 *      <property name="hostManager" ref="loadBalancedHostManager"/>
 *      <property name="maxThreads" ref="200"/>
 *      <property name="maxThreadsPerHost" ref="200"/>
 *      <property name="passiveBlockDuration" value="3000"/>
 *      <property name="connectTimeout" value="3000"/>
 *      <property name="writeTimeout" value="10000"/>
 *      <property name="readTimeout" value="10000"/>
 *  </bean>
 *
 * }</pre>
 *
 * <p>Spring MVC: 没注册SlateServletContextListener的场合, 需要设置destroy-method="close"</p>
 *
 * <pre>{@code
 *
 *  <bean id="loadBalancedHostManager" class="sviolet.slate.common.modelx.loadbalance.LoadBalancedHostManager">
 *      <property name="hosts" value="http://127.0.0.1:8081,http://127.0.0.1:8082"/>
 *  </bean>
 *
 *  <bean id="loadBalancedInspector" class="sviolet.slate.common.modelx.loadbalance.LoadBalancedInspectManager"
 *      destroy-method="close">
 *      <property name="hostManager" ref="loadBalancedHostManager"/>
 *      <property name="inspectInterval" value="10000"/>
 *  </bean>
 *
 *  <bean id="loadBalancedOkHttpClient" class="sviolet.slate.common.modelx.loadbalance.classic.LoadBalancedOkHttpClient">
 *      <property name="hostManager" ref="loadBalancedHostManager"/>
 *      <property name="maxThreads" ref="200"/>
 *      <property name="maxThreadsPerHost" ref="200"/>
 *      <property name="passiveBlockDuration" value="3000"/>
 *      <property name="connectTimeout" value="3000"/>
 *      <property name="writeTimeout" value="10000"/>
 *      <property name="readTimeout" value="10000"/>
 *  </bean>
 *
 * }</pre>
 *
 * @author S.Violet
 */
public class LoadBalancedOkHttpClient {

    public static final int LOG_CONFIG_ALL = 0xFFFFFFFF;
    public static final int LOG_CONFIG_NONE = 0x00000000;
    public static final int LOG_CONFIG_REAL_URL = 0x00000001;
    public static final int LOG_CONFIG_BLOCK = 0x00000010;

    public static final int VERBOSE_LOG_CONFIG_ALL = 0xFFFFFFFF;
    public static final int VERBOSE_LOG_CONFIG_NONE = 0x00000000;
    public static final int VERBOSE_LOG_CONFIG_REQUEST_INPUTS = 0x00000001;
    public static final int VERBOSE_LOG_CONFIG_REQUEST_STRING_BODY= 0x00000010;
    public static final int VERBOSE_LOG_CONFIG_RAW_URL= 0x00000100;
    public static final int VERBOSE_LOG_CONFIG_RESPONSE_CODE = 0x00001000;

    private static final long PASSIVE_BLOCK_DURATION = 3000L;
    private static final String MEDIA_TYPE = "application/json;charset=utf-8";
    private static final String ENCODE = "utf-8";

    private Logger logger = LoggerFactory.getLogger(getClass());

    private OkHttpClient okHttpClient;
    private LoadBalancedHostManager hostManager;

    private Settings settings = new Settings();
    private boolean refreshSettings = false;
    private ReentrantLock settingsLock = new ReentrantLock();

    //Settings ///////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 设置远端管理器(必须)
     * @param hostManager 远端管理器
     */
    public void setHostManager(LoadBalancedHostManager hostManager) {
        this.hostManager = hostManager;
    }

    /**
     * 设置被动检测到网络故障时阻断后端的时间
     * @param passiveBlockDuration 阻断时长ms
     */
    public void setPassiveBlockDuration(long passiveBlockDuration) {
        settings.passiveBlockDuration = passiveBlockDuration;
    }

    /**
     * 设置MediaType
     * @param mediaType 设置MediaType
     */
    public void setMediaType(String mediaType) {
        settings.mediaType = mediaType;
    }

    /**
     * 设置编码
     * @param encode 编码
     */
    public void setEncode(String encode) {
        settings.encode = encode;
    }

    /**
     * 设置HTTP请求头参数
     * @param headers 请求头参数
     */
    public void setHeaders(Map<String, String> headers) {
        settings.headers = headers;
    }

    /**
     * 打印更多的日志, 默认关闭
     * @param verboseLog true:打印更多的调试日志, 默认关闭
     */
    public void setVerboseLog(boolean verboseLog) {
        settings.verboseLog = verboseLog;
    }

    /**
     * 打印更多的日志, 细粒度配置, 默认全打印, 当verboseLog=true时该参数生效<br>
     *
     * VERBOSE_LOG_CONFIG_ALL:{@value VERBOSE_LOG_CONFIG_ALL}<br>
     * VERBOSE_LOG_CONFIG_REQUEST_INPUTS:{@value VERBOSE_LOG_CONFIG_REQUEST_INPUTS}<br>
     * VERBOSE_LOG_CONFIG_REQUEST_STRING_BODY:{@value VERBOSE_LOG_CONFIG_REQUEST_STRING_BODY}<br>
     * VERBOSE_LOG_CONFIG_RAW_URL:{@value VERBOSE_LOG_CONFIG_RAW_URL}<br>
     * VERBOSE_LOG_CONFIG_RESPONSE_CODE:{@value VERBOSE_LOG_CONFIG_RESPONSE_CODE}<br>
     *
     * @param verboseLogConfig 详细配置
     */
    public void setVerboseLogConfig(int verboseLogConfig) {
        settings.verboseLogConfig = verboseLogConfig;
    }

    /**
     * 日志打印细粒度配置, 默认全打印<br>
     *
     * LOG_CONFIG_ALL:{@value LOG_CONFIG_ALL}<br>
     * LOG_CONFIG_REAL_URL:{@value LOG_CONFIG_REAL_URL}<br>
     * LOG_CONFIG_BLOCK:{@value LOG_CONFIG_BLOCK}<br>
     *
     * @param logConfig 详细配置
     */
    public void setLogConfig(int logConfig) {
        settings.logConfig = logConfig;
    }

    /**
     * 最大请求线程数(仅异步请求时有效)
     * @param maxThreads 最大请求线程数
     */
    public void setMaxThreads(int maxThreads) {
        settings.maxThreads = maxThreads;
    }

    /**
     * 对应每个后端的最大请求线程数(仅异步请求时有效)
     * @param maxThreadsPerHost 对应每个后端的最大请求线程数
     */
    public void setMaxThreadsPerHost(int maxThreadsPerHost) {
        settings.maxThreadsPerHost = maxThreadsPerHost;
    }

    /**
     * 设置连接超时ms
     * @param connectTimeout 连接超时ms
     */
    public void setConnectTimeout(long connectTimeout) {
        try {
            settingsLock.lock();
            settings.connectTimeout = connectTimeout;
            refreshSettings = true;
        } finally {
            settingsLock.unlock();
        }
    }

    /**
     * 设置写数据超时ms
     * @param writeTimeout 写数据超时ms
     */
    public void setWriteTimeout(long writeTimeout) {
        try {
            settingsLock.lock();
            settings.writeTimeout = writeTimeout;
            refreshSettings = true;
        } finally {
            settingsLock.unlock();
        }
    }

    /**
     * 设置读数据超时ms
     * @param readTimeout 读数据超时ms
     */
    public void setReadTimeout(long readTimeout) {
        try {
            settingsLock.lock();
            settings.readTimeout = readTimeout;
            refreshSettings = true;
        } finally {
            settingsLock.unlock();
        }
    }

    /**
     * 设置最大读取数据长度(默认:10M)
     * @param maxReadLength 设置最大读取数据长度, 单位bytes
     */
    public void setMaxReadLength(long maxReadLength){
        settings.maxReadLength = maxReadLength;
    }

    /**
     * CookieJar
     * @param cookieJar CookieJar
     */
    public void setCookieJar(CookieJar cookieJar) {
        try {
            settingsLock.lock();
            settings.cookieJar = cookieJar;
            refreshSettings = true;
        } finally {
            settingsLock.unlock();
        }
    }

    /**
     * Proxy
     * @param proxy 例如127.0.0.1:8080
     * @throws IllegalArgumentException if the proxy string is invalid
     * @throws NumberFormatException  if the string does not contain a parsable integer.
     * @throws SecurityException if a security manager is present and permission to resolve the host name is denied.
     */
    public void setProxy(String proxy) {
        Proxy proxyObj = null;
        if (proxy == null){
            throw new IllegalArgumentException("Invalid proxy string \"" + proxy + "\", correct \"X.X.X.X:XXX\", example \"127.0.0.1:8080\"");
        }
        int index = proxy.indexOf(":");
        if (index <= 0 || index >= proxy.length() - 1){
            throw new IllegalArgumentException("Invalid proxy string \"" + proxy + "\", correct \"X.X.X.X:XXX\", example \"127.0.0.1:8080\"");
        }
        try {
            proxyObj = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
                    proxy.substring(0, index),
                    Integer.parseInt(proxy.substring(index + 1))));
        } catch (Throwable t){
            throw new IllegalArgumentException("Invalid proxy string \"" + proxy + "\", correct \"X.X.X.X:XXX\", example \"127.0.0.1:8080\"");
        }
        try {
            settingsLock.lock();
            settings.proxy = proxyObj;
            refreshSettings = true;
        } finally {
            settingsLock.unlock();
        }
    }

    /**
     * Dns
     * @param dns Dns
     */
    public void setDns(Dns dns) {
        try {
            settingsLock.lock();
            settings.dns = dns;
            refreshSettings = true;
        } finally {
            settingsLock.unlock();
        }
    }

    //sync /////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 同步POST请求,
     * 如果响应码不为2XX, 会抛出HttpRejectException异常
     *
     * @param urlSuffix url后缀
     * @param body 报文体
     * @return 二进制数据(可能为null)
     * @throws NoHostException 当前没有可发送的后端(网络请求发送前的异常, 准备阶段异常)
     * @throws RequestBuildException 请求初始化异常(通常是网络请求发送前的异常, 准备阶段异常)
     * @throws IOException 网络通讯异常(通常是网络请求发送中的异常)
     * @throws HttpRejectException Http请求拒绝异常(网络请求发送后的异常, HTTP响应码不为2XX)
     */
    public byte[] syncPostForBytes(String urlSuffix, byte[] body) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        return syncPostForBytes(urlSuffix, body, null);
    }

    /**
     * 同步POST请求,
     * 如果响应码不为2XX, 会抛出HttpRejectException异常
     *
     * @param urlSuffix url后缀
     * @param body 报文体
     * @param params URL请求参数
     * @return 二进制数据(可能为null)
     * @throws NoHostException 当前没有可发送的后端(网络请求发送前的异常, 准备阶段异常)
     * @throws RequestBuildException 请求初始化异常(通常是网络请求发送前的异常, 准备阶段异常)
     * @throws IOException 网络通讯异常(通常是网络请求发送中的异常)
     * @throws HttpRejectException Http请求拒绝异常(网络请求发送后的异常, HTTP响应码不为2XX)
     */
    public byte[] syncPostForBytes(String urlSuffix, byte[] body, Map<String, Object> params) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        ResponseBody responseBody = null;
        try {
            responseBody = syncPost(urlSuffix, body, params);
            //返回空
            if (responseBody == null) {
                return null;
            }
            //限定读取长度
            if (settings.maxReadLength > 0 && responseBody.contentLength() > settings.maxReadLength){
                throw new IOException("Response contentLength is out of limit, contentLength:" + responseBody.contentLength() + ", limit:" + settings.maxReadLength);
            }
            //返回二进制数据
            return responseBody.bytes();
        } finally {
            if (responseBody != null){
                try {
                    responseBody.close();
                } catch (Throwable ignore) {
                }
            }
        }
    }

    /**
     * 同步POST请求,
     * 如果响应码不为2XX, 会抛出HttpRejectException异常
     *
     * @param urlSuffix url后缀
     * @param body 报文体
     * @return InputStream(可能为null), 注意:使用完必须关闭流!!!
     * @throws NoHostException 当前没有可发送的后端(网络请求发送前的异常, 准备阶段异常)
     * @throws RequestBuildException 请求初始化异常(通常是网络请求发送前的异常, 准备阶段异常)
     * @throws IOException 网络通讯异常(通常是网络请求发送中的异常)
     * @throws HttpRejectException Http请求拒绝异常(网络请求发送后的异常, HTTP响应码不为2XX)
     */
    public InputStream syncPostForInputStream(String urlSuffix, byte[] body) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        return syncPostForInputStream(urlSuffix, body, null);
    }

    /**
     * 同步POST请求,
     * 如果响应码不为2XX, 会抛出HttpRejectException异常
     *
     * @param urlSuffix url后缀
     * @param body 报文体
     * @param params URL请求参数
     * @return InputStream(可能为null), 注意:使用完必须关闭流!!!
     * @throws NoHostException 当前没有可发送的后端(网络请求发送前的异常, 准备阶段异常)
     * @throws RequestBuildException 请求初始化异常(通常是网络请求发送前的异常, 准备阶段异常)
     * @throws IOException 网络通讯异常(通常是网络请求发送中的异常)
     * @throws HttpRejectException Http请求拒绝异常(网络请求发送后的异常, HTTP响应码不为2XX)
     */
    public InputStream syncPostForInputStream(String urlSuffix, byte[] body, Map<String, Object> params) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        ResponseBody responseBody = syncPost(urlSuffix, body, params);
        //返回空
        if (responseBody == null) {
            return null;
        }
        //限定读取长度
        if (settings.maxReadLength > 0 && responseBody.contentLength() > settings.maxReadLength){
            throw new IOException("Response contentLength is out of limit, contentLength:" + responseBody.contentLength() + ", limit:" + settings.maxReadLength);
        }
        //返回二进制数据
        return responseBody.byteStream();
    }

    /**
     * 同步GET请求,
     * 如果响应码不为2XX, 会抛出HttpRejectException异常
     *
     * @param urlSuffix url后缀
     * @param params 请求参数
     * @return 二进制数据(可能为null)
     * @throws NoHostException 当前没有可发送的后端(网络请求发送前的异常, 准备阶段异常)
     * @throws RequestBuildException 请求初始化异常(通常是网络请求发送前的异常, 准备阶段异常)
     * @throws IOException 网络通讯异常(通常是网络请求发送中的异常)
     * @throws HttpRejectException Http请求拒绝异常(网络请求发送后的异常, HTTP响应码不为2XX)
     */
    public byte[] syncGetForBytes(String urlSuffix, Map<String, Object> params) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        ResponseBody responseBody = null;
        try {
            responseBody = syncGet(urlSuffix, params);
            //返回空
            if (responseBody == null) {
                return null;
            }
            //限定读取长度
            if (settings.maxReadLength > 0 && responseBody.contentLength() > settings.maxReadLength){
                throw new IOException("Response contentLength is out of limit, contentLength:" + responseBody.contentLength() + ", limit:" + settings.maxReadLength);
            }
            //返回二进制数据
            return responseBody.bytes();
        } finally {
            if (responseBody != null){
                try {
                    responseBody.close();
                } catch (Throwable ignore) {
                }
            }
        }
    }

    /**
     * 同步GET请求,
     * 如果响应码不为2XX, 会抛出HttpRejectException异常
     *
     * @param urlSuffix url后缀
     * @param params 请求参数
     * @return InputStream(可能为null), 注意:使用完必须关闭流!!!
     * @throws NoHostException 当前没有可发送的后端(网络请求发送前的异常, 准备阶段异常)
     * @throws RequestBuildException 请求初始化异常(通常是网络请求发送前的异常, 准备阶段异常)
     * @throws IOException 网络通讯异常(通常是网络请求发送中的异常)
     * @throws HttpRejectException Http请求拒绝异常(网络请求发送后的异常, HTTP响应码不为2XX)
     */
    public InputStream syncGetForInputStream(String urlSuffix, Map<String, Object> params) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        ResponseBody responseBody = syncGet(urlSuffix, params);
        //返回空
        if (responseBody == null) {
            return null;
        }
        //限定读取长度
        if (settings.maxReadLength > 0 && responseBody.contentLength() > settings.maxReadLength){
            throw new IOException("Response contentLength is out of limit, contentLength:" + responseBody.contentLength() + ", limit:" + settings.maxReadLength);
        }
        //返回二进制数据
        return responseBody.byteStream();
    }

    /**
     * 同步POST请求,
     * 如果响应码不为2XX, 会抛出HttpRejectException异常,
     * 该方法不会根据maxReadLength限定最大读取长度
     *
     * @param urlSuffix url后缀
     * @param body      报文体
     * @return ResponseBody(可能为null), 注意:使用完必须关闭(ResponseBody.close())!!!
     * @throws NoHostException       当前没有可发送的后端(网络请求发送前的异常, 准备阶段异常)
     * @throws RequestBuildException 请求初始化异常(通常是网络请求发送前的异常, 准备阶段异常)
     * @throws IOException           网络通讯异常(通常是网络请求发送中的异常)
     * @throws HttpRejectException   Http请求拒绝异常(网络请求发送后的异常, HTTP响应码不为2XX)
     */
    public ResponseBody syncPost(String urlSuffix, byte[] body) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        return syncPost(urlSuffix, body, null);
    }

    /**
     * 同步POST请求,
     * 如果响应码不为2XX, 会抛出HttpRejectException异常,
     * 该方法不会根据maxReadLength限定最大读取长度
     *
     * @param urlSuffix url后缀
     * @param body      报文体
     * @param params    URL请求参数
     * @return ResponseBody(可能为null), 注意:使用完必须关闭(ResponseBody.close())!!!
     * @throws NoHostException       当前没有可发送的后端(网络请求发送前的异常, 准备阶段异常)
     * @throws RequestBuildException 请求初始化异常(通常是网络请求发送前的异常, 准备阶段异常)
     * @throws IOException           网络通讯异常(通常是网络请求发送中的异常)
     * @throws HttpRejectException   Http请求拒绝异常(网络请求发送后的异常, HTTP响应码不为2XX)
     */
    public ResponseBody syncPost(String urlSuffix, byte[] body, Map<String, Object> params) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        //获取远端
        LoadBalancedHostManager.Host host = fetchHost();

        printPostRequestLog(urlSuffix, body, params, host);
        printUrlLog(urlSuffix, params, host);

        //装配Request
        Request request;
        try {
            request = buildPostRequest(host.getUrl(), urlSuffix, body, params, settings);
        } catch (Throwable t) {
            throw new RequestBuildException("Error while building request", t);
        }
        if (request == null) {
            throw new RequestBuildException("Null request built");
        }

        if (logger.isInfoEnabled() && CheckUtils.isFlagMatch(settings.logConfig, LOG_CONFIG_REAL_URL)) {
            logger.info("POST: real-url:" + request.url().toString());
        }

        //请求
        return syncCall(host, request);
    }

    /**
     * 同步GET请求,
     * 如果响应码不为2XX, 会抛出HttpRejectException异常,
     * 该方法不会根据maxReadLength限定最大读取长度
     *
     * @param urlSuffix url后缀
     * @param params 请求参数
     * @return ResponseBody(可能为null), 注意:使用完必须关闭(ResponseBody.close())!!!
     * @throws NoHostException 当前没有可发送的后端(网络请求发送前的异常, 准备阶段异常)
     * @throws RequestBuildException 请求初始化异常(通常是网络请求发送前的异常, 准备阶段异常)
     * @throws IOException 网络通讯异常(通常是网络请求发送中的异常)
     * @throws HttpRejectException Http请求拒绝异常(网络请求发送后的异常, HTTP响应码不为2XX)
     */
    public ResponseBody syncGet(String urlSuffix, Map<String, Object> params) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        //获取远端
        LoadBalancedHostManager.Host host = fetchHost();

        printGetRequestLog(urlSuffix, params, host);
        printUrlLog(urlSuffix, params, host);

        //装配Request
        Request request;
        try {
            request = buildGetRequest(host.getUrl(), urlSuffix, params, settings);
        } catch (Throwable t) {
            throw new RequestBuildException("Error while building request", t);
        }
        if (request == null) {
            throw new RequestBuildException("Null request built");
        }

        if (logger.isInfoEnabled() && CheckUtils.isFlagMatch(settings.logConfig, LOG_CONFIG_REAL_URL)) {
            logger.info("GET: real-url:" + request.url().toString());
        }

        //请求
        return syncCall(host, request);
    }

    // async ////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 异步POST请求,
     * 如果响应码不为2XX, 会回调onErrorAfterSend()方法给出HttpRejectException异常
     *
     * @param urlSuffix url后缀
     * @param body      报文体
     * @param params    URL请求参数
     * @param callback  回调函数
     */
    public void asyncPostForBytes(String urlSuffix, byte[] body, Map<String, Object> params, BytesCallback callback) {
        callback.setSettings(settings);
        asyncPost(urlSuffix, body, params, callback);
    }

    /**
     * 异步POST请求,
     * 如果响应码不为2XX, 会回调onErrorAfterSend()方法给出HttpRejectException异常
     *
     * @param urlSuffix url后缀
     * @param body      报文体
     * @param params    URL请求参数
     * @param callback  回调函数
     */
    public void asyncPostForInputStream(String urlSuffix, byte[] body, Map<String, Object> params, InputStreamCallback callback) {
        callback.setSettings(settings);
        asyncPost(urlSuffix, body, params, callback);
    }

    /**
     * 异步POST请求,
     * 如果响应码不为2XX, 会回调onErrorAfterSend()方法给出HttpRejectException异常,
     * 该方法不会根据maxReadLength限定最大读取长度
     *
     * @param urlSuffix url后缀
     * @param body      报文体
     * @param params    URL请求参数
     * @param callback  回调函数{@link BytesCallback}/{@link InputStreamCallback}/{@link ResponseBodyCallback}
     */
    public void asyncPost(String urlSuffix, byte[] body, Map<String, Object> params, ResponseBodyCallback callback) {
        try {
            //获取远端
            LoadBalancedHostManager.Host host = fetchHost();

            printPostRequestLog(urlSuffix, body, params, host);
            printUrlLog(urlSuffix, params, host);

            //装配Request
            Request request;
            try {
                request = buildPostRequest(host.getUrl(), urlSuffix, body, params, settings);
            } catch (Throwable t) {
                throw new RequestBuildException("Error while building request", t);
            }
            if (request == null) {
                throw new RequestBuildException("Null request built");
            }

            if (logger.isInfoEnabled() && CheckUtils.isFlagMatch(settings.logConfig, LOG_CONFIG_REAL_URL)) {
                logger.info("POST: real-url:" + request.url().toString());
            }

            //请求
            asyncCall(host, request, callback);
        } catch (NoHostException | RequestBuildException e) {
            callback.onErrorBeforeSend(e);
        }
    }

    /**
     * 异步GET请求,
     * 如果响应码不为2XX, 会抛出HttpRejectException异常,
     * 该方法不会根据maxReadLength限定最大读取长度
     *
     * @param urlSuffix url后缀
     * @param params 请求参数
     * @param callback  回调函数
     */
    public void asyncGetForBytes(String urlSuffix, Map<String, Object> params, BytesCallback callback) {
        callback.setSettings(settings);
        asyncGet(urlSuffix, params, callback);
    }

    /**
     * 异步GET请求,
     * 如果响应码不为2XX, 会抛出HttpRejectException异常,
     * 该方法不会根据maxReadLength限定最大读取长度
     *
     * @param urlSuffix url后缀
     * @param params 请求参数
     * @param callback  回调函数
     */
    public void asyncGetForInputStream(String urlSuffix, Map<String, Object> params, InputStreamCallback callback) {
        callback.setSettings(settings);
        asyncGet(urlSuffix, params, callback);
    }

    /**
     * 异步GET请求,
     * 如果响应码不为2XX, 会抛出HttpRejectException异常,
     * 该方法不会根据maxReadLength限定最大读取长度
     *
     * @param urlSuffix url后缀
     * @param params 请求参数
     * @param callback  回调函数{@link BytesCallback}/{@link InputStreamCallback}/{@link ResponseBodyCallback}
     */
    public void asyncGet(String urlSuffix, Map<String, Object> params, ResponseBodyCallback callback) {
        try {
            //获取远端
            LoadBalancedHostManager.Host host = fetchHost();

            printGetRequestLog(urlSuffix, params, host);
            printUrlLog(urlSuffix, params, host);

            //装配Request
            Request request;
            try {
                request = buildGetRequest(host.getUrl(), urlSuffix, params, settings);
            } catch (Throwable t) {
                throw new RequestBuildException("Error while building request", t);
            }
            if (request == null) {
                throw new RequestBuildException("Null request built");
            }

            if (logger.isInfoEnabled() && CheckUtils.isFlagMatch(settings.logConfig, LOG_CONFIG_REAL_URL)) {
                logger.info("GET: real-url:" + request.url().toString());
            }

            //请求
            asyncCall(host, request, callback);
        } catch (NoHostException | RequestBuildException e) {
            callback.onErrorBeforeSend(e);
        }
    }

    //private //////////////////////////////////////////////////////////////////////////////////////////////////////

    private LoadBalancedHostManager.Host fetchHost() throws NoHostException {
        LoadBalancedHostManager.Host host = hostManager.nextHost();
        if (host == null){
            throw new NoHostException("No host");
        }
        return host;
    }

    private ResponseBody syncCall(LoadBalancedHostManager.Host host, Request request) throws RequestBuildException, IOException, HttpRejectException {
        try {
            //同步请求
            Response response = getOkHttpClient().newCall(request).execute();
            printResponseCodeLog(response);
            //Http拒绝
            if (!isSucceed(response)) {
                throw new HttpRejectException(response.code(), response.message());
            }
            //报文体
            return response.body();
        } catch (Throwable t) {
            if (needBlock(t, settings)) {
                //网络故障阻断后端
                host.block(settings.passiveBlockDuration);
                if (logger.isInfoEnabled() && CheckUtils.isFlagMatch(settings.logConfig, LOG_CONFIG_BLOCK)){
                    logger.info("Block: " + host.getUrl() + " " + settings.passiveBlockDuration);
                }
            }
            if (t instanceof  IOException ||
                    t instanceof HttpRejectException) {
                throw t;
            } else {
                throw new RequestBuildException("Error while request build ?", t);
            }
        }
    }

    private void asyncCall(final LoadBalancedHostManager.Host host, Request request, final ResponseBodyCallback callback)  {
        //异步请求
        try {
            getOkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    printResponseCodeLog(response);
                    //Http拒绝
                    if (!isSucceed(response)) {
                        Exception exception = new HttpRejectException(response.code(), response.message());
                        tryBlock(exception);
                        callback.onErrorAfterSend(exception);
                        return;
                    }
                    //报文体
                    callback.onSucceed(response.body());
                }
                @Override
                public void onFailure(Call call, IOException e) {
                    tryBlock(e);
                    callback.onErrorAfterSend(e);
                }
                private void tryBlock(Exception e){
                    if (needBlock(e, settings)) {
                        //网络故障阻断后端
                        host.block(settings.passiveBlockDuration);
                        if (logger.isInfoEnabled() && CheckUtils.isFlagMatch(settings.logConfig, LOG_CONFIG_BLOCK)) {
                            logger.info("Block: " + host.getUrl() + " " + settings.passiveBlockDuration);
                        }
                    }
                }
            });
        } catch (Exception t) {
            callback.onErrorBeforeSend(new RequestBuildException("Error while request build ?", t));
        }
    }

    private OkHttpClient getOkHttpClient(){
        OkHttpClient client = okHttpClient;
        if (client == null || refreshSettings) {
            try {
                settingsLock.lock();
                client = okHttpClient;
                if (client == null || refreshSettings) {
                    client = createOkHttpClient(settings);
                    okHttpClient = client;
                    refreshSettings = false;
                }
            } finally {
                settingsLock.unlock();
            }
        }
        return client;
    }

    private void printPostRequestLog(String urlSuffix, byte[] body, Map<String, Object> params, LoadBalancedHostManager.Host host) {
        if (settings.verboseLog && logger.isDebugEnabled()) {
            if (CheckUtils.isFlagMatch(settings.verboseLogConfig, VERBOSE_LOG_CONFIG_REQUEST_INPUTS)) {
                logger.debug("POST: url:" + host.getUrl() + ", suffix:" + urlSuffix + "urlParams:" + params + ", body:" + ByteUtils.bytesToHex(body));
            }
            if (CheckUtils.isFlagMatch(settings.verboseLogConfig, VERBOSE_LOG_CONFIG_REQUEST_STRING_BODY)) {
                try {
                    logger.debug("POST: string-body:" + new String(body, settings.encode));
                } catch (Exception e) {
                    logger.warn("Error while printing string body", e);
                }
            }
        }
    }

    private void printGetRequestLog(String urlSuffix, Map<String, Object> params, LoadBalancedHostManager.Host host) {
        if (settings.verboseLog && logger.isDebugEnabled() && CheckUtils.isFlagMatch(settings.verboseLogConfig, VERBOSE_LOG_CONFIG_REQUEST_INPUTS)) {
            logger.debug("GET: url:" + host.getUrl() + ", suffix:" + urlSuffix + ", urlParams:" + params);
        }
    }

    private void printUrlLog(String urlSuffix, Map<String, Object> params, LoadBalancedHostManager.Host host) {
        if (settings.verboseLog && logger.isDebugEnabled()
                && CheckUtils.isFlagMatch(settings.verboseLogConfig, VERBOSE_LOG_CONFIG_RAW_URL)
                && params != null && params.size() > 0) {
            StringBuilder stringBuilder = new StringBuilder("raw-url:" + host.getUrl() + urlSuffix + "?");
            int i = 0;
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (i++ > 0) {
                    stringBuilder.append("&");
                }
                stringBuilder.append(entry.getKey());
                stringBuilder.append("=");
                stringBuilder.append(entry.getValue());
            }
            logger.debug(stringBuilder.toString());
        }
    }

    private void printResponseCodeLog(Response response) {
        if (settings.verboseLog && logger.isDebugEnabled() && CheckUtils.isFlagMatch(settings.verboseLogConfig, VERBOSE_LOG_CONFIG_RESPONSE_CODE)) {
            logger.debug("Response: code:" + response.code() + ", message:" + response.message());
        }
    }

    //Override //////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 初始化OkHttpClient实例(复写本方法实现自定义的逻辑)
     * @return OkHttpClient实例
     */
    protected OkHttpClient createOkHttpClient(Settings settings){

        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(settings.maxThreads);
        dispatcher.setMaxRequestsPerHost(settings.maxThreadsPerHost);

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(settings.connectTimeout, TimeUnit.MILLISECONDS)
                .writeTimeout(settings.writeTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(settings.readTimeout, TimeUnit.MILLISECONDS)
                .dispatcher(dispatcher);

        if (settings.cookieJar != null) {
            builder.cookieJar(settings.cookieJar);
        }
        if (settings.proxy != null) {
            builder.proxy(settings.proxy);
        }
        if (settings.dns != null) {
            builder.dns(settings.dns);
        }

        return builder.build();
    }

    /**
     * 根据URL和报文体组POST请求(复写本方法实现自定义的逻辑)
     * @param url 由LoadBalancedHostManager选择出的远端url(前缀)
     * @param urlSuffix URL后缀
     * @param body 报文体
     * @param params URL请求参数
     * @return Request
     * @throws RequestBuildException 构建异常
     */
    protected Request buildPostRequest(String url, String urlSuffix, byte[] body, Map<String, Object> params, Settings settings) throws RequestBuildException{
        HttpUrl httpUrl = HttpUrl.parse(url + urlSuffix);
        if (httpUrl == null){
            throw new RequestBuildException("Invalid url:" + url + urlSuffix);
        }

        if (params != null){
            HttpUrl.Builder httpUrlBuilder = httpUrl.newBuilder();
            for (Map.Entry<String, Object> param : params.entrySet()) {
                try {
                    httpUrlBuilder.addEncodedQueryParameter(param.getKey(), URLEncoder.encode(param.getValue() != null ? param.getValue().toString() : "", settings.encode));
                } catch (UnsupportedEncodingException e) {
                    throw new RequestBuildException("Error while encode to url format", e);
                }
            }
            httpUrl = httpUrlBuilder.build();
        }

        Request.Builder builder = new Request.Builder()
                .url(httpUrl)
                .post(RequestBody.create(MediaType.parse(settings.mediaType), body));

        Map<String, String> headers = settings.headers;
        if (headers != null){
            for (Map.Entry<String, String> entry : headers.entrySet()){
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }

    /**
     * 根据URL和报文体组GET请求(复写本方法实现自定义的逻辑)
     * @param url 由LoadBalancedHostManager选择出的远端url(前缀)
     * @param urlSuffix URL后缀
     * @param params 请求参数
     * @return Request
     * @throws RequestBuildException 构建异常
     */
    protected Request buildGetRequest(String url, String urlSuffix, Map<String, Object> params, Settings settings) throws RequestBuildException{
        HttpUrl httpUrl = HttpUrl.parse(url + urlSuffix);
        if (httpUrl == null){
            throw new RequestBuildException("Invalid url:" + url + urlSuffix);
        }

        if (params != null){
            HttpUrl.Builder httpUrlBuilder = httpUrl.newBuilder();
            for (Map.Entry<String, Object> param : params.entrySet()) {
                try {
                    httpUrlBuilder.addEncodedQueryParameter(param.getKey(), URLEncoder.encode(param.getValue() != null ? param.getValue().toString() : "", settings.encode));
                } catch (UnsupportedEncodingException e) {
                    throw new RequestBuildException("Error while encode to url format", e);
                }
            }
            httpUrl = httpUrlBuilder.build();
        }

        Request.Builder builder = new Request.Builder()
                .url(httpUrl)
                .get();

        Map<String, String> headers = settings.headers;
        if (headers != null){
            for (Map.Entry<String, String> entry : headers.entrySet()){
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }

    protected boolean needBlock(Throwable t, Settings settings) {
        return t instanceof ConnectException ||
                t instanceof SocketTimeoutException ||
                t instanceof UnknownHostException;
    }

    protected boolean isSucceed(Response response) {
        return response.isSuccessful();
    }

    public static class Settings {

        private long passiveBlockDuration = PASSIVE_BLOCK_DURATION;
        private String mediaType = MEDIA_TYPE;
        private String encode = ENCODE;
        private Map<String, String> headers;
        private boolean verboseLog = false;
        private int verboseLogConfig = VERBOSE_LOG_CONFIG_ALL;
        private int logConfig = LOG_CONFIG_ALL;

        private int maxThreads = 64;
        private int maxThreadsPerHost = 64;
        private long connectTimeout = 3000L;
        private long writeTimeout = 1000L;
        private long readTimeout = 1000L;
        private long maxReadLength = 10L * 1024L * 1024L;
        private CookieJar cookieJar;
        private Proxy proxy;
        private Dns dns;

    }

    /**
     * 请求回调
     */
    public interface ResponseBodyCallback {

        /**
         * 请求成功
         * 注意:处理完毕后必须关闭ResponseBody(调用close())!!!
         * @param responseBody 响应报文体, 注意:处理完毕后必须关闭ResponseBody(调用close())!!!
         */
        void onSucceed(ResponseBody responseBody);

        /**
         * 请求前发生异常
         * @param e {@link RequestBuildException}:请求前发生异常, {@link NoHostException}:未配置后端地址或所有后端地址均不可用
         */
        void onErrorBeforeSend(Exception e);

        /**
         * 请求后发生异常
         * @param e {@link HttpRejectException}:后端Http拒绝(返回码不为200), {@link IOException}:通讯异常
         */
        void onErrorAfterSend(Exception e);

    }

    /**
     * 请求回调(获得byte[]响应体)
     */
    public abstract class BytesCallback implements ResponseBodyCallback {

        private Settings settings;

        /**
         * 请求成功
         * @param body 响应报文体, 可能为null
         */
        public abstract void onSucceed(byte[] body);

        @Override
        public void onSucceed(ResponseBody responseBody) {
            byte[] bytes = null;
            try {
                if (responseBody != null) {
                    //限定读取长度
                    if (settings.maxReadLength > 0 && responseBody.contentLength() > settings.maxReadLength) {
                        throw new IOException("Response contentLength is out of limit, contentLength:" + responseBody.contentLength() + ", limit:" + settings.maxReadLength);
                    }
                    //返回二进制数据
                    bytes = responseBody.bytes();
                }
            } catch (IOException e) {
                onErrorAfterSend(e);
                return;
            } finally {
                if (responseBody != null){
                    try {
                        responseBody.close();
                    } catch (Throwable ignore) {
                    }
                }
            }
            onSucceed(bytes);
        }

        private void setSettings(Settings settings) {
            this.settings = settings;
        }

    }

    /**
     * 请求回调(获得InputStream响应体)
     */
    public abstract class InputStreamCallback implements ResponseBodyCallback {

        private Settings settings;

        /**
         * 请求成功
         * @param inputStream 报文体输入流, 可能为null, 该输入流会被自动关闭
         */
        public abstract void onSucceed(InputStream inputStream);

        @Override
        public void onSucceed(ResponseBody responseBody) {
            //返回空
            if (responseBody == null) {
                onSucceed((InputStream) null);
                return;
            }
            try {
                //限定读取长度
                if (settings.maxReadLength > 0 && responseBody.contentLength() > settings.maxReadLength) {
                    onErrorAfterSend(new IOException("Response contentLength is out of limit, contentLength:" + responseBody.contentLength() + ", limit:" + settings.maxReadLength));
                    return;
                }
                //返回二进制数据
                onSucceed(responseBody.byteStream());
            } finally {
                try {
                    responseBody.close();
                } catch (Throwable ignore) {
                }
            }
        }

        private void setSettings(Settings settings) {
            this.settings = settings;
        }

    }

}
