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

package sviolet.slate.common.modelx.loadbalance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sviolet.slate.common.modelx.loadbalance.classic.HttpRejectException;
import sviolet.slate.common.modelx.loadbalance.classic.MultiHostOkHttpClient;
import sviolet.slate.common.modelx.loadbalance.classic.NoHostException;
import sviolet.slate.common.modelx.loadbalance.classic.RequestBuildException;
import sviolet.slate.common.modelx.loadbalance.inspector.TelnetLoadBalanceInspector;

import java.io.IOException;
import java.io.InputStream;

/**
 * 支持均衡负载的OkHttpClient测试案例
 */
public class MultiHostOkHttpClientTest {

    private static Logger logger = LoggerFactory.getLogger(MultiHostOkHttpClientTest.class);

    public static void main(String[] args) throws NoHostException, RequestBuildException, HttpRejectException, IOException {

        LoadBalancedHostManager hostManager = new LoadBalancedHostManager()
                .setHostArray(new String[]{
                        "http://127.0.0.1:8080",
                        "http://127.0.0.1:8081"
                });

        LoadBalancedInspectManager inspectManager = new LoadBalancedInspectManager()
                .setHostManager(hostManager)
                .setInspectInterval(5000L)
                .setInspector(new TelnetLoadBalanceInspector())
                .setVerboseLog(true);

        final MultiHostOkHttpClient client = new MultiHostOkHttpClient()
                .setHostManager(hostManager)
                .setPassiveBlockDuration(3000L)
                .setConnectTimeout(3000L)
                .setWriteTimeout(10000L)
                .setReadTimeout(10000L)
                .setVerboseLog(true);

        // sync

        byte[] response = client.get("/get/json")
                .urlParam("name", "wang wang")
                .urlParam("key", "321")
                .sendForBytes();

        System.out.println(new String(response));

        try (InputStream inputStream = client.get("/get/wildcard")
                .sendForInputStream()) {
            int len;
            byte[] buff = new byte[1024];
            StringBuilder stringBuilder = new StringBuilder();
            while ((len = inputStream.read(buff)) >= 0) {
                stringBuilder.append(new String(buff, 0, len));
            }
            System.out.println(stringBuilder.toString());
        }

        try (MultiHostOkHttpClient.ResponsePackage responsePackage = client.get("/")
                .send()) {
            System.out.println(responsePackage.body().string());
        }

        response = client.post("/post/json")
                .urlParam("traceId", "000000001")
                .body("hello json 1".getBytes())
                .sendForBytes();

        System.out.println(new String(response));

        try (InputStream inputStream = client.post("/post/json")
                .urlParam("traceId", "000000001")
                .body("hello json 2".getBytes())
                .sendForInputStream()) {
            int len;
            byte[] buff = new byte[1024];
            StringBuilder stringBuilder = new StringBuilder();
            while ((len = inputStream.read(buff)) >= 0) {
                stringBuilder.append(new String(buff, 0, len));
            }
            System.out.println(stringBuilder.toString());
        }

        try (MultiHostOkHttpClient.ResponsePackage responsePackage = client.post("/post/json")
                .urlParam("traceId", "000000001")
                .body("hello json 3".getBytes())
                .send()) {
            System.out.println(responsePackage.body().string());
        }

        // async

        client.get("/get/json")
                .urlParam("name", "wang wang")
                .urlParam("key", "321")
                .enqueue(new MultiHostOkHttpClient.BytesCallback() {
                    @Override
                    public void onSucceed(byte[] body) {
                        System.out.println(new String(body));
                    }
                    @Override
                    protected void onErrorBeforeSend(Exception e) {
                        e.printStackTrace();
                    }
                    @Override
                    protected void onErrorAfterSend(Exception e) {
                        e.printStackTrace();
                    }
                });

        client.get("/get/json")
                .urlParam("name", "wang wang")
                .urlParam("key", "654")
                .enqueue(new MultiHostOkHttpClient.InputStreamCallback() {
                    @Override
                    public void onSucceed(InputStream inputStream) throws Exception {
                        int len;
                        byte[] buff = new byte[1024];
                        StringBuilder stringBuilder = new StringBuilder();
                        while ((len = inputStream.read(buff)) >= 0) {
                            stringBuilder.append(new String(buff, 0, len));
                        }
                        System.out.println(stringBuilder.toString());
                    }
                    @Override
                    protected void onErrorBeforeSend(Exception e) {
                        e.printStackTrace();
                    }
                    @Override
                    protected void onErrorAfterSend(Exception e) {
                        e.printStackTrace();
                    }
                });

        client.get("/get/json")
                .urlParam("name", "wang wang")
                .urlParam("key", "987")
                .enqueue(new MultiHostOkHttpClient.ResponsePackageCallback() {
                    @Override
                    public void onSucceed(MultiHostOkHttpClient.ResponsePackage responsePackage) throws Exception {
                        System.out.println(responsePackage.body().string());
                    }
                    @Override
                    protected void onErrorBeforeSend(Exception e) {
                        e.printStackTrace();
                    }
                    @Override
                    protected void onErrorAfterSend(Exception e) {
                        e.printStackTrace();
                    }
                });

        client.post("/post/json")
                .urlParam("traceId", "000000001")
                .body("hello json 4".getBytes())
                .enqueue(new MultiHostOkHttpClient.BytesCallback() {
                    @Override
                    public void onSucceed(byte[] body) {
                        System.out.println(new String(body));
                    }
                    @Override
                    protected void onErrorBeforeSend(Exception e) {
                        e.printStackTrace();
                    }
                    @Override
                    protected void onErrorAfterSend(Exception e) {
                        e.printStackTrace();
                    }
                });

        client.post("/post/json")
                .urlParam("traceId", "000000001")
                .body("hello json 5".getBytes())
                .enqueue(new MultiHostOkHttpClient.InputStreamCallback() {
                    @Override
                    public void onSucceed(InputStream inputStream) throws Exception {
                        int len;
                        byte[] buff = new byte[1024];
                        StringBuilder stringBuilder = new StringBuilder();
                        while ((len = inputStream.read(buff)) >= 0) {
                            stringBuilder.append(new String(buff, 0, len));
                        }
                        System.out.println(stringBuilder.toString());
                    }
                    @Override
                    protected void onErrorBeforeSend(Exception e) {
                        e.printStackTrace();
                    }
                    @Override
                    protected void onErrorAfterSend(Exception e) {
                        e.printStackTrace();
                    }
                });

        client.post("/post/json")
                .urlParam("traceId", "000000001")
                .body("hello json 6".getBytes())
                .enqueue(new MultiHostOkHttpClient.ResponsePackageCallback() {
                    @Override
                    public void onSucceed(MultiHostOkHttpClient.ResponsePackage responsePackage) throws Exception {
                        System.out.println(responsePackage.body().string());
                    }
                    @Override
                    protected void onErrorBeforeSend(Exception e) {
                        e.printStackTrace();
                    }
                    @Override
                    protected void onErrorAfterSend(Exception e) {
                        e.printStackTrace();
                    }
                });

//        System.exit(0);

    }

}
