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
 * Project GitHub: https://github.com/shepherdviolet/slate
 * Email: shepherdviolet@163.com
 */

package sviolet.slate.common.x.net.loadbalance.inspector;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sviolet.slate.common.x.net.loadbalance.LoadBalanceInspector;

import java.util.concurrent.TimeUnit;

/**
 * 负载均衡--HTTP GET方式探测网络状况
 *
 * @author S.Violet
 */
public class HttpGetLoadBalanceInspector implements LoadBalanceInspector {

    private static final int HTTP_SUCCESS = 200;
    private static final long DEFAULT_TIMEOUT = 2000L;

    private Logger logger = LoggerFactory.getLogger(getClass());

    private volatile OkHttpClient client;
    private volatile String urlSuffix;
    private boolean verboseLog = false;

    public HttpGetLoadBalanceInspector() {
        this("", DEFAULT_TIMEOUT);
    }

    /**
     * @param urlSuffix 探测URL的后缀
     * @param timeout 探测超时时间
     */
    public HttpGetLoadBalanceInspector(String urlSuffix, long timeout) {
        this.urlSuffix = urlSuffix;
        setTimeout(timeout);
    }

    @Override
    public boolean inspect(String url, long timeout) {
        //组装request
        Request request;
        Response response = null;
        try {
            request = new Request.Builder()
                    .url(url + urlSuffix)
                    .get()
                    .build();
        } catch (Throwable t) {
            if (logger.isErrorEnabled()){
                logger.error("Inspect: invalid url " + url, t);
            }
            return false;
        }
        //GET请求
        try {
            response = client.newCall(request).execute();
            if (response.code() == HTTP_SUCCESS){
                return true;
            }
        } catch (Throwable t) {
            if (verboseLog){
                logger.warn("Inspect: error, url " + url, t);
            }
        } finally {
            if (response != null){
                try {
                    response.close();
                } catch (Exception ignored){
                }
            }
        }
        return false;
    }

    /**
     * [可运行时修改(不建议频繁修改)]
     * 设置单次探测网络超时时间(必须), 建议为LoadBalancedInspectManager.setInspectInterval设置值的1/4
     */
    public void setTimeout(long timeout){
        //除以2, 因为网络超时包括连接/写入/读取超时, 避免过长
        timeout = timeout >> 1;
        if (timeout <= 0){
            throw new IllegalArgumentException("timeout must > 1 (usually > 1000)");
        }
        client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * [可运行时修改]
     * 设置探测页面的后缀URL
     * @param urlSuffix 探测页面后缀URL
     */
    public void setUrlSuffix(String urlSuffix) {
        this.urlSuffix = urlSuffix;
    }

    /**
     * 是否输出调试日志
     */
    public void setVerboseLog(boolean verboseLog) {
        this.verboseLog = verboseLog;
    }
}
