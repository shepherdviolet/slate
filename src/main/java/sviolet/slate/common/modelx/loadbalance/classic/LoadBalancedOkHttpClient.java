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
     * 打印更多的调试日志, 默认关闭
     * @param verboseLog true:打印更多的调试日志, 默认关闭
     */
    public void setVerboseLog(boolean verboseLog) {
        settings.verboseLog = verboseLog;
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

    //Request /////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 同步POST请求
     * @param urlSuffix url后缀
     * @param body 报文体
     * @return 二进制数据(可能为null)
     * @throws NoHostException 当前没有可发送的后端(网络请求发送前的异常, 准备阶段异常)
     * @throws RequestBuildException 请求初始化异常(通常是网络请求发送前的异常, 准备阶段异常)
     * @throws IOException 网络通讯异常(通常是网络请求发送中的异常)
     * @throws HttpRejectException Http请求拒绝异常(网络请求发送后的异常, HTTP响应码不为2XX)
     */
    public byte[] syncPostForBytes(String urlSuffix, byte[] body) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        ResponseBody responseBody = null;
        try {
            responseBody = syncPost(urlSuffix, body);
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
                responseBody.close();
            }
        }
    }

    /**
     * 同步POST请求
     * @param urlSuffix url后缀
     * @param body 报文体
     * @return InputStream(可能为null), 注意:使用完必须关闭流!!!
     * @throws NoHostException 当前没有可发送的后端(网络请求发送前的异常, 准备阶段异常)
     * @throws RequestBuildException 请求初始化异常(通常是网络请求发送前的异常, 准备阶段异常)
     * @throws IOException 网络通讯异常(通常是网络请求发送中的异常)
     * @throws HttpRejectException Http请求拒绝异常(网络请求发送后的异常, HTTP响应码不为2XX)
     */
    public InputStream syncPostForInputStream(String urlSuffix, byte[] body) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        ResponseBody responseBody = syncPost(urlSuffix, body);
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
     * 同步GET请求
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
                responseBody.close();
            }
        }
    }

    /**
     * 同步GET请求
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
     * 同步POST请求
     * @param urlSuffix url后缀
     * @param body 报文体
     * @return ResponseBody(可能为null), 注意:使用完必须关闭(ResponseBody.close())!!!
     * @throws NoHostException 当前没有可发送的后端(网络请求发送前的异常, 准备阶段异常)
     * @throws RequestBuildException 请求初始化异常(通常是网络请求发送前的异常, 准备阶段异常)
     * @throws IOException 网络通讯异常(通常是网络请求发送中的异常)
     * @throws HttpRejectException Http请求拒绝异常(网络请求发送后的异常, HTTP响应码不为2XX)
     */
    public ResponseBody syncPost(String urlSuffix, byte[] body) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        //获取远端
        LoadBalancedHostManager.Host host = fetchHost();

        if (settings.verboseLog) {
            logger.debug("POST url:" + host.getUrl() + ", suffix:" + urlSuffix + ", body:" + ByteUtils.bytesToHex(body));
        }

        //装配Request
        Request request;
        try {
            request = buildPostRequest(host.getUrl(), urlSuffix, body, settings);
        } catch (Throwable t) {
            throw new RequestBuildException("Error while building request", t);
        }
        if (request == null) {
            throw new RequestBuildException("Null request built");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("POST real url:" + request.url().toString());
        }

        //请求
        return syncCall(host, request);
    }

    /**
     * 同步GET请求
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

        if (settings.verboseLog) {
            logger.debug("GET url:" + host.getUrl() + ", suffix:" + urlSuffix + ", params:" + params);
        }

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

        if (logger.isDebugEnabled()) {
            logger.debug("GET real url:" + request.url().toString());
        }

        //请求
        return syncCall(host, request);
    }

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
            //Http拒绝
            if (!response.isSuccessful()) {
                throw new HttpRejectException(response.code(), response.message());
            }
            //报文体
            return response.body();
        } catch (Throwable t) {
            if (needBlock(t, settings)) {
                //网络故障阻断后端
                host.block(settings.passiveBlockDuration);
                if (logger.isInfoEnabled()){
                    logger.info("Block " + host.getUrl() + " " + settings.passiveBlockDuration);
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

    //Override //////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 初始化OkHttpClient实例(复写本方法实现自定义的逻辑)
     * @return OkHttpClient实例
     */
    protected OkHttpClient createOkHttpClient(Settings settings){

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(settings.connectTimeout, TimeUnit.MILLISECONDS)
                .writeTimeout(settings.writeTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(settings.readTimeout, TimeUnit.MILLISECONDS);

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
     * @return Request
     * @throws RequestBuildException 构建异常
     */
    protected Request buildPostRequest(String url, String urlSuffix, byte[] body, Settings settings) throws RequestBuildException{
        Request.Builder builder = new Request.Builder()
                .url(url + urlSuffix)
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

    public static class Settings {

        private long passiveBlockDuration = PASSIVE_BLOCK_DURATION;
        private String mediaType = MEDIA_TYPE;
        private String encode = ENCODE;
        private Map<String, String> headers;
        private boolean verboseLog = false;

        private long connectTimeout = 5000L;
        private long writeTimeout = 60000L;
        private long readTimeout = 60000L;
        private long maxReadLength = 10L * 1024L * 1024L;
        private CookieJar cookieJar;
        private Proxy proxy;
        private Dns dns;

    }

}
