# 支持软负载的Http请求客户端使用手册

* 包路径:sviolet.slate.common.modelx.loadbalance
* 支持配置多个后端地址, 平均分配流量
* 支持被动/主动方式探测后端是否可用, 自动选择可用的后端发送请求
* 支持同步/异步方式请求

# 依赖

* gradle

```text

//依赖
dependencies {
    compile 'com.github.shepherdviolet:slate-common:9.8'
}
```

* gradle(最少依赖)

```text
dependencies {
    compile ('com.github.shepherdviolet:slate-common:9.8') {
        transitive = false
    }
    compile ('com.github.shepherdviolet:thistle:9.7') {
        transitive = false
    }
    compile 'com.squareup.okhttp3:okhttp:3.9.0'
    compile 'ch.qos.logback:logback-classic:1.2.3'
}
```

* maven

```maven
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-common</artifactId>
        <version>9.8</version>
    </dependency>
```

* maven(最少依赖)

```maven
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-common</artifactId>
        <version>9.8</version>
        <exclusions>
             <exclusion>
                 <groupId>*</groupId>
                 <artifactId>*</artifactId>
             </exclusion>
        </exclusions>
    </dependency>
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>thistle</artifactId>
        <version>9.7</version>
        <exclusions>
             <exclusion>
                 <groupId>*</groupId>
                 <artifactId>*</artifactId>
             </exclusion>
        </exclusions>
    </dependency>
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>3.9.0</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.2.3</version>
    </dependency>
```

# 配置

### Spring(XML)
* `LoadBalancedInspectManager需要配置destroy-method="close"`

```text

    <!-- 后端管理器 -->
    <!-- 配置管理后端地址和状态 -->
    <bean id="loadBalancedHostManager" class="sviolet.slate.common.modelx.loadbalance.LoadBalancedHostManager">
        <property name="hosts" value="http://127.0.0.1:8081,http://127.0.0.1:8082"/>
    </bean>
    
    <!-- 主动探测管理器 -->
    <!-- 定时探测后端状态(默认Telnet方式) -->
    <bean id="loadBalancedInspector" class="sviolet.slate.common.modelx.loadbalance.LoadBalancedInspectManager"
        destroy-method="close">
        <property name="hostManager" ref="loadBalancedHostManager"/>
        <property name="inspectInterval" value="5000"/>
    </bean>
    
    <!-- HTTP请求客户端 -->
    <!-- 调用该实例发送请求 -->
    <bean id="multiHostOkHttpClient" class="sviolet.slate.common.modelx.loadbalance.classic.MultiHostOkHttpClient">
        <property name="hostManager" ref="loadBalancedHostManager"/>
        <property name="maxThreads" value="200"/>
        <property name="maxThreadsPerHost" value="200"/>
        <property name="passiveBlockDuration" value="3000"/>
        <property name="connectTimeout" value="3000"/>
        <property name="writeTimeout" value="10000"/>
        <property name="readTimeout" value="10000"/>
        <property name="verboseLog" value="true"/>
        <property name="verboseLogConfig" value="272"/>
        <!--<property name="dataConverter" ref="dataConverter"/> 默认提供GsonDataConverter-->
    </bean>
    
```

### Spring(注解)
* `LoadBalancedInspectManager需要配置@Bean(destroyMethod = "close")`

