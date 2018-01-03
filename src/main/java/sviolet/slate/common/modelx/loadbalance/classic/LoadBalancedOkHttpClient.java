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

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sviolet.slate.common.modelx.loadbalance.LoadBalancedHostManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * 支持均衡负载的OkHttpClient(简单的示例模板, 建议自行实现)
 *
 * @author S.Violet
 */
public abstract class LoadBalancedOkHttpClient <BodyType> {

    private static final long PASSIVE_BLOCK_DURATION = 3000L;

    private Logger logger = LoggerFactory.getLogger(getClass());

    private OkHttpClient okHttpClient;
    private LoadBalancedHostManager hostManager;

    private long passiveBlockDuration = PASSIVE_BLOCK_DURATION;

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
     * 同步POST请求
     * @param urlSuffix url后缀
     * @param body 报文体
     * @return 二进制数据(可能为null)
     * @throws NoHostException 当前没有可发送的后端
     * @throws RequestBuildException 构建Request对象时报错
     * @throws IOException 网络通讯异常
     * @throws HttpRejectException Http请求拒绝异常
     */
    public byte[] syncPostForBytes(String urlSuffix, BodyType body) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
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
    public InputStream syncPostForInputStream(String urlSuffix, BodyType body) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        ResponseBody responseBody = syncPost(urlSuffix, body);
        //返回空
        if (responseBody == null) {
            return null;
        }
        //返回二进制数据
        return responseBody.byteStream();
    }

    private ResponseBody syncPost(String urlSuffix, BodyType body) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        //获取远端
        LoadBalancedHostManager.Host host = hostManager.nextHost();
        if (host == null){
            throw new NoHostException("No host");
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

        //请求
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
            if (t instanceof SocketTimeoutException ||
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
     * 初始化OkHttpClient实例
     * @return OkHttpClient实例
     */
    protected abstract OkHttpClient initOkHttpClient();

    /**
     * 根据URL和报文体
     * @param url 由LoadBalancedHostManager选择出的远端url(前缀)
     * @param urlSuffix URL后缀
     * @param body 报文体
     * @return Request
     * @throws RequestBuildException 构建异常
     */
    protected abstract Request buildPostRequest(String url, String urlSuffix, BodyType body) throws RequestBuildException;

}
