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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sviolet.slate.common.modelx.loadbalance.LoadBalancedHostManager;
import sviolet.thistle.util.conversion.ByteUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Map;

/**
 * 支持均衡负载的HttpUrlConnection Client(简单的示例模板, 建议自行实现)
 *
 * <pre>{@code
 *  LoadBalancedHostManager hostManager = new LoadBalancedHostManager();
 *  hostManager.setHostArray(new String[]{
 *      "http://127.0.0.1:8080",
 *      "http://127.0.0.1:8081"
 *  });
 *
 *  LoadBalancedInspectManager inspectManager = new LoadBalancedInspectManager();
 *  inspectManager.setHostManager(hostManager);
 *  inspectManager.setInspectInterval(5000L);
 *  inspectManager.setInspector(new TelnetLoadBalanceInspector());
 *  inspectManager.setVerboseLog(true);
 *
 *  LoadBalancedHttpUrlConnClient client = new LoadBalancedHttpUrlConnClient();
 *  client.setHostManager(hostManager);
 *  client.setPassiveBlockDuration(3000L);
 *  client.setConnectTimeout(3000);
 *  client.setReadTimeout(10000);
 * }</pre>
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
 *  <bean id="loadBalancedHttpUrlConnClient" class="sviolet.slate.common.modelx.loadbalance.classic.LoadBalancedHttpUrlConnClient">
 *      <property name="hostManager" ref="loadBalancedHostManager"/>
 *      <property name="passiveBlockDuration" value="3000"/>
 *      <property name="connectTimeout" value="3000"/>
 *      <property name="readTimeout" value="10000"/>
 *  </bean>
 *
 *  }</pre>
 *
 * @author S.Violet
 */
public class LoadBalancedHttpUrlConnClient {

    private static final String CONTENT_TYPE = "Content-type";
    private static final String POST = "POST";
    private static final String GET = "GET";

    private static final long PASSIVE_BLOCK_DURATION = 3000L;
    private static final String MEDIA_TYPE = "application/json;charset=utf-8";
    private static final String ENCODE = "utf-8";

    private Logger logger = LoggerFactory.getLogger(getClass());

    private LoadBalancedHostManager hostManager;

    private Settings settings = new Settings();

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
    public void setConnectTimeout(int connectTimeout) {
        settings.connectTimeout = connectTimeout;
    }

    /**
     * 设置读数据超时ms
     * @param readTimeout 读数据超时ms
     */
    public void setReadTimeout(int readTimeout) {
        settings.readTimeout = readTimeout;
    }

    /**
     * Proxy
     * @param proxy 例如127.0.0.1:8080
     * @throws IllegalArgumentException if the proxy string is invalid
     * @throws NumberFormatException  if the string does not contain a parsable integer.
     * @throws SecurityException if a security manager is present and permission to resolve the host name is denied.
     */
    public void setProxy(String proxy) {
        Proxy proxyObj;
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
        settings.proxy = proxyObj;
    }

    //Request /////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 同步POST请求
     * @param urlSuffix url后缀
     * @param body 报文体
     * @return 二进制数据(可能为null)
     * @throws NoHostException 当前没有可发送的后端
     * @throws IOException 网络通讯异常
     * @throws HttpRejectException Http请求拒绝异常
     */
    public byte[] syncPostForBytes(String urlSuffix, byte[] body) throws NoHostException, IOException, HttpRejectException {
        HttpURLConnection httpURLConnection = null;
        InputStream inputStream = null;
        try {
            httpURLConnection = syncPost(urlSuffix, body);
            inputStream = httpURLConnection.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int length;
            byte[] buff = new byte[4096];
            while ((length = inputStream.read(buff)) >= 0) {
                outputStream.write(buff, 0, length);
            }
            return outputStream.toByteArray();
        } finally {
            if (inputStream != null){
                try {
                    inputStream.close();
                } catch (Throwable ignore){
                }
            }
            if (httpURLConnection != null){
                try {
                    httpURLConnection.disconnect();
                } catch (Throwable ignore){
                }
            }
        }
    }