```text

    /**
     * 后端管理器
     * 配置管理后端地址和状态
     */
    @Bean
    public LoadBalancedHostManager loadBalancedHostManager() {
        return new LoadBalancedHostManager()
                .setHostArray(new String[]{
                    "http://127.0.0.1:8081",
                    "http://127.0.0.1:8082"
                });
    }
    
    /**
     * 主动探测管理器
     * 定时探测后端状态(默认Telnet方式)
     * 注意, 必须配置destroyMethod = "close"
     */
    @Bean(destroyMethod = "close")
    public LoadBalancedInspectManager loadBalancedInspectManager(LoadBalancedHostManager loadBalancedHostManager) {
        return new LoadBalancedInspectManager()
                //.setInspector(new TelnetLoadBalanceInspector())  
                .setHostManager(loadBalancedHostManager)
                .setInspectInterval(5000L);        
    }
    
    /**
     * HTTP请求客户端
     * 调用该实例发送请求
     */ 
    @Bean
    public MultiHostOkHttpClient multiHostOkHttpClient(LoadBalancedHostManager loadBalancedHostManager) {
        return new MultiHostOkHttpClient()
                .setHostManager(loadBalancedHostManager)
                .setMaxThreads(200)
                .setMaxThreadsPerHost(200)
                .setPassiveBlockDuration(3000L)
                .setConnectTimeout(3000L)
                .setWriteTimeout(10000L)
                .setReadTimeout(10000L)
                //.setDataConverter(new GsonDataConverter())
                .setVerboseLog(true)
                .setVerboseLogConfig(MultiHostOkHttpClient.VERBOSE_LOG_CONFIG_RAW_URL|MultiHostOkHttpClient.VERBOSE_LOG_CONFIG_REQUEST_STRING_BODY);
    }
    
```

### 简化版客户端配置(新增)
* 在MultiHostOkHttpClient的基础上, 封装了LoadBalancedHostManager和LoadBalancedInspectManager, 简化了配置, 免去了配置三个Bean的麻烦
* 配置被简化, 如需高度定制, 请使用LoadBalancedHostManager + LoadBalancedInspectManager + MultiHostOkHttpClient
* 内置的LoadBalancedInspectManager采用TELNET方式探测后端(不可自定义探测方式)<br>
* 屏蔽了setHostManager()方法, 调用会抛出异常<br>
* 实现了DisposableBean, 在Spring容器中会自动销毁<br>

```text
    <bean id="simpleOkHttpClient" class="sviolet.slate.common.modelx.loadbalance.classic.SimpleOkHttpClient">
        <property name="hosts" value="http://127.0.0.1:8081,http://127.0.0.1:8082"/>
        <property name="initiativeInspectInterval" value="10000"/>
        <property name="maxThreads" value="200"/>
        <property name="maxThreadsPerHost" value="200"/>
        <property name="passiveBlockDuration" value="3000"/>
        <property name="connectTimeout" value="3000"/>
        <property name="writeTimeout" value="10000"/>
        <property name="readTimeout" value="10000"/>
        <!--<property name="dataConverter" ref="dataConverter"/> 默认提供GsonDataConverter-->
    </bean>
```

```text
    SimpleOkHttpClient client = new SimpleOkHttpClient()
            .setHosts("http://127.0.0.1:8081,http://127.0.0.1:8082")
            .setInitiativeInspectInterval(5000L)
            .setMaxThreads(200)
            .setMaxThreadsPerHost(200)
            .setPassiveBlockDuration(3000L)
            .setConnectTimeout(3000L)
            .setWriteTimeout(10000L)
            .setReadTimeout(10000L)
            //.setDataConverter(new GsonDataConverter())
            .setVerboseLog(true)
            .setVerboseLogConfig(MultiHostOkHttpClient.VERBOSE_LOG_CONFIG_RAW_URL|MultiHostOkHttpClient.VERBOSE_LOG_CONFIG_REQUEST_STRING_BODY);

```

# 调用

* 关于URL地址

> 以本文档中的代码为例 <br>
> http://127.0.0.1:8081 和 http://127.0.0.1:8082 是两台应用服务器, 我们要向他们请求数据, 请求的URL后缀为 /user/update.json <br>
> 配置LoadBalancedHostManager的hosts参数为 http://127.0.0.1:8081,http://127.0.0.1:8082 <br>
> 调用客户端发送请求 multiHostOkHttpClient.get("/user/update.json").send() <br>
> 程序会自动选择一个应用服务器, 然后将应用服务器的地址与URL后缀拼接 <br>
> 最终的请求地址为 http://127.0.0.1:8081/user/update.json 或 http://127.0.0.1:8082/user/update.json <br>

