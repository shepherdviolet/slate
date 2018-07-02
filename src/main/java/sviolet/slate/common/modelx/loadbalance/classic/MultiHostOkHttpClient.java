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

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sviolet.slate.common.modelx.loadbalance.LoadBalancedHostManager;
import sviolet.thistle.util.conversion.ByteUtils;
import sviolet.thistle.util.judge.CheckUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>支持均衡负载的OkHttpClient(简单的示例模板, 建议自行实现)</p>
 *
 * <p>Java:</p>
 *
 * <pre>{@code
 *
 *      LoadBalancedHostManager hostManager = new LoadBalancedHostManager()
 *              .setHostArray(new String[]{
 *                  "http://127.0.0.1:8080",
 *                  "http://127.0.0.1:8081"
 *              });
 *
 *      LoadBalancedInspectManager inspectManager = new LoadBalancedInspectManager()
 *              .setHostManager(hostManager)
 *              .setInspectInterval(5000L)
 *              .setInspector(new TelnetLoadBalanceInspector());
 *
 *      MultiHostOkHttpClient client = new MultiHostOkHttpClient()
 *              .setHostManager(hostManager)
 *              .setMaxThreads(200)
 *              .setMaxThreadsPerHost(200)
 *              .setPassiveBlockDuration(3000L)
 *              .setConnectTimeout(3000L)
 *              .setWriteTimeout(10000L)
 *              .setReadTimeout(10000L);
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
 *  <bean id="loadBalancedOkHttpClient" class="sviolet.slate.common.modelx.loadbalance.classic.MultiHostOkHttpClient">
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
 *  <bean id="loadBalancedOkHttpClient" class="sviolet.slate.common.modelx.loadbalance.classic.MultiHostOkHttpClient">
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
public class MultiHostOkHttpClient {

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

    private static Logger logger = LoggerFactory.getLogger(MultiHostOkHttpClient.class);

    private OkHttpClient okHttpClient;
    private LoadBalancedHostManager hostManager;

    private Settings settings = new Settings();
    private boolean refreshSettings = false;
    private ReentrantLock settingsLock = new ReentrantLock();

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 请求 ///////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * <p>创建POST请求</p>
     *
     * <p>同步请求:</p>
     *
     * <pre>
     *
     *  //返回byte[]类型的响应
     *  try {
     *      byte[] response = client.post("/post/json")
     *              .urlParam("traceId", "000000001")
     *              .body("hello world".getBytes())
     *              .sendForBytes();
     *  } catch (NoHostException e) {
     *      //当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
     *  } catch (RequestBuildException e) {
     *      //在网络请求未发送前抛出的异常
     *  } catch (IOException e) {
     *      //网络异常
     *  } catch (HttpRejectException e) {
     *      //HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
     *      //获得拒绝码 e.getResponseCode()
     *      //获得拒绝信息 e.getResponseMessage()
     *  }
     *
     *  //返回InputStream类型的响应
     *  //注意:InputStream需要手动关闭(close)
     *  try (InputStream inputStream = client.post("/post/json")
     *          .body("hello world".getBytes())
     *          .sendForInputStream()) {
     *
     *      inputStream......
     *
     *  } catch (NoHostException e) {
     *      //当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
     *  } catch (RequestBuildException e) {
     *      //在网络请求未发送前抛出的异常
     *  } catch (IOException e) {
     *      //网络异常
     *  } catch (HttpRejectException e) {
     *      //HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
     *      //获得拒绝码 e.getResponseCode()
     *      //获得拒绝信息 e.getResponseMessage()
     *  }
     *
     *  //返回ResponseBody类型的响应
     *  //注意:ResponseBody需要手动关闭(close)
     *  try (ResponseBody responseBody = client.post("/post/json")
     *          .body("hello world".getBytes())
     *          .send()) {
     *
     *      String response = responseBody.string();
     *
     *  } catch (NoHostException e) {
     *      //当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
     *  } catch (RequestBuildException e) {
     *      //在网络请求未发送前抛出的异常
     *  } catch (IOException e) {
     *      //网络异常
     *  } catch (HttpRejectException e) {
     *      //HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
     *      //获得拒绝码 e.getResponseCode()
     *      //获得拒绝信息 e.getResponseMessage()
     *  }
     *
     * </pre>
     *
     * <p>异步请求:</p>
     *
     * <pre>
     *
     *  //返回byte[]类型的响应
     *  client.post("/post/json")
     *          .urlParam("traceId", "000000001")
     *          .body("hello world".getBytes())
     *          .enqueue(new MultiHostOkHttpClient.BytesCallback() {
     *              public void onSucceed(byte[] body) {
     *                  ......
     *              }
     *              protected void onErrorBeforeSend(Exception e) {
     *                  //NoHostException: 当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
     *                  //RequestBuildException: 在网络请求未发送前抛出的异常
     *              }
     *              protected void onErrorAfterSend(Exception e) {
     *                  //IOException: 网络异常
     *                  //HttpRejectException: HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
     *                  //获得拒绝码 e.getResponseCode()
     *                  //获得拒绝信息 e.getResponseMessage()
     *                  //另外, 如果onSucceed方法中抛出异常, 默认会将异常转交到这个方法处理
     *              }
     *          });
     *
     *  //返回InputStream类型的响应
     *  //当autoClose=true时, onSucceed方法回调结束后, 输入流会被自动关闭, 无需手动调用close方法
     *  //当autoClose=false时, onSucceed方法回调结束后, 输入流不会自动关闭, 需要手动调用InputStream.close()关闭, 注意!!!
     *  client.post("/post/json")
     *          .urlParam("traceId", "000000001")
     *          .body("hello world".getBytes())
     *          //.autoClose(false)//默认为true
     *          .enqueue(new MultiHostOkHttpClient.InputStreamCallback() {
     *              public void onSucceed(InputStream inputStream) throws Exception {
     *                  ......
     *              }
     *              protected void onErrorBeforeSend(Exception e) {
     *                  //NoHostException: 当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
     *                  //RequestBuildException: 在网络请求未发送前抛出的异常
     *              }
     *              protected void onErrorAfterSend(Exception e) {
     *                  //IOException: 网络异常
     *                  //HttpRejectException: HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
     *                  //获得拒绝码 e.getResponseCode()
     *                  //获得拒绝信息 e.getResponseMessage()
     *                  //另外, 如果onSucceed方法中抛出异常, 默认会将异常转交到这个方法处理
     *              }
     *          });
     *
     *  //返回ResponseBody类型的响应
     *  //当autoClose=true时, onSucceed方法回调结束后, ResponseBody会被自动关闭, 无需手动调用close方法
     *  //当autoClose=false时, onSucceed方法回调结束后, ResponseBody不会自动关闭, 需要手动调用ResponseBody.close()关闭, 注意!!!
     *  client.post("/post/json")
     *          .urlParam("traceId", "000000001")
     *          .body("hello world".getBytes())
     *          //.autoClose(false)//默认为true
     *          .enqueue(new MultiHostOkHttpClient.ResponseBodyCallback() {
     *              public void onSucceed(ResponseBody responseBody) throws Exception {
     *                  ......
     *              }
     *              protected void onErrorBeforeSend(Exception e) {
     *                  //NoHostException: 当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
     *                  //RequestBuildException: 在网络请求未发送前抛出的异常
     *              }
     *              protected void onErrorAfterSend(Exception e) {
     *                  //IOException: 网络异常
     *                  //HttpRejectException: HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
     *                  //获得拒绝码 e.getResponseCode()
     *                  //获得拒绝信息 e.getResponseMessage()
     *                  //另外, 如果onSucceed方法中抛出异常, 默认会将异常转交到这个方法处理
     *              }
     *          });
     *
     * </pre>
     *
     * @param urlSuffix 请求的url后缀, 例如/user/add.json
     */
    public Request post(String urlSuffix) {
        return new Request(this, urlSuffix, true);
    }

    /**
     * <p>创建GET请求</p>
     *
     * <p>同步请求:</p>
     *
     * <pre>
     *
     *  //返回byte[]类型的响应
     *  try {
     *      byte[] response = client.get("/get/json")
     *              .urlParam("name", "000000001")
     *              .urlParam("key", "000000001")
     *              .sendForBytes();
     *  } catch (NoHostException e) {
     *      //当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
     *  } catch (RequestBuildException e) {
     *      //在网络请求未发送前抛出的异常
     *  } catch (IOException e) {
     *      //网络异常
     *  } catch (HttpRejectException e) {
     *      //HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
     *      //获得拒绝码 e.getResponseCode()
     *      //获得拒绝信息 e.getResponseMessage()
     *  }
     *
     *  //返回InputStream类型的响应
     *  //注意:InputStream需要手动关闭(close)
     *  try (InputStream inputStream = client.get("/get/json")
     *          .urlParam("name", "000000001")
     *          .urlParam("key", "000000001")
     *          .sendForInputStream()) {
     *
     *      inputStream......
     *
     *  } catch (NoHostException e) {
     *      //当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
     *  } catch (RequestBuildException e) {
     *      //在网络请求未发送前抛出的异常
     *  } catch (IOException e) {
     *      //网络异常
     *  } catch (HttpRejectException e) {
     *      //HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
     *      //获得拒绝码 e.getResponseCode()
     *      //获得拒绝信息 e.getResponseMessage()
     *  }
     *
     *  //返回ResponseBody类型的响应
     *  //注意:ResponseBody需要手动关闭(close)
     *  try (ResponseBody responseBody = client.get("/get/json")
     *          .urlParam("name", "000000001")
     *          .urlParam("key", "000000001")
     *          .send()) {
     *
     *      String response = responseBody.string();
     *
     *  } catch (NoHostException e) {
     *      //当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
     *  } catch (RequestBuildException e) {
     *      //在网络请求未发送前抛出的异常
     *  } catch (IOException e) {
     *      //网络异常
     *  } catch (HttpRejectException e) {
     *      //HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
     *      //获得拒绝码 e.getResponseCode()
     *      //获得拒绝信息 e.getResponseMessage()
     *  }
     *
     * </pre>
     *
     * <p>异步请求:</p>
     *
     * <pre>
     *
     *  //返回byte[]类型的响应
     *  client.get("/get/json")
     *          .urlParam("name", "000000001")
     *          .urlParam("key", "000000001")
     *          .enqueue(new MultiHostOkHttpClient.BytesCallback() {
     *              public void onSucceed(byte[] body) {
     *                  ......
     *              }
     *              protected void onErrorBeforeSend(Exception e) {
     *                  //NoHostException: 当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
     *                  //RequestBuildException: 在网络请求未发送前抛出的异常
     *              }
     *              protected void onErrorAfterSend(Exception e) {
     *                  //IOException: 网络异常
     *                  //HttpRejectException: HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
     *                  //获得拒绝码 e.getResponseCode()
     *                  //获得拒绝信息 e.getResponseMessage()
     *                  //另外, 如果onSucceed方法中抛出异常, 默认会将异常转交到这个方法处理
     *              }
     *          });
     *
     *  //返回InputStream类型的响应
     *  //当autoClose=true时, onSucceed方法回调结束后, 输入流会被自动关闭, 无需手动调用close方法
     *  //当autoClose=false时, onSucceed方法回调结束后, 输入流不会自动关闭, 需要手动调用InputStream.close()关闭, 注意!!!
     *  client.get("/get/json")
     *          .urlParam("name", "000000001")
     *          .urlParam("key", "000000001")
     *          //.autoClose(false)//默认为true
     *          .enqueue(new MultiHostOkHttpClient.InputStreamCallback() {
     *              public void onSucceed(InputStream inputStream) throws Exception {
     *                  ......
     *              }
     *              protected void onErrorBeforeSend(Exception e) {
     *                  //NoHostException: 当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
     *                  //RequestBuildException: 在网络请求未发送前抛出的异常
     *              }
     *              protected void onErrorAfterSend(Exception e) {
     *                  //IOException: 网络异常
     *                  //HttpRejectException: HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
     *                  //获得拒绝码 e.getResponseCode()
     *                  //获得拒绝信息 e.getResponseMessage()
     *                  //另外, 如果onSucceed方法中抛出异常, 默认会将异常转交到这个方法处理
     *              }
     *          });
     *
     *  //返回ResponseBody类型的响应
     *  //当autoClose=true时, onSucceed方法回调结束后, ResponseBody会被自动关闭, 无需手动调用close方法
     *  //当autoClose=false时, onSucceed方法回调结束后, ResponseBody不会自动关闭, 需要手动调用ResponseBody.close()关闭, 注意!!!
     *  client.get("/get/json")
     *          .urlParam("name", "000000001")
     *          .urlParam("key", "000000001")
     *          //.autoClose(false)//默认为true
     *          .enqueue(new MultiHostOkHttpClient.ResponseBodyCallback() {
     *              public void onSucceed(ResponseBody responseBody) throws Exception {
     *                  ......
     *              }
     *              protected void onErrorBeforeSend(Exception e) {
     *                  //NoHostException: 当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
     *                  //RequestBuildException: 在网络请求未发送前抛出的异常
     *              }
     *              protected void onErrorAfterSend(Exception e) {
     *                  //IOException: 网络异常
     *                  //HttpRejectException: HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
     *                  //获得拒绝码 e.getResponseCode()
     *                  //获得拒绝信息 e.getResponseMessage()
     *                  //另外, 如果onSucceed方法中抛出异常, 默认会将异常转交到这个方法处理
     *              }
     *          });
     *
     * </pre>
     *
     * @param urlSuffix 请求的url后缀, 例如/user/add.json
     */
    public Request get(String urlSuffix) {
        return new Request(this, urlSuffix, false);
    }

    /**
     * 请求
     */
    public static class Request {

        private WeakReference<MultiHostOkHttpClient> clientReference;
        private boolean isSend = false;

        private String urlSuffix;
        private boolean isPost = false;
        private Map<String, String> headers;
        private Map<String, Object> urlParams;
        private byte[] body;

        private boolean autoClose = true;
        private long passiveBlockDuration = -1;
        private String mediaType;
        private String encode;

        private Request(MultiHostOkHttpClient client, String urlSuffix, boolean isPost) {
            this.clientReference = new WeakReference<>(client);
            this.urlSuffix = urlSuffix;
            this.isPost = isPost;
        }

        /**
         * <p>[配置]URL参数, 即HTTP请求中URL后面跟随的?key=value&key=value</p>
         */
        public Request urlParams(Map<String, Object> urlParams) {
            this.urlParams = urlParams;
            return this;
        }

        /**
         * <p>[配置]添加一个URL参数, 即HTTP请求中URL后面跟随的?key=value&key=value</p>
         */
        public Request urlParam(String key, Object value) {
            if (this.urlParams == null) {
                this.urlParams = new HashMap<>(8);
            }
            this.urlParams.put(key, value);
            return this;
        }

        /**
         * <p>[配置]POST请求专用: 报文体数据</p>
         */
        public Request body(byte[] body) {
            this.body = body;
            return this;
        }

        /**
         * <p>[配置]HTTP请求头参数, 客户端配置和此处配置的均生效(此处配置优先)</p>
         */
        public Request httpHeaders(Map<String, String> httpHeaders) {
            this.headers = httpHeaders;
            return this;
        }

        /**
         * <p>[配置]添加一个HTTP请求头参数, 客户端配置和此处配置的均生效(此处配置优先)</p>
         */
        public Request httpHeader(String key, String value) {
            if (this.headers == null) {
                this.headers = new HashMap<>(8);
            }
            this.headers.put(key, value);
            return this;
        }

        /**
         * <p>[配置]设置被动检测到网络故障时阻断后端的时间, 客户端配置和此处配置的均生效(此处配置优先)</p>
         *
         * <p>当请求服务端时, 发生特定的异常或返回特定的响应码(MultiHostOkHttpClient.needBlock方法决定), 客户端会将该
         * 后端服务器的IP/PORT标记为暂不可用状态, 而阻断时长是不可用的时长</p>
         *
         * @param passiveBlockDuration 阻断时长ms
         */
        public Request setPassiveBlockDuration(long passiveBlockDuration) {
            this.passiveBlockDuration = passiveBlockDuration;
            return this;
        }

        /**
         * <p>[配置]报文体MediaType, 客户端配置和此处配置的均生效(此处配置优先)</p>
         */
        public Request setMediaType(String mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        /**
         * <p>[配置]字符编码, 客户端配置和此处配置的均生效(此处配置优先)</p>
         */
        public Request setEncode(String encode) {
            this.encode = encode;
            return this;
        }

        /**
         * <p>[配置]异步请求专用: 配置响应实例(或输入流)是否在回调方法onSucceed结束后自动关闭, 默认true</p>
         *
         * <p>注意:同步请求返回的ResponseBode/InputStream是必须手动关闭的!!!</p>
         *
         * <p>
         * 当autoClose=true时, onSucceed方法回调结束后, ResponseBody/InputStream会被自动关闭, 无需手动调用close方法. 适用于
         * 响应数据在回调方法中处理完的场合.<br>
         * 当autoClose=false时, onSucceed方法回调结束后, ResponseBody/InputStream不会自动关闭, 需要手动调用ResponseBody.close()关闭,
         * 注意!!! 适用于响应数据需要交由其他的线程处理, 或暂时持有的场合使用.
         * </p>
         */
        public Request autoClose(boolean autoClose){
            this.autoClose = autoClose;
            return this;
        }

        /**
         * [请求发送]同步请求并获取byte[]返回,
         * 如果响应码不为2XX, 会抛出HttpRejectException异常
         *
         * @return 响应, 可能为null
         * @throws NoHostException       当前没有可发送的后端(网络请求发送前的异常, 准备阶段异常)
         * @throws RequestBuildException 请求初始化异常(通常是网络请求发送前的异常, 准备阶段异常)
         * @throws IOException           网络通讯异常(通常是网络请求发送中的异常)
         * @throws HttpRejectException   Http请求拒绝异常(网络请求发送后的异常, HTTP响应码不为2XX)
         */
        public byte[] sendForBytes() throws NoHostException, RequestBuildException, HttpRejectException, IOException {
            if (isSend) {
                throw new IllegalStateException("MultiHostOkHttpClient.Request can only send once!");
            }
            isSend = true;

            MultiHostOkHttpClient client = getClient();
            if (client == null) {
                throw new RequestBuildException("Missing MultiHostOkHttpClient instance, has been destroyed (cleaned by gc)");
            }
            if (isPost) {
                return client.responseBodyToBytes(client.syncPost(this));
            } else {
                return client.responseBodyToBytes(client.syncGet(this));
            }
        }

        /**
         * [请求发送]同步请求并获取InputStream返回,
         * 如果响应码不为2XX, 会抛出HttpRejectException异常
         *
         * @return 响应, 可能为null, InputStream用完后必须手动关闭!!!
         * @throws NoHostException       当前没有可发送的后端(网络请求发送前的异常, 准备阶段异常)
         * @throws RequestBuildException 请求初始化异常(通常是网络请求发送前的异常, 准备阶段异常)
         * @throws IOException           网络通讯异常(通常是网络请求发送中的异常)
         * @throws HttpRejectException   Http请求拒绝异常(网络请求发送后的异常, HTTP响应码不为2XX)
         */
        public InputStream sendForInputStream() throws NoHostException, RequestBuildException, HttpRejectException, IOException {
            if (isSend) {
                throw new IllegalStateException("MultiHostOkHttpClient.Request can only send once!");
            }
            isSend = true;

            MultiHostOkHttpClient client = getClient();
            if (client == null) {
                throw new RequestBuildException("Missing MultiHostOkHttpClient instance, has been destroyed (cleaned by gc)");
            }
            if (isPost) {
                return client.responseBodyToInputStream(client.syncPost(this));
            } else {
                return client.responseBodyToInputStream(client.syncGet(this));
            }
        }

        /**
         * [请求发送]同步请求并获取okhttp ResponseBody返回,
         * 如果响应码不为2XX, 会抛出HttpRejectException异常,
         * 该方法不会根据maxReadLength限定最大读取长度
         * @return 响应, 可能为null, ResponseBody用完后必须手动关闭!!!
         * @throws NoHostException       当前没有可发送的后端(网络请求发送前的异常, 准备阶段异常)
         * @throws RequestBuildException 请求初始化异常(通常是网络请求发送前的异常, 准备阶段异常)
         * @throws IOException           网络通讯异常(通常是网络请求发送中的异常)
         * @throws HttpRejectException   Http请求拒绝异常(网络请求发送后的异常, HTTP响应码不为2XX)
         */
        public ResponseBody send() throws NoHostException, RequestBuildException, IOException, HttpRejectException {
            if (isSend) {
                throw new IllegalStateException("MultiHostOkHttpClient.Request can only send once!");
            }
            isSend = true;

            MultiHostOkHttpClient client = getClient();
            if (client == null) {
                throw new RequestBuildException("Missing MultiHostOkHttpClient instance, has been destroyed (cleaned by gc)");
            }
            if (isPost) {
                return client.syncPost(this);
            } else {
                return client.syncGet(this);
            }
        }

        /**
         * [请求发送]异步请求,
         * 如果响应码不为2XX, 会回调onErrorAfterSend()方法给出HttpRejectException异常,
         * @param callback 回调函数{@link BytesCallback}
         */
        public void enqueue(BytesCallback callback) {
            enqueue((ResponseBodyCallback)callback);
        }

        /**
         * [请求发送]异步请求,
         * 如果响应码不为2XX, 会回调onErrorAfterSend()方法给出HttpRejectException异常,
         * @param callback 回调函数{@link InputStreamCallback}
         */
        public void enqueue(InputStreamCallback callback) {
            enqueue((ResponseBodyCallback)callback);
        }

        /**
         * [请求发送]异步请求,
         * 如果响应码不为2XX, 会回调onErrorAfterSend()方法给出HttpRejectException异常,
         * 该方法不会根据maxReadLength限定最大读取长度
         * @param callback 回调函数{@link BytesCallback}/{@link InputStreamCallback}/{@link ResponseBodyCallback}
         */
        public void enqueue(ResponseBodyCallback callback) {
            if (isSend) {
                throw new IllegalStateException("MultiHostOkHttpClient.Request can only send once!");
            }
            isSend = true;

            MultiHostOkHttpClient client = getClient();
            if (client == null) {
                callback.onErrorBeforeSend(new RequestBuildException("Missing MultiHostOkHttpClient instance, has been destroyed (cleaned by gc)"));
                return;
            }
            if (isPost) {
                client.asyncPost(this, callback);
            } else {
                client.asyncGet(this, callback);
            }
        }

        private MultiHostOkHttpClient getClient(){
            MultiHostOkHttpClient client = clientReference.get();
            if (client == null) {
                logger.error("Missing MultiHostOkHttpClient instance, has been destroyed (cleaned by gc), data:" + this);
            }
            return client;
        }

        @Override
        public String toString() {
            return "Request{" +
                    "clientReference=" + clientReference +
                    ", urlSuffix='" + urlSuffix + '\'' +
                    ", isPost=" + isPost +
                    ", headers=" + headers +
                    ", urlParams=" + urlParams +
                    ", body(hex)=" + ByteUtils.bytesToHex(body) +
                    '}';
        }

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Sync ///////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private byte[] responseBodyToBytes(ResponseBody responseBody) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        //返回空
        if (responseBody == null) {
            return null;
        }
        try {
            //限定读取长度
            if (settings.maxReadLength > 0 && responseBody.contentLength() > settings.maxReadLength){
                throw new IOException("Response contentLength is out of limit, contentLength:" + responseBody.contentLength() + ", limit:" + settings.maxReadLength);
            }
            //返回二进制数据
            return responseBody.bytes();
        } finally {
            //返回byte[]类型时自动关闭
            try {
                responseBody.close();
            } catch (Throwable ignore) {
            }
        }
    }

    private InputStream responseBodyToInputStream(ResponseBody responseBody) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        //返回空
        if (responseBody == null) {
            return null;
        }
        //限定读取长度
        if (settings.maxReadLength > 0 && responseBody.contentLength() > settings.maxReadLength){
            try {
                responseBody.close();
            } catch (Throwable ignore) {
            }
            throw new IOException("Response contentLength is out of limit, contentLength:" + responseBody.contentLength() + ", limit:" + settings.maxReadLength);
        }
        //返回二进制数据
        return responseBody.byteStream();
    }

    private ResponseBody syncPost(Request request) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        //获取远端
        LoadBalancedHostManager.Host host = fetchHost();

        printPostRequestLog(request.urlSuffix, request.body, request.urlParams, host);
        printUrlLog(request.urlSuffix, request.urlParams, host);

        //装配Request
        okhttp3.Request okRequest;
        try {
            okRequest = buildPostRequest(host.getUrl(), request, settings);
        } catch (Throwable t) {
            throw new RequestBuildException("Error while building request", t);
        }
        if (okRequest == null) {
            throw new RequestBuildException("Null request built");
        }

        if (logger.isInfoEnabled() && CheckUtils.isFlagMatch(settings.logConfig, LOG_CONFIG_REAL_URL)) {
            logger.info("POST: real-url:" + okRequest.url().toString());
        }

        //请求
        return syncCall(host, okRequest, request);
    }

    private ResponseBody syncGet(Request request) throws NoHostException, RequestBuildException, IOException, HttpRejectException {
        //获取远端
        LoadBalancedHostManager.Host host = fetchHost();

        printGetRequestLog(request.urlSuffix, request.urlParams, host);
        printUrlLog(request.urlSuffix, request.urlParams, host);

        //装配Request
        okhttp3.Request okRequest;
        try {
            okRequest = buildGetRequest(host.getUrl(), request, settings);
        } catch (Throwable t) {
            throw new RequestBuildException("Error while building request", t);
        }
        if (okRequest == null) {
            throw new RequestBuildException("Null request built");
        }

        if (logger.isInfoEnabled() && CheckUtils.isFlagMatch(settings.logConfig, LOG_CONFIG_REAL_URL)) {
            logger.info("GET: real-url:" + okRequest.url().toString());
        }

        //请求
        return syncCall(host, okRequest, request);
    }

    private ResponseBody syncCall(LoadBalancedHostManager.Host host, okhttp3.Request okRequest, Request request) throws RequestBuildException, IOException, HttpRejectException {
        try {
            //同步请求
            Response response = getOkHttpClient().newCall(okRequest).execute();
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
                long passiveBlockDuration = request.passiveBlockDuration >= 0 ? request.passiveBlockDuration : settings.passiveBlockDuration;
                host.block(passiveBlockDuration);
                if (logger.isInfoEnabled() && CheckUtils.isFlagMatch(settings.logConfig, LOG_CONFIG_BLOCK)){
                    logger.info("Block: " + host.getUrl() + " " + passiveBlockDuration);
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

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Async //////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void asyncPost(Request request, ResponseBodyCallback callback) {

        callback.setSettings(settings);

        try {
            //获取远端
            LoadBalancedHostManager.Host host = fetchHost();

            printPostRequestLog(request.urlSuffix, request.body, request.urlParams, host);
            printUrlLog(request.urlSuffix, request.urlParams, host);

            //装配Request
            okhttp3.Request okRequest;
            try {
                okRequest = buildPostRequest(host.getUrl(), request, settings);
            } catch (Throwable t) {
                throw new RequestBuildException("Error while building request", t);
            }
            if (okRequest == null) {
                throw new RequestBuildException("Null request built");
            }

            if (logger.isInfoEnabled() && CheckUtils.isFlagMatch(settings.logConfig, LOG_CONFIG_REAL_URL)) {
                logger.info("POST: real-url:" + okRequest.url().toString());
            }

            //请求
            asyncCall(host, okRequest, request, callback);
        } catch (NoHostException | RequestBuildException e) {
            callback.onErrorBeforeSend(e);
        }
    }

    private void asyncGet(Request request, ResponseBodyCallback callback) {

        callback.setSettings(settings);

        try {
            //获取远端
            LoadBalancedHostManager.Host host = fetchHost();

            printGetRequestLog(request.urlSuffix, request.urlParams, host);
            printUrlLog(request.urlSuffix, request.urlParams, host);

            //装配Request
            okhttp3.Request okRequest;
            try {
                okRequest = buildGetRequest(host.getUrl(), request, settings);
            } catch (Throwable t) {
                throw new RequestBuildException("Error while building request", t);
            }
            if (okRequest == null) {
                throw new RequestBuildException("Null request built");
            }

            if (logger.isInfoEnabled() && CheckUtils.isFlagMatch(settings.logConfig, LOG_CONFIG_REAL_URL)) {
                logger.info("GET: real-url:" + okRequest.url().toString());
            }

            //请求
            asyncCall(host, okRequest, request, callback);
        } catch (NoHostException | RequestBuildException e) {
            callback.onErrorBeforeSend(e);
        }
    }

    private void asyncCall(final LoadBalancedHostManager.Host host, okhttp3.Request okRequest, final Request request, final ResponseBodyCallback callback)  {
        //异步请求
        try {
            getOkHttpClient().newCall(okRequest).enqueue(new Callback() {
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
                    try {
                        callback.onSucceed(response.body());
                        //自动关闭
                        if (request.autoClose) {
                            try {
                                response.close();
                            } catch (Exception ignore) {
                            }
                        }
                    } catch (Exception e) {
                        //处理onSucceed
                        callback.errorOnSucceedProcessing(e);
                        //强制关闭
                        try {
                            response.close();
                        } catch (Exception ignore) {
                        }
                    }
                }
                @Override
                public void onFailure(Call call, IOException e) {
                    tryBlock(e);
                    callback.onErrorAfterSend(e);
                }
                private void tryBlock(Exception e){
                    if (needBlock(e, settings)) {
                        //网络故障阻断后端
                        long passiveBlockDuration = request.passiveBlockDuration >= 0 ? request.passiveBlockDuration : settings.passiveBlockDuration;
                        host.block(passiveBlockDuration);
                        if (logger.isInfoEnabled() && CheckUtils.isFlagMatch(settings.logConfig, LOG_CONFIG_BLOCK)) {
                            logger.info("Block: " + host.getUrl() + " " + passiveBlockDuration);
                        }
                    }
                }
            });
        } catch (Exception t) {
            callback.onErrorBeforeSend(new RequestBuildException("Error while request build ?", t));
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 私有逻辑 //////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private LoadBalancedHostManager.Host fetchHost() throws NoHostException {
        LoadBalancedHostManager.Host host = hostManager.nextHost();
        if (host == null){
            throw new NoHostException("No host");
        }
        return host;
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

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 可复写逻辑 //////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
     * @param request 请求参数
     * @param settings 客户端配置
     * @return Request
     * @throws RequestBuildException 构建异常
     */
    protected okhttp3.Request buildPostRequest(String url, Request request, Settings settings) throws RequestBuildException{
        if (request.urlSuffix == null) {
            request.urlSuffix = "";
        }
        HttpUrl httpUrl = HttpUrl.parse(url + request.urlSuffix);
        if (httpUrl == null){
            throw new RequestBuildException("Invalid url:" + url + request.urlSuffix);
        }

        if (request.urlParams != null){
            HttpUrl.Builder httpUrlBuilder = httpUrl.newBuilder();
            String encode = request.encode != null ? request.encode : settings.encode;
            for (Map.Entry<String, Object> param : request.urlParams.entrySet()) {
                try {
                    httpUrlBuilder.addEncodedQueryParameter(param.getKey(),
                            URLEncoder.encode(param.getValue() != null ? param.getValue().toString() : "", encode));
                } catch (UnsupportedEncodingException e) {
                    throw new RequestBuildException("Error while encode to url format", e);
                }
            }
            httpUrl = httpUrlBuilder.build();
        }

        okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
                .url(httpUrl)
                .post(RequestBody.create(MediaType.parse(request.mediaType != null ? request.mediaType : settings.mediaType),
                        request.body));

        Map<String, String> headers = settings.headers;
        if (headers != null){
            for (Map.Entry<String, String> entry : headers.entrySet()){
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        if (request.headers != null){
            for (Map.Entry<String, String> entry : request.headers.entrySet()){
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        return builder.build();
    }

    /**
     * 根据URL和报文体组GET请求(复写本方法实现自定义的逻辑)
     * @param url 由LoadBalancedHostManager选择出的远端url(前缀)
     * @param request 请求参数
     * @param settings 客户端配置
     * @return Request
     * @throws RequestBuildException 构建异常
     */
    protected okhttp3.Request buildGetRequest(String url, Request request, Settings settings) throws RequestBuildException{
        if (request.urlSuffix == null) {
            request.urlSuffix = "";
        }
        HttpUrl httpUrl = HttpUrl.parse(url + request.urlSuffix);
        if (httpUrl == null){
            throw new RequestBuildException("Invalid url:" + url + request.urlSuffix);
        }

        if (request.urlParams != null){
            HttpUrl.Builder httpUrlBuilder = httpUrl.newBuilder();
            String encode = request.encode != null ? request.encode : settings.encode;
            for (Map.Entry<String, Object> param : request.urlParams.entrySet()) {
                try {
                    httpUrlBuilder.addEncodedQueryParameter(param.getKey(),
                            URLEncoder.encode(param.getValue() != null ? param.getValue().toString() : "", encode));
                } catch (UnsupportedEncodingException e) {
                    throw new RequestBuildException("Error while encode to url format", e);
                }
            }
            httpUrl = httpUrlBuilder.build();
        }

        okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
                .url(httpUrl)
                .get();

        Map<String, String> headers = settings.headers;
        if (headers != null){
            for (Map.Entry<String, String> entry : headers.entrySet()){
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        if (request.headers != null){
            for (Map.Entry<String, String> entry : request.headers.entrySet()){
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        return builder.build();
    }

    /**
     * 判断该异常是否需要阻断后端, 返回true阻断
     */
    protected boolean needBlock(Throwable t, Settings settings) {
        return t instanceof ConnectException ||
                t instanceof SocketTimeoutException ||
                t instanceof UnknownHostException ||
                (t instanceof HttpRejectException && settings.httpCodeNeedBlock.contains(((HttpRejectException) t).getResponseCode()));
    }

    /**
     * 判断HTTP请求是否成功, 返回true成功
     */
    protected boolean isSucceed(Response response) {
        return response.isSuccessful();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 配置 //////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 客户端配置
     */
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

        private Set<Integer> httpCodeNeedBlock = new HashSet<>(8);

        private Settings(){
        }

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 异步方式的Cllback /////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 请求回调(通用)
     */
    public static abstract class ResponseBodyCallback {

        /**
         * <p>请求成功</p>
         *
         * <p>注意: ResponseBody实例是需要关闭的(close), 但我们提供autoClose配置, 详见{@link MultiHostOkHttpClient.Request#autoClose(boolean)}</p>
         *
         * <p>
         * 当autoClose=true时, onSucceed方法回调结束后, ResponseBody/InputStream会被自动关闭, 无需手动调用close方法. 适用于
         * 响应数据在回调方法中处理完的场合.<br>
         * 当autoClose=false时, onSucceed方法回调结束后, ResponseBody/InputStream不会自动关闭, 需要手动调用ResponseBody.close()关闭,
         * 注意!!! 适用于响应数据需要交由其他的线程处理, 或暂时持有的场合使用.
         * </p>
         *
         * @param responseBody 响应
         */
        protected abstract void onSucceed(ResponseBody responseBody) throws Exception;

        /**
         * 请求前发生异常
         * @param e {@link RequestBuildException}:请求前发生异常, {@link NoHostException}:未配置后端地址或所有后端地址均不可用
         */
        protected abstract void onErrorBeforeSend(Exception e);

        /**
         * 请求后发生异常
         * @param e {@link HttpRejectException}:后端Http拒绝(返回码不为200), {@link IOException}:通讯异常
         */
        protected abstract void onErrorAfterSend(Exception e);

        /**
         * 回调方法onSucceed执行时如果抛出异常, 会回调该方法处理异常, 默认转交onErrorAfterSend方法处理
         */
        protected void errorOnSucceedProcessing(Exception e){
            onErrorAfterSend(e);
        }

        void setSettings(Settings settings) {
            //do nothing
        }

    }

    /**
     * 请求回调(获得byte[]响应体)
     */
    public static abstract class BytesCallback extends ResponseBodyCallback {

        private Settings settings;

        /**
         * <p>请求成功</p>
         *
         * @param body 响应
         */
        public abstract void onSucceed(byte[] body) throws Exception;

        @Override
        public final void onSucceed(ResponseBody responseBody) throws Exception {
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
                //byte[]类型返回时, 强制关闭(无论autoClose是什么配置)
                if (responseBody != null){
                    try {
                        responseBody.close();
                    } catch (Throwable ignore) {
                    }
                }
            }
            onSucceed(bytes);
        }

        @Override
        final void setSettings(Settings settings) {
            this.settings = settings;
        }

    }

    /**
     * 请求回调(获得InputStream响应体)
     */
    public static abstract class InputStreamCallback extends ResponseBodyCallback {

        private Settings settings;

        /**
         * <p>请求成功</p>
         *
         * <p>注意: InputStream实例是需要关闭的(close), 但我们提供autoClose配置, 详见{@link MultiHostOkHttpClient.Request#autoClose(boolean)}</p>
         *
         * <p>
         * 当autoClose=true时, onSucceed方法回调结束后, ResponseBody/InputStream会被自动关闭, 无需手动调用close方法. 适用于
         * 响应数据在回调方法中处理完的场合.<br>
         * 当autoClose=false时, onSucceed方法回调结束后, ResponseBody/InputStream不会自动关闭, 需要手动调用ResponseBody.close()关闭,
         * 注意!!! 适用于响应数据需要交由其他的线程处理, 或暂时持有的场合使用.
         * </p>
         *
         * @param inputStream 响应
         */
        public abstract void onSucceed(InputStream inputStream) throws Exception;

        @Override
        public final void onSucceed(ResponseBody responseBody) throws Exception {
            //返回空
            if (responseBody == null) {
                onSucceed((InputStream) null);
                return;
            }
            //限定读取长度
            if (settings.maxReadLength > 0 && responseBody.contentLength() > settings.maxReadLength) {
                //长度超过限制时, 强制关闭(无论autoClose是什么配置)
                try {
                    responseBody.close();
                } catch (Throwable ignore) {
                }
                onErrorAfterSend(new IOException("Response contentLength is out of limit, contentLength:" + responseBody.contentLength() + ", limit:" + settings.maxReadLength));
                return;
            }
            //返回二进制数据
            onSucceed(responseBody.byteStream());
        }

        @Override
        final void setSettings(Settings settings) {
            this.settings = settings;
        }

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 客户端设置 ///////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 设置远端管理器(必须)
     * @param hostManager 远端管理器
     */
    public MultiHostOkHttpClient setHostManager(LoadBalancedHostManager hostManager) {
        this.hostManager = hostManager;
        return this;
    }

    /**
     * <p>[配置]设置被动检测到网络故障时阻断后端的时间</p>
     *
     * <p>当请求服务端时, 发生特定的异常或返回特定的响应码(MultiHostOkHttpClient.needBlock方法决定), 客户端会将该
     * 后端服务器的IP/PORT标记为暂不可用状态, 而阻断时长是不可用的时长</p>
     *
     * @param passiveBlockDuration 阻断时长ms
     */
    public MultiHostOkHttpClient setPassiveBlockDuration(long passiveBlockDuration) {
        settings.passiveBlockDuration = passiveBlockDuration;
        return this;
    }

    /**
     * 设置MediaType
     * @param mediaType 设置MediaType
     */
    public MultiHostOkHttpClient setMediaType(String mediaType) {
        settings.mediaType = mediaType;
        return this;
    }

    /**
     * 设置编码
     * @param encode 编码
     */
    public MultiHostOkHttpClient setEncode(String encode) {
        settings.encode = encode;
        return this;
    }

    /**
     * 设置HTTP请求头参数
     * @param headers 请求头参数
     */
    public MultiHostOkHttpClient setHeaders(Map<String, String> headers) {
        settings.headers = headers;
        return this;
    }

    /**
     * 打印更多的日志, 默认关闭
     * @param verboseLog true:打印更多的调试日志, 默认关闭
     */
    public MultiHostOkHttpClient setVerboseLog(boolean verboseLog) {
        settings.verboseLog = verboseLog;
        return this;
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
    public MultiHostOkHttpClient setVerboseLogConfig(int verboseLogConfig) {
        settings.verboseLogConfig = verboseLogConfig;
        return this;
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
    public MultiHostOkHttpClient setLogConfig(int logConfig) {
        settings.logConfig = logConfig;
        return this;
    }

    /**
     * 最大请求线程数(仅异步请求时有效)
     * @param maxThreads 最大请求线程数
     */
    public MultiHostOkHttpClient setMaxThreads(int maxThreads) {
        settings.maxThreads = maxThreads;
        return this;
    }

    /**
     * 对应每个后端的最大请求线程数(仅异步请求时有效)
     * @param maxThreadsPerHost 对应每个后端的最大请求线程数
     */
    public MultiHostOkHttpClient setMaxThreadsPerHost(int maxThreadsPerHost) {
        settings.maxThreadsPerHost = maxThreadsPerHost;
        return this;
    }

    /**
     * 设置连接超时ms
     * @param connectTimeout 连接超时ms
     */
    public MultiHostOkHttpClient setConnectTimeout(long connectTimeout) {
        try {
            settingsLock.lock();
            settings.connectTimeout = connectTimeout;
            refreshSettings = true;
        } finally {
            settingsLock.unlock();
        }
        return this;
    }

    /**
     * 设置写数据超时ms
     * @param writeTimeout 写数据超时ms
     */
    public MultiHostOkHttpClient setWriteTimeout(long writeTimeout) {
        try {
            settingsLock.lock();
            settings.writeTimeout = writeTimeout;
            refreshSettings = true;
        } finally {
            settingsLock.unlock();
        }
        return this;
    }

    /**
     * 设置读数据超时ms
     * @param readTimeout 读数据超时ms
     */
    public MultiHostOkHttpClient setReadTimeout(long readTimeout) {
        try {
            settingsLock.lock();
            settings.readTimeout = readTimeout;
            refreshSettings = true;
        } finally {
            settingsLock.unlock();
        }
        return this;
    }

    /**
     * 设置最大读取数据长度(默认:10M)
     * @param maxReadLength 设置最大读取数据长度, 单位bytes
     */
    public MultiHostOkHttpClient setMaxReadLength(long maxReadLength){
        settings.maxReadLength = maxReadLength;
        return this;
    }

    /**
     * CookieJar
     * @param cookieJar CookieJar
     */
    public MultiHostOkHttpClient setCookieJar(CookieJar cookieJar) {
        try {
            settingsLock.lock();
            settings.cookieJar = cookieJar;
            refreshSettings = true;
        } finally {
            settingsLock.unlock();
        }
        return this;
    }

    /**
     * Proxy
     * @param proxy 例如127.0.0.1:8080
     * @throws IllegalArgumentException if the proxy string is invalid
     * @throws NumberFormatException  if the string does not contain a parsable integer.
     * @throws SecurityException if a security manager is present and permission to resolve the host name is denied.
     */
    public MultiHostOkHttpClient setProxy(String proxy) {
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
        return this;
    }

    /**
     * Dns
     * @param dns Dns
     */
    public MultiHostOkHttpClient setDns(Dns dns) {
        try {
            settingsLock.lock();
            settings.dns = dns;
            refreshSettings = true;
        } finally {
            settingsLock.unlock();
        }
        return this;
    }

    /**
     * 当HTTP返回码为指定返回码时, 阻断后端
     * @param codes 指定需要阻断的返回码, 例如:403,404
     */
    public MultiHostOkHttpClient setHttpCodeNeedBlock(String codes) {
        try {
            String[] codeArray = codes.split(",");
            settings.httpCodeNeedBlock.clear();
            for (String code : codeArray) {
                settings.httpCodeNeedBlock.add(Integer.parseInt(code));
            }
        } catch (Throwable t) {
            throw new RuntimeException("Invalid httpCodeNeedBlock " + codes, t);
        }
        return this;
    }

}
