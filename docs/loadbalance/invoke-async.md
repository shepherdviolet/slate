# HttpClient调用方法(异步GET/POST)

* 关于URL地址

> 以本文档中的代码为例 <br>
> http://127.0.0.1:8081 和 http://127.0.0.1:8082 是两台应用服务器, 我们要向他们请求数据, 请求的URL后缀为 /user/update.json <br>
> 配置SimpleOkHttpClient或LoadBalancedHostManager的hosts参数为 http://127.0.0.1:8081,http://127.0.0.1:8082 <br>
> 调用客户端发送请求 multiHostOkHttpClient.get("/user/update.json").send() <br>
> 程序会自动选择一个应用服务器, 然后将应用服务器的地址与URL后缀拼接 <br>
> 最终的请求地址为 http://127.0.0.1:8081/user/update.json 或 http://127.0.0.1:8082/user/update.json <br>

* 建议

> 异步方式通常在终端应用使用(安卓客户端等), 便于UI交互 <br>
> 异步方式的等待队列长度无限, 并发数通过`maxThreads` / `maxThreadsPerHost`配置决定 <br>
> 用于服务端时, 建议使用同步方式, 并自行实现线程隔离/线程数限制/等待队列限制等 <br>

* 特别注意

> 切勿在发送请求前调整客户端配置, 很多配置是异步生效的(例如: 在发送前使用setHosts方法设置后端地址, 最终请求还是会发往老地址!!!). 
> 更不能将一个客户端用于发往不同的服务方集群(它们提供不同的服务), 会导致严重的错发现象. 必须严格按照一个服务方集群对应一个客户端实例的方式使用.

### POST

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