* 注入客户端
* 一般情况下, 需要根据实际情况, 对MultiHostOkHttpClient做再封装, 实现报文转换, 异常统一处理等

```text
    @Autowired
    private MultiHostOkHttpClient client;
```

### POST

* 同步POST:返回byte[]类型的响应
 
 ```text
  try {
      byte[] response = client.post("/post/json")
              .urlParam("traceId", "000000001")
              .body("hello world".getBytes())
              //.formBody(formBody)//表单提交
              //.beanBody(bean)//发送JavaBean, 需要配置dataConverter
              //.httpHeader("Accept", "application/json;charset=utf-8")
              //.mediaType("application/json;charset=utf-8")
              //.encode("utf-8")
              .sendForBytes();
  } catch (NoHostException e) {
      //当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
  } catch (RequestBuildException e) {
      //在网络请求未发送前抛出的异常
  } catch (IOException e) {
      //网络异常
  } catch (HttpRejectException e) {
      //HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
      //获得拒绝码 e.getResponseCode()
      //获得拒绝信息 e.getResponseMessage()
  }
 ```

* 同步POST:返回InputStream类型的响应
* 注意:InputStream需要手动关闭(close)

 ```text
 try (InputStream inputStream = client.post("/post/json")
         .body("hello world".getBytes())
         //.formBody(formBody)//表单提交
         //.beanBody(bean)//发送JavaBean, 需要配置dataConverter
         //.httpHeader("Accept", "application/json;charset=utf-8")
         //.mediaType("application/json;charset=utf-8")
         //.encode("utf-8")
         .sendForInputStream()) {

     inputStream......

 } catch (NoHostException e) {
     //当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
 } catch (RequestBuildException e) {
     //在网络请求未发送前抛出的异常
 } catch (IOException e) {
     //网络异常
 } catch (HttpRejectException e) {
     //HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
     //获得拒绝码 e.getResponseCode()
     //获得拒绝信息 e.getResponseMessage()
 }
```

* 同步POST:返回ResponsePackage类型的响应
* 注意:ResponsePackage需要手动关闭(close)

 ```text
 try (MultiHostOkHttpClient.ResponsePackage responsePackage = client.post("/post/json")
         .body("hello world".getBytes())
         //.formBody(formBody)//表单提交
         //.beanBody(bean)//发送JavaBean, 需要配置dataConverter
         //.httpHeader("Accept", "application/json;charset=utf-8")
         //.mediaType("application/json;charset=utf-8")
         //.encode("utf-8")
         .send()) {

     String response = responsePackage.body().string();

 } catch (NoHostException e) {
     //当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
 } catch (RequestBuildException e) {
     //在网络请求未发送前抛出的异常
 } catch (IOException e) {
     //网络异常
 } catch (HttpRejectException e) {
     //HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
     //获得拒绝码 e.getResponseCode()
     //获得拒绝信息 e.getResponseMessage()
 }
```

* 同步POST:请求报文体Map, 返回报文体Map
* 注意:必须要配置dataConverter

```text
 try {
        Map<String, Object> requestMap = new HashMap<>(2);
        requestMap.put("name", "wang wang");
        requestMap.put("key", "963");
        Map<String, Object> responseMap = client.post("/post/json")
                .beanBody(requestMap)
                .sendForBean(Map.class);
 } catch (NoHostException e) {
     //当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
 } catch (RequestBuildException e) {
     //在网络请求未发送前抛出的异常
 } catch (IOException e) {
     //网络异常
 } catch (HttpRejectException e) {
     //HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
     //获得拒绝码 e.getResponseCode()
     //获得拒绝信息 e.getResponseMessage()
 }
```

