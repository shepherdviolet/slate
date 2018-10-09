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

package sviolet.slate.common.x.net.loadbalance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sviolet.slate.common.x.net.loadbalance.classic.HttpRejectException;
import sviolet.slate.common.x.net.loadbalance.classic.LoadBalancedOkHttpClient;
import sviolet.slate.common.x.net.loadbalance.classic.NoHostException;
import sviolet.slate.common.x.net.loadbalance.classic.RequestBuildException;
import sviolet.slate.common.x.net.loadbalance.inspector.TelnetLoadBalanceInspector;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 支持均衡负载的OkHttpClient测试案例
 */
public class LoadBalancedOkHttpClientTest {

    private static Logger logger = LoggerFactory.getLogger(LoadBalancedOkHttpClientTest.class);

    public static void main(String[] args) {

        LoadBalancedHostManager hostManager = new LoadBalancedHostManager();
        hostManager.setHostArray(new String[]{
                "http://127.0.0.1:8080",
                "http://127.0.0.1:8081"
        });

        LoadBalancedInspectManager inspectManager = new LoadBalancedInspectManager();
        inspectManager.setHostManager(hostManager);
        inspectManager.setInspectInterval(5000L);
        inspectManager.setInspector(new TelnetLoadBalanceInspector());
        inspectManager.setVerboseLog(true);

        final LoadBalancedOkHttpClient client = new LoadBalancedOkHttpClient();
        client.setHostManager(hostManager);
        client.setPassiveBlockDuration(3000L);
        client.setConnectTimeout(3000L);
        client.setWriteTimeout(10000L);
        client.setReadTimeout(10000L);
//        client.setProxy("127.0.0.1:17711");

        postTask(client);
//        getTask(client);

    }

    private static void postTask(final LoadBalancedOkHttpClient client) {
        for (int i = 0 ; i < 4 ; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 10000; i++) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ignore) {
                        }
                        try {
                            byte[] response = client.syncPostForBytes("/post/json", "hello".getBytes("utf-8"));
                            logger.debug("response:" + (response != null ? new String(response, "UTF-8") : "null"));
                        } catch (NoHostException e) {
                            logger.error("error: no host");
                        } catch (RequestBuildException e) {
                            logger.error("error: request build");
                        } catch (IOException e) {
                            logger.error("error: io " + e.getMessage());
                        } catch (HttpRejectException e) {
                            logger.error("reject: " + e.getResponseCode());
                        }
                    }
                }
            }).start();
        }
    }

    private static void getTask(final LoadBalancedOkHttpClient client) {
        for (int i = 0 ; i < 4 ; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 10000; i++) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ignore) {
                        }
                        try {
                            Map<String, Object> params = new HashMap<>(2);
                            params.put("name", "tester");
                            params.put("key", "123456");
                            byte[] response = client.syncGetForBytes("/get/json", params);
                            logger.debug("response:" + (response != null ? new String(response, "UTF-8") : "null"));
                        } catch (NoHostException e) {
                            logger.error("error: no host");
                        } catch (RequestBuildException e) {
                            logger.error("error: request build");
                        } catch (IOException e) {
                            logger.error("error: io " + e.getMessage());
                        } catch (HttpRejectException e) {
                            logger.error("reject: " + e.getResponseCode());
                        }
                    }
                }
            }).start();
        }
    }

}