    /**
     * 同步POST请求
     * @param urlSuffix url后缀
     * @param body 报文体
     * @return InputStream(可能为null), 注意:使用完必须关闭流!!!
     * @throws NoHostException 当前没有可发送的后端
     * @throws IOException 网络通讯异常
     * @throws HttpRejectException Http请求拒绝异常
     */
    public HttpURLConnection syncPost(String urlSuffix, byte[] body) throws NoHostException, IOException, HttpRejectException {
        LoadBalancedHostManager.Host host = fetchHost();

        if (settings.verboseLog) {
            logger.debug("POST url:" + host.getUrl() + ", suffix:" + urlSuffix + ", body:" + ByteUtils.bytesToHex(body));
        }

        URL url = new URL(host.getUrl() + urlSuffix);

        if (logger.isDebugEnabled()) {
            logger.debug("POST real url:" + url.toString());
        }

        HttpURLConnection urlConnection = null;
        OutputStream outputStream = null;
        try {
            Proxy proxy = settings.proxy;
            if (proxy == null) {
                urlConnection = (HttpURLConnection)url.openConnection();
            } else {
                urlConnection = (HttpURLConnection)url.openConnection(proxy);
            }

            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setRequestMethod(POST);
            urlConnection.setConnectTimeout(settings.connectTimeout);
            urlConnection.setReadTimeout(settings.readTimeout);
            urlConnection.setRequestProperty(CONTENT_TYPE, settings.mediaType);

            Map<String, String> headers = settings.headers;
            if (headers != null){
                for (Map.Entry<String, String> entry : headers.entrySet()){
                    urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            urlConnection.connect();
            outputStream = urlConnection.getOutputStream();
            outputStream.write(body);
            outputStream.flush();

            int code = urlConnection.getResponseCode();
            String message = urlConnection.getResponseMessage();
            if (isSucceed(code)){
                throw new HttpRejectException(code, message);
            }
            return urlConnection;
        } catch (Throwable t) {
            if (needBlock(t, settings)) {
                //网络故障阻断后端
                host.block(settings.passiveBlockDuration);
                if (logger.isInfoEnabled()){
                    logger.info("Block " + host.getUrl() + " " + settings.passiveBlockDuration);
                }
            }
            throw t;
        } finally {
            if (outputStream != null){
                try {
                    outputStream.close();
                } catch (Throwable ignore){
                }
            }
        }
    }

    /**
     * 同步GET请求
     * @param urlSuffix url后缀
     * @param params 请求参数
     * @return 二进制数据(可能为null)
     * @throws NoHostException 当前没有可发送的后端
     * @throws IOException 网络通讯异常
     * @throws HttpRejectException Http请求拒绝异常
     */
    public byte[] syncGetForBytes(String urlSuffix, Map<String, Object> params) throws NoHostException, IOException, HttpRejectException {
        HttpURLConnection httpURLConnection = null;
        InputStream inputStream = null;
        try {
            httpURLConnection = syncGet(urlSuffix, params);
            inputStream = httpURLConnection.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int length;
            byte[] buff = new byte[4096];
            while ((length = inputStream.read(buff)) >= 0) {
                outputStream.write(buff, 0, length);
            }
            return outputStream.toByteArray();
        } finally {
            if (inputStream != null){
                try {
                    inputStream.close();
                } catch (Throwable ignore){
                }
            }
            if (httpURLConnection != null){
                try {
                    httpURLConnection.disconnect();
                } catch (Throwable ignore){
                }
            }
        }
    }

    /**
     * 同步GET请求
     * @param urlSuffix url后缀
     * @param params 请求参数
     * @return InputStream(可能为null), 注意:使用完必须关闭流!!!
     * @throws NoHostException 当前没有可发送的后端
     * @throws IOException 网络通讯异常
     * @throws HttpRejectException Http请求拒绝异常
     */
    public HttpURLConnection syncGet(String urlSuffix, Map<String, Object> params) throws NoHostException, IOException, HttpRejectException {
        LoadBalancedHostManager.Host host = fetchHost();

        if (settings.verboseLog) {
            logger.debug("GET url:" + host.getUrl() + ", suffix:" + urlSuffix + ", params:" + params);
        }

        StringBuilder paramsBuilder = new StringBuilder();
        if (params != null && params.size() > 0) {
            int count = 0;
            paramsBuilder.append("?");
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (count++ > 0){
                    paramsBuilder.append("&");
                }
                paramsBuilder.append(entry.getKey());
                paramsBuilder.append("=");
                paramsBuilder.append(entry.getValue() == null ? "" : URLEncoder.encode(String.valueOf(entry.getValue()), settings.encode));
            }
        }

        URL url = new URL(host.getUrl() + urlSuffix + paramsBuilder.toString());

        if (logger.isDebugEnabled()) {
            logger.debug("GET real url:" + url.toString());
        }

        HttpURLConnection urlConnection = null;
        try {
            Proxy proxy = settings.proxy;
            if (proxy == null) {
                urlConnection = (HttpURLConnection)url.openConnection();
            } else {
                urlConnection = (HttpURLConnection)url.openConnection(proxy);
            }

            urlConnection.setDoOutput(false);
            urlConnection.setDoInput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setRequestMethod(GET);
            urlConnection.setConnectTimeout(settings.connectTimeout);
            urlConnection.setReadTimeout(settings.readTimeout);
            urlConnection.setRequestProperty(CONTENT_TYPE, settings.mediaType);

            Map<String, String> headers = settings.headers;
            if (headers != null){
                for (Map.Entry<String, String> entry : headers.entrySet()){
                    urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            urlConnection.connect();
            int code = urlConnection.getResponseCode();
            String message = urlConnection.getResponseMessage();
            if (isSucceed(code)){
                throw new HttpRejectException(code, message);
            }
            return urlConnection;
        } catch (Throwable t) {
            if (needBlock(t, settings)) {
                //网络故障阻断后端
                host.block(settings.passiveBlockDuration);
                if (logger.isInfoEnabled()){
                    logger.info("Block " + host.getUrl() + " " + settings.passiveBlockDuration);
                }
            }
            throw t;
        }
    }

    private LoadBalancedHostManager.Host fetchHost() throws NoHostException {
        LoadBalancedHostManager.Host host = hostManager.nextHost();
        if (host == null){
            throw new NoHostException("No host");
        }
        return host;
    }

    //Override //////////////////////////////////////////////////////////////////////////////////////////////////////

    protected boolean isSucceed(int code) {
        return code < 200 || code >= 300;
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

        private int connectTimeout = 5000;
        private int readTimeout = 60000;
        private Proxy proxy;

    }

}