* 异步POST:返回byte[]类型的响应

 ```text
 //返回byte[]类型的响应
 client.post("/post/json")
         .urlParam("traceId", "000000001")
         .body("hello world".getBytes())
         //.formBody(formBody)//表单提交
         //.beanBody(bean)//发送JavaBean, 需要配置dataConverter
         //.httpHeader("Accept", "application/json;charset=utf-8")
         //.mediaType("application/json;charset=utf-8")
         //.encode("utf-8")
         .enqueue(new MultiHostOkHttpClient.BytesCallback() {
             public void onSucceed(byte[] body) {
                 ......
             }
             protected void onErrorBeforeSend(Exception e) {
                 //NoHostException: 当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
                 //RequestBuildException: 在网络请求未发送前抛出的异常
             }
             protected void onErrorAfterSend(Exception e) {
                 //IOException: 网络异常
                 //HttpRejectException: HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
                 //获得拒绝码 e.getResponseCode()
                 //获得拒绝信息 e.getResponseMessage()
                 //另外, 如果onSucceed方法中抛出异常, 默认会将异常转交到这个方法处理
             }
         });
```

* 异步POST:返回InputStream类型的响应
* 当autoClose=true时, onSucceed方法回调结束后, 输入流会被自动关闭, 无需手动调用close方法
* 当autoClose=false时, onSucceed方法回调结束后, 输入流不会自动关闭, 需要手动调用InputStream.close()关闭, 注意!!!

 ```text
 client.post("/post/json")
         .urlParam("traceId", "000000001")
         .body("hello world".getBytes())
         //.formBody(formBody)//表单提交
         //.beanBody(bean)//发送JavaBean, 需要配置dataConverter
         //.autoClose(false)//默认为true
         //.httpHeader("Accept", "application/json;charset=utf-8")
         //.mediaType("application/json;charset=utf-8")
         //.encode("utf-8")
         .enqueue(new MultiHostOkHttpClient.InputStreamCallback() {
             public void onSucceed(InputStream inputStream) throws Exception {
                 ......
             }
             protected void onErrorBeforeSend(Exception e) {
                 //NoHostException: 当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
                 //RequestBuildException: 在网络请求未发送前抛出的异常
             }
             protected void onErrorAfterSend(Exception e) {
                 //IOException: 网络异常
                 //HttpRejectException: HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
                 //获得拒绝码 e.getResponseCode()
                 //获得拒绝信息 e.getResponseMessage()
                 //另外, 如果onSucceed方法中抛出异常, 默认会将异常转交到这个方法处理
             }
         });
 ```

* 异步POST:返回ResponsePackage类型的响应
* 当autoClose=true时, onSucceed方法回调结束后, ResponsePackage会被自动关闭, 无需手动调用close方法
* 当autoClose=false时, onSucceed方法回调结束后, ResponsePackage不会自动关闭, 需要手动调用ResponsePackage.close()关闭, 注意!!!

 ```text
 client.post("/post/json")
         .urlParam("traceId", "000000001")
         .body("hello world".getBytes())
         //.formBody(formBody)//表单提交
         //.beanBody(bean)//发送JavaBean, 需要配置dataConverter
         //.autoClose(false)//默认为true
         //.httpHeader("Accept", "application/json;charset=utf-8")
         //.mediaType("application/json;charset=utf-8")
         //.encode("utf-8")
         .enqueue(new MultiHostOkHttpClient.ResponsePackageCallback() {
             public void onSucceed(MultiHostOkHttpClient.ResponsePackage responsePackage) throws Exception {
                 ......
             }
             protected void onErrorBeforeSend(Exception e) {
                 //NoHostException: 当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
                 //RequestBuildException: 在网络请求未发送前抛出的异常
             }
             protected void onErrorAfterSend(Exception e) {
                 //IOException: 网络异常
                 //HttpRejectException: HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
                 //获得拒绝码 e.getResponseCode()
                 //获得拒绝信息 e.getResponseMessage()
                 //另外, 如果onSucceed方法中抛出异常, 默认会将异常转交到这个方法处理
             }
         });
```

* 异步POST:请求报文体Map, 返回报文体Map
* 注意:必须要配置dataConverter

