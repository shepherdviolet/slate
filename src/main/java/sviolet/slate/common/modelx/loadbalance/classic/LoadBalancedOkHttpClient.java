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
import sviolet.slate.common.modelx.loadbalance.LoadBalancedInspectManager;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public abstract class LoadBalancedOkHttpClient {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private OkHttpClient okHttpClient;
    private LoadBalancedHostManager hostManager;
    private LoadBalancedInspectManager inspectManager;

    private long passiveBlockTimeout;

    public LoadBalancedOkHttpClient() {
        okHttpClient = initOkHttpClient();
    }

    public String postSync(String urlSuffix, String body) throws NoHostException, RequestBuildException, IOException, HttpRejectException {

        LoadBalancedHostManager.Host host = hostManager.nextHost();
        if (host == null){
            throw new NoHostException("No host");
        }

        Request request;
        try {
            request = buildPostRequest(host.getUrl(), urlSuffix, body);
        } catch (Throwable t) {
            throw new RequestBuildException("Error while building request", t);
        }

        if (request == null) {
            throw new RequestBuildException("Null request built");
        }
        Response response = null;
        try {
            response = okHttpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new HttpRejectException(response.code());
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null){
                return "";
            }
            return new String(responseBody.bytes(), "UTF-8");
        } catch (Throwable t) {
            if (t instanceof SocketTimeoutException ||
                    t instanceof UnknownHostException) {
                host.block(passiveBlockTimeout);
                if (logger.isInfoEnabled()){
                    logger.info("Block " + host.getUrl());
                }
            }
            throw t;
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (Exception ignored){
                }
            }
        }

    }

    protected abstract OkHttpClient initOkHttpClient();

    protected abstract Request buildPostRequest(String url, String urlSuffix, String body);

    public void setHostManager(LoadBalancedHostManager hostManager) {
        this.hostManager = hostManager;
    }

    public void setInspectManager(LoadBalancedInspectManager inspectManager) {
        this.inspectManager = inspectManager;
    }

    public void setPassiveBlockTimeout(long passiveBlockTimeout) {
        this.passiveBlockTimeout = passiveBlockTimeout;
    }
}
