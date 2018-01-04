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
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * 支持均衡负载的OkHttpClient(简单的示例模板, 建议自行实现)
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

    private long passiveBlockDuration = PASSIVE_BLOCK_DURATION;
    private String mediaType = MEDIA_TYPE;
    private String encode = ENCODE;
    private boolean verboseLog = false;

    public LoadBalancedOkHttpClient() {
        okHttpClient = initOkHttpClient();
    }

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
        this.passiveBlockDuration = passiveBlockDuration;
    }

    /**
     * 设置MediaType
     * @param mediaType 设置MediaType
     */
    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    /**
     * 设置编码
     * @param encode 编码
     */
    public void setEncode(String encode) {
        this.encode = encode;
    }

    /**
     * @param verboseLog true:打印更多的调试日志, 默认关闭
     */
    public void setVerboseLog(boolean verboseLog) {
        this.verboseLog = verboseLog;
    }

    /**
     * 同步POST请求
     * @param urlSuffix url后缀
     * @param body 报文体
     * @return 二进制数据(可能为null)
     * @throws NoHostException 当前没有可发送的后端
     * @throws RequestBuildException 构建Request对象时报错
     * @throws IOException 网络通讯异常
     * @throws HttpRejectException Http请求拒绝异常
     */
    public byte[] syncPostForBytes(String urlSuffix, byte[] body) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        ResponseBody responseBody = null;
        try {
            responseBody = syncPost(urlSuffix, body);
            //返回空
            if (responseBody == null) {
                return null;
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
     * @throws NoHostException 当前没有可发送的后端
     * @throws RequestBuildException 构建Request对象时报错
     * @throws IOException 网络通讯异常
     * @throws HttpRejectException Http请求拒绝异常
     */
    public InputStream syncPostForInputStream(String urlSuffix, byte[] body) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        ResponseBody responseBody = syncPost(urlSuffix, body);
        //返回空
        if (responseBody == null) {
            return null;
        }
        //返回二进制数据
        return responseBody.byteStream();
    }

    /**
     * 同步GET请求
     * @param urlSuffix url后缀
     * @param params 请求参数
     * @return 二进制数据(可能为null)
     * @throws NoHostException 当前没有可发送的后端
     * @throws RequestBuildException 构建Request对象时报错
     * @throws IOException 网络通讯异常
     * @throws HttpRejectException Http请求拒绝异常
     */
    public byte[] syncGetForBytes(String urlSuffix, Map<String, Object> params) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        ResponseBody responseBody = null;
        try {
            responseBody = syncGet(urlSuffix, params);
            //返回空
            if (responseBody == null) {
                return null;
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
     * @throws NoHostException 当前没有可发送的后端
     * @throws RequestBuildException 构建Request对象时报错
     * @throws IOException 网络通讯异常
     * @throws HttpRejectException Http请求拒绝异常
     */
    public InputStream syncGetForInputStream(String urlSuffix, Map<String, Object> params) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        ResponseBody responseBody = syncGet(urlSuffix, params);
        //返回空
        if (responseBody == null) {
            return null;
        }
        //返回二进制数据
        return responseBody.byteStream();
    }

    private ResponseBody syncPost(String urlSuffix, byte[] body) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        //获取远端
        LoadBalancedHostManager.Host host = fetchHost();

        if (verboseLog) {
            logger.debug("POST url:" + host.getUrl() + ", suffix:" + urlSuffix + ", body:" + ByteUtils.bytesToHex(body));
        }

        //装配Request
        Request request;
        try {
            request = buildPostRequest(host.getUrl(), urlSuffix, body);
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

    private ResponseBody syncGet(String urlSuffix, Map<String, Object> params) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        //获取远端
        LoadBalancedHostManager.Host host = fetchHost();

        if (verboseLog) {
            logger.debug("GET url:" + host.getUrl() + ", suffix:" + urlSuffix + ", params:" + params);
        }

        //装配Request
        Request request;
        try {
            request = buildGetRequest(host.getUrl(), urlSuffix, params);
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

    private ResponseBody syncCall(LoadBalancedHostManager.Host host, Request request) throws IOException, HttpRejectException {
        try {
            //同步请求
            Response response = okHttpClient.newCall(request).execute();
            //Http拒绝
            if (!response.isSuccessful()) {
                throw new HttpRejectException(response.code());
            }
            //报文体
            return response.body();
        } catch (Throwable t) {
            if (t instanceof ConnectException ||
                    t instanceof SocketTimeoutException ||
                    t instanceof UnknownHostException) {
                //网络故障阻断后端
                host.block(passiveBlockDuration);
                if (logger.isInfoEnabled()){
                    logger.info("Block " + host.getUrl() + " " + passiveBlockDuration);
                }
            }
            throw t;
        }
    }

    /**
     * 初始化OkHttpClient实例(复写本方法实现自定义的逻辑)
     * @return OkHttpClient实例
     */
    protected OkHttpClient initOkHttpClient(){
        return new OkHttpClient();
    }

    /**
     * 根据URL和报文体组POST请求(复写本方法实现自定义的逻辑)
     * @param url 由LoadBalancedHostManager选择出的远端url(前缀)
     * @param urlSuffix URL后缀
     * @param body 报文体
     * @return Request
     * @throws RequestBuildException 构建异常
     */
    protected Request buildPostRequest(String url, String urlSuffix, byte[] body) throws RequestBuildException{
        return new Request.Builder()
                .url(url + urlSuffix)
                .post(RequestBody.create(MediaType.parse(mediaType), body))
                .build();
    }

    /**
     * 根据URL和报文体组GET请求(复写本方法实现自定义的逻辑)
     * @param url 由LoadBalancedHostManager选择出的远端url(前缀)
     * @param urlSuffix URL后缀
     * @param params 请求参数
     * @return Request
     * @throws RequestBuildException 构建异常
     */
    protected Request buildGetRequest(String url, String urlSuffix, Map<String, Object> params) throws RequestBuildException{
        HttpUrl httpUrl = HttpUrl.parse(url + urlSuffix);
        if (httpUrl == null){
            throw new RequestBuildException("Invalid url:" + url + urlSuffix);
        }

        if (params != null){
            HttpUrl.Builder httpUrlBuilder = httpUrl.newBuilder();
            for (Map.Entry<String, Object> param : params.entrySet()) {
                try {
                    httpUrlBuilder.addEncodedQueryParameter(param.getKey(), URLEncoder.encode(param.getValue() != null ? param.getValue().toString() : "", encode));
                } catch (UnsupportedEncodingException e) {
                    throw new RequestBuildException("Error while encode to url format", e);
                }
            }
            httpUrl = httpUrlBuilder.build();
        }

        return new Request.Builder()
                .url(httpUrl)
                .get()
                .build();
    }

}