```text
Map<String, Object> requestMap = new HashMap<>(2);
requestMap.put("name", "wang wang");
requestMap.put("key", "963");
client.post("/post/json")
        .beanBody(requestMap)
        .enqueue(new MultiHostOkHttpClient.BeanCallback<Map<String, Object>>() {
            @Override
            public void onSucceed(Map<String, Object> bean) throws Exception {
                ......
            }
            protected void onErrorBeforeSend(Exception e) {
                //NoHostException: 当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
                //RequestBuildException: 在网络请求未发送前抛出的异常
            }
            protected void onErrorAfterSend(Exception e) {
                //IOException: 网络异常
                //HttpRejectException: HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
                //获得拒绝码 e.getResponseCode()
                //获得拒绝信息 e.getResponseMessage()
                //另外, 如果onSucceed方法中抛出异常, 默认会将异常转交到这个方法处理
            }
        });
```

### GET

* 同步GET:返回byte[]类型的响应
 
 ```text
  try {
      byte[] response = client.get("/get/json")
              .urlParam("name", "000000001")
              .urlParam("key", "000000001")
              //.httpHeader("Accept", "application/json;charset=utf-8")
              //.mediaType("application/json;charset=utf-8")
              //.encode("utf-8")
              .sendForBytes();
  } catch (NoHostException e) {
      //当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
  } catch (RequestBuildException e) {
      //在网络请求未发送前抛出的异常
  } catch (IOException e) {
      //网络异常
  } catch (HttpRejectException e) {
      //HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
      //获得拒绝码 e.getResponseCode()
      //获得拒绝信息 e.getResponseMessage()
  }
 ```

* 同步GET:返回InputStream类型的响应
* 注意:InputStream需要手动关闭(close)

 ```text
 try (InputStream inputStream = client.get("/get/json")
         .urlParam("name", "000000001")
         .urlParam("key", "000000001")
         //.httpHeader("Accept", "application/json;charset=utf-8")
         //.mediaType("application/json;charset=utf-8")
         //.encode("utf-8")
         .sendForInputStream()) {

     inputStream......

 } catch (NoHostException e) {
     //当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
 } catch (RequestBuildException e) {
     //在网络请求未发送前抛出的异常
 } catch (IOException e) {
     //网络异常
 } catch (HttpRejectException e) {
     //HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
     //获得拒绝码 e.getResponseCode()
     //获得拒绝信息 e.getResponseMessage()
 }
```

* 同步GET:返回ResponsePackage类型的响应
* 注意:ResponsePackage需要手动关闭(close)

 ```text
 try (MultiHostOkHttpClient.ResponsePackage responsePackage = client.get("/get/json")
         .urlParam("name", "000000001")
         .urlParam("key", "000000001")
         //.httpHeader("Accept", "application/json;charset=utf-8")
         //.mediaType("application/json;charset=utf-8")
         //.encode("utf-8")
         .send()) {

     String response = responsePackage.body().string();

 } catch (NoHostException e) {
     //当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
 } catch (RequestBuildException e) {
     //在网络请求未发送前抛出的异常
 } catch (IOException e) {
     //网络异常
 } catch (HttpRejectException e) {
     //HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
     //获得拒绝码 e.getResponseCode()
     //获得拒绝信息 e.getResponseMessage()
 }
```

* 同步GET:返回报文体Map
* 注意:必须要配置dataConverter

```text
 try {
        Map<String, Object> responseMap = client.get("/get/json")
                .urlParam("name", "000000001")
                .urlParam("key", "000000001")
                .sendForBean(Map.class);
 } catch (NoHostException e) {
     //当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
 } catch (RequestBuildException e) {
     //在网络请求未发送前抛出的异常
 } catch (IOException e) {
     //网络异常
 } catch (HttpRejectException e) {
     //HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
     //获得拒绝码 e.getResponseCode()
     //获得拒绝信息 e.getResponseMessage()
 }
```

