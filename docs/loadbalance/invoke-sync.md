# HttpClient调用方法(同步GET/POST)

* 关于URL地址

> 以本文档中的代码为例 <br>
> http://127.0.0.1:8081 和 http://127.0.0.1:8082 是两台应用服务器, 我们要向他们请求数据, 请求的URL后缀为 /user/update.json <br>
> 配置SimpleOkHttpClient或LoadBalancedHostManager的hosts参数为 http://127.0.0.1:8081,http://127.0.0.1:8082 <br>
> 调用客户端发送请求 multiHostOkHttpClient.get("/user/update.json").send() <br>
> 程序会自动选择一个应用服务器, 然后将应用服务器的地址与URL后缀拼接 <br>
> 最终的请求地址为 http://127.0.0.1:8081/user/update.json 或 http://127.0.0.1:8082/user/update.json <br>

* 建议

> 同步方式通常在服务端使用时, 通常需要自行实现线程隔离/线程数限制/等待队列限制等 <br>
> 同步方式中`maxThreads` / `maxThreadsPerHost`配置无效, 使用调用线程发起网络请求 <br>

* 特别注意

> 切勿在发送请求前调整客户端配置, 很多配置是异步生效的(例如: 在发送前使用setHosts方法设置后端地址, 最终请求还是会发往老地址!!!). 
> 更不能将一个客户端用于发往不同的服务方集群(它们提供不同的服务), 会导致严重的错发现象. 必须严格按照一个服务方集群对应一个客户端实例的方式使用.

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