* 异步GET:返回byte[]类型的响应

 ```text
 //返回byte[]类型的响应
 client.get("/get/json")
         .urlParam("name", "000000001")
         .urlParam("key", "000000001")
         //.httpHeader("Accept", "application/json;charset=utf-8")
         //.mediaType("application/json;charset=utf-8")
         //.encode("utf-8")
         .enqueue(new MultiHostOkHttpClient.BytesCallback() {
             public void onSucceed(byte[] body) {
                 ......
             }
             protected void onErrorBeforeSend(Exception e) {
                 //NoHostException: 当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
                 //RequestBuildException: 在网络请求未发送前抛出的异常
             }
             protected void onErrorAfterSend(Exception e) {
                 //IOException: 网络异常
                 //HttpRejectException: HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
                 //获得拒绝码 e.getResponseCode()
                 //获得拒绝信息 e.getResponseMessage()
                 //另外, 如果onSucceed方法中抛出异常, 默认会将异常转交到这个方法处理
             }
         });
```

* 异步GET:返回InputStream类型的响应
* 当autoClose=true时, onSucceed方法回调结束后, 输入流会被自动关闭, 无需手动调用close方法
* 当autoClose=false时, onSucceed方法回调结束后, 输入流不会自动关闭, 需要手动调用InputStream.close()关闭, 注意!!!

 ```text
 client.get("/get/json")
         .urlParam("name", "000000001")
         .urlParam("key", "000000001")
         //.autoClose(false)//默认为true
         //.httpHeader("Accept", "application/json;charset=utf-8")
         //.mediaType("application/json;charset=utf-8")
         //.encode("utf-8")
         .enqueue(new MultiHostOkHttpClient.InputStreamCallback() {
             public void onSucceed(InputStream inputStream) throws Exception {
                 ......
             }
             protected void onErrorBeforeSend(Exception e) {
                 //NoHostException: 当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
                 //RequestBuildException: 在网络请求未发送前抛出的异常
             }
             protected void onErrorAfterSend(Exception e) {
                 //IOException: 网络异常
                 //HttpRejectException: HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
                 //获得拒绝码 e.getResponseCode()
                 //获得拒绝信息 e.getResponseMessage()
                 //另外, 如果onSucceed方法中抛出异常, 默认会将异常转交到这个方法处理
             }
         });
 ```

* 异步GET:返回ResponsePackage类型的响应
* 当autoClose=true时, onSucceed方法回调结束后, ResponsePackage会被自动关闭, 无需手动调用close方法
* 当autoClose=false时, onSucceed方法回调结束后, ResponsePackage不会自动关闭, 需要手动调用ResponsePackage.close()关闭, 注意!!!

 ```text
 client.get("/get/json")
         .urlParam("name", "000000001")
         .urlParam("key", "000000001")
         //.autoClose(false)//默认为true
         //.httpHeader("Accept", "application/json;charset=utf-8")
         //.mediaType("application/json;charset=utf-8")
         //.encode("utf-8")
         .enqueue(new MultiHostOkHttpClient.ResponsePackageCallback() {
             public void onSucceed(MultiHostOkHttpClient.ResponsePackage responsePackage) throws Exception {
                 ......
             }
             protected void onErrorBeforeSend(Exception e) {
                 //NoHostException: 当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
                 //RequestBuildException: 在网络请求未发送前抛出的异常
             }
             protected void onErrorAfterSend(Exception e) {
                 //IOException: 网络异常
                 //HttpRejectException: HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
                 //获得拒绝码 e.getResponseCode()
                 //获得拒绝信息 e.getResponseMessage()
                 //另外, 如果onSucceed方法中抛出异常, 默认会将异常转交到这个方法处理
             }
         });
```

* 异步GET:返回报文体Map
* 注意:必须要配置dataConverter

```text
client.get("/get/json")
        .urlParam("name", "000000001")
        .urlParam("key", "000000001")
        .enqueue(new MultiHostOkHttpClient.BeanCallback<Map<String, Object>>() {
            @Override
            public void onSucceed(Map<String, Object> bean) throws Exception {
                ......
            }
            protected void onErrorBeforeSend(Exception e) {
                //NoHostException: 当hosts没有配置任何后端地址, 或配置returnNullIfAllBlocked=true时所有后端都处于异常状态, 则抛出该异常
                //RequestBuildException: 在网络请求未发送前抛出的异常
            }
            protected void onErrorAfterSend(Exception e) {
                //IOException: 网络异常
                //HttpRejectException: HTTP拒绝, 即HTTP返回码不为200(2??)时, 抛出该异常
                //获得拒绝码 e.getResponseCode()
                //获得拒绝信息 e.getResponseMessage()
                //另外, 如果onSucceed方法中抛出异常, 默认会将异常转交到这个方法处理
            }
        });
```

# 配置参数详解

### LoadBalancedHostManager配置

##### hosts

* 配置远端地址, 可配置多个, 逗号分隔
* 该参数可以在运行时`实时变更`. 例如使用apollo配置中心时, 可以在监听方法中使用LoadBalancedHostManager#setHosts()方法改变后端地址
* 例如:http://127.0.0.1:8081,http://127.0.0.1:8082

##### returnNullIfAllBlocked (可选项)

* 设置false时, 当所有后端都处于异常状态时, 随机选择一个后端发送请求
* 设置true时, 当所有后端都处于异常状态时, 抛出NoHostException
* 默认:false

### LoadBalancedInspectManager配置

##### hostManager

* 后端管理器(LoadBalancedHostManager, 必须配置)

##### inspectInterval (可选项)

* 探测间隔, 单位ms
* 默认:5000ms

##### inspector (可选项)

* 配置主动探测器, TelnetLoadBalanceInspector/HttpGetLoadBalanceInspector
* 可配置多个探测器, 依次探测
* 可自行实现探测逻辑
* 默认TelnetLoadBalanceInspector, 可不配置该选项

##### verboseLog (可选项)

* 开启更多的日志输出
* 默认:false

### MultiHostOkHttpClient配置

##### hostManager

* 后端管理器(LoadBalancedHostManager, 必须配置)

##### maxThreads

* 异步请求时该参数有效(asyncPost/asyncGet)
* 最大请求线程数
* 建议配置成较大值, 如:200
* 默认:64

##### maxThreadsPerHost

* 异步请求时该参数有效(asyncPost/asyncGet)
* 同一个后端地址的最大请求线程数
* 建议配置成较大值, 如:200
* 默认:64

##### connectTimeout

* 网络连接超时, 单位ms
* 默认:3000ms

##### writeTimeout

* 网络写超时, 单位ms
* 默认:10000ms

##### readTimeout

* 网络读超时, 单位ms
* 默认:10000ms

##### maxReadLength

* 允许读取的最大响应报文长度, 字节
* 默认:10L * 1024L * 1024L

##### passiveBlockDuration (可选项)

* 被动探测阻断时长, 单位ms
* 当请求发生网络异常(连接失败/超时等), 程序会暂停向该后端发送请求, 暂停时间由该参数决定
* 默认:3000ms

##### mediaType (可选)

* 请求报文的MediaType
* 默认:application/json;charset=utf-8

##### encode (可选)

* 请求报文的编码
* 默认:utf-8

##### headers (可选)

* HTTP报文头, Map

##### verboseLog (可选)

* 开启更多的日志输出
* 默认:false

##### verboseLogConfig (可选)

* 细粒度调整日志输出
* 在verboseLog=true时有效
* 默认:全输出

```text
1   (0x00000001) -> 打印: URL后缀 / URL参数(Map) / 请求报文体(Hex)
16  (0x00000010) -> 打印: 请求报文体(String)
256 (0x00000100) -> 打印: 未编码的完整URL(包括参数), http://host:port/app?key1=value1格式
4096(0x00001000) -> 打印: 响应码 / 响应信息

例如需要打印未编码的完整URL和请求报文体(String), 设置值为:256 + 16 = 272
```

##### cookieJar (可选)

* 实现请求的Cookie管理, 需自行实现
* 默认:无

##### proxy (可选)

* 代理配置
* 默认:无

##### dns (可选)

* DNS配置
* 默认:无
