# HttpClient配置方法(SpringBoot自动配置)

* `Maven/Gradle依赖配置`在本文最后

# `注意 | WARNING`

```text
一个客户端实例对应一个服务端集群, 若要向不同的服务端集群发送请求, 必须创建多个客户端实例. 例如, 客户端需要向 
A系统(150.1.1.1,155.1.1.2) 和 B系统(150.1.2.1,150.1.2.2) 发送请求, 则需要配置两个客户端: 
ClientA (150.1.1.1,155.1.1.2) 和 ClientB (150.1.2.1,155.1.2.2). 使用ClientA向A系统发送请求, 
使用ClientB向B系统发送请求. 严禁将一个客户端用于请求不同的服务方集群, 这会导致请求被发往错误的服务端!!!

A client corresponds to a server cluster. You should use different client instances to send requests 
to different server clusters. For example, we need to send requests to system A (150.1.1.1, 155.1.1.2) 
and system B (150.1.2.1, 150.1.2.2), we need to create two clients: client A (150.1.1.1, 155.1.1.2) 
and client B (150.1.2.1, 155.1.2.2). Use client A to send requests to system A and client B to send 
requests to system B. It is strictly forbidden to requesting different service clusters by one client 
instance, the request will be sent to the wrong host!!!
```

```text
    /**
     * 错误示范, 请勿模仿!!!
     * 一个客户端用于向不同的后端服务发送请求, 后端地址在请求时才指定. 这种方式有严重的问题, 
     * 不仅仅是发送请求时无法使用到刚设置的后端地址, 而且在多线程环境下会把请求发到错误的服务端. 
     *
     * Error demonstration, please do not imitate !!!
     * If a client is used to send requests to different host clusters (which provide 
     * different services), and you want to specify the host address before request, 
     * This is a big mistake ! The new hosts you set cannot take effect immediately, 
     * and requests will be send to the wrong host! 
     */
    public byte[] send(String hosts, byte[] request) {
        client.setHosts(hosts);//错误 Wrong !!!
        return client.post("/post/json")
                .body(request)
                .sendForBytes();
    }
```

```text
客户端所有配置均可以在运行时调整, set系列方法均为线程安全. 但是, 配置的调整是异步生效的, 即不会在执行set
方法的同时生效. 例如, 在发送请求前修改服务端地址(hosts), 请求仍然会被发往老的服务端地址. 
正确的方式是: 开发一个控制台, 在控制台中调整参数时, 调用客户端的set系列方法调整配置; 使用Apollo配置中心, 
监听到配置发生变化时, 调用客户端的set系列方法调整配置. 
错误的方式是: 在每次发送请求前调用set系列方法调整配置. 

All configuration of the client can be adjusted at runtime, and all the setter methods are thread 
safe. However, the configuration will be applied asynchronously, that is, they do not take effect 
at the same time as the set method is invoked.For example, modify the server address (hosts) before 
sending the request, the request will still be sent to the old address.
The correct way is: develop a console, invoke the client's setter method while adjusting 
configuration in console; Use a configuration center like the Apollo, monitoring configuration 
changes, invoke the client's setter method while the configuration changed.
The wrong way is: to invoke the setter method (adjust configurations) before sending the request.
```

```text
    @Value("${hosts:}")
    private String hosts;

    /**
     * 错误示范, 请勿模仿!!!
     * 在发送请求前, 才被动地设置最新的hosts, 客户端最终会使用旧的hosts发送请求, 新的hosts不生效!!!
     *
     * Error demonstration, please do not imitate !!!
     * If you invoke the setHosts method to set the hosts before requesting, the request will 
     * still be sent to the old hosts !
     */
    public byte[] send(byte[] request) {
        client.setHosts("http://127.0.0.1:8080");//错误 Wrong !!!
        return client.post("/post/json")
                .body(request)
                .sendForBytes();
    }
```

<br>
<br>
<br>

# YML配置(推荐)

* 在application.yml/application-profile.yml中增加配置

```yaml
slate:
  httpclient:
    enabled: true
  httpclients:
    client1:
      hosts: http://127.0.0.1:8081,http://127.0.0.1:8082
      initiativeInspectInterval: 5000
      passiveBlockDuration: 30000
      connectTimeout: 3000
      writeTimeout: 10000
      readTimeout: 10000
      maxReadLength: 10485760
      verboseLog: true
      txTimerEnabled: true
    client2:
      hostList:
        - http://127.0.0.1:8083
        - http://127.0.0.1:8084
      initiativeInspectInterval: 5000
      passiveBlockDuration: 30000
      connectTimeout: 3000
      writeTimeout: 10000
      readTimeout: 10000
      maxReadLength: 10485760
      verboseLog: true
      txTimerEnabled: true
```

* 以上文为例, 启用并配置了client1和client2两个HTTP请求客户端
* client1有两个主机http://127.0.0.1:8081和http://127.0.0.1:8082
* client2有两个主机http://127.0.0.1:8083和http://127.0.0.1:8084
* hosts配置的优先级比hostList高, 同时配置时只有hosts生效
* 健康主动探测间隔(initiativeInspectInterval)为5000ms
* 健康被动探测阻断时长(passiveBlockDuration)为30000ms
* connectTimeout/writeTimeout/readTimeout分别为连接/写/读超时时间, 单位ms
* maxReadLength数据最大读取长度, 单位字节
* verboseLog为true时会输出更多日志
* 启用TxTimer对请求耗时的统计(目前只支持同步方式)

### YML中所提供的全部配置说明

```yaml
slate:
  httpclient:
    # 启用HttpClients (必须)
    enabled: true
    # 使用Apollo配置中心动态调整配置 (可选)
    apollo-support: true
  httpclients:
    client1:
      # 后端列表
      hosts: http://127.0.0.1:8083,http://127.0.0.1:8084
      # 健康主动探测间隔, 单位ms
      initiativeInspectInterval: 5000
      # true: 当所有后端都被阻断时不发送请求(抛异常), false: 当所有后端都被阻断时随机发送请求
      returnNullIfAllBlocked: false
      # 启用HTTP Get方式进行主动健康探测, URL为http://127.0.0.1:8083/health和http://127.0.0.1:8084/health, (设置+telnet+改回TELNET方式)
      httpGetInspectorUrlSuffix: /health
      # 主动探测器打印更多日志
      inspectorVerboseLog: false
      # 健康被动探测阻断时长, 单位ms
      passiveBlockDuration: 30000
      # mediaType
      mediaType: application/json;charset=utf-8
      # 编码
      encode: utf-8
      # Http请求头, 键值对格式参考: https://github.com/shepherdviolet/thistle/blob/master/docs/kvencoder/guide.md
      headers: User-Agent=SlateHttpClient,Referer=http://github.com
      # 阻断后的恢复期系数, 恢复期时长 = blockDuration * recoveryCoefficient, 设置1则无恢复期
      recoveryCoefficient: 10
      # 最大闲置连接数. 客户端会保持与服务端的连接, 保持数量由此设置决定, 直到闲置超过5分钟. 默认16
      maxIdleConnections: 16
      # 异步方式最大线程数, 配置仅在异步方式有效, 同步无限制
      maxThreads: 256
      # 异步方式每个后端最大线程数, 配置仅在异步方式有效, 同步无限制
      maxThreadsPerHost: 256
      # 连接超时时间, 单位ms
      connectTimeout: 3000
      # 写超时时间, 单位ms
      writeTimeout: 10000
      # 读超时时间, 单位ms
      readTimeout: 10000
      # 数据最大读取长度, 单位字节
      maxReadLength: 10485760
      # 当后端HTTP返回码为400或500时阻断后端
      httpCodeNeedBlock: 400,500
      # true时会输出更多日志
      verboseLog: false
      # true启用TxTimer对请求耗时的统计(目前只支持同步方式)
      txTimerEnabled: false
```

<br>
<br>
<br>

# 手动配置

* 除了用YML配置, 也可以进行手动配置, 参考https://github.com/shepherdviolet/slate/blob/master/docs/loadbalance/config-annotation.md

<br>
<br>
<br>

# 获得客户端实例

* 获得所有客户端(包括运行时动态添加的)

```text
    private SimpleOkHttpClient client1;
    
    /**
     * 使用构造注入, 保证在操作时HttpClients已经注入
     */
    @Autowired
    public Constructor(HttpClients httpClients) {
        this.client1 = httpClients.get("cliente1");
    }
}
```

* 注解注入(无法获得运行时动态添加的)
* 注意, 无法用@Autowired注解获得客户端

```text
    @HttpClient("client1")
    private SimpleOkHttpClient client1;
    
    @HttpClient("client2")
    public void setClient2(SimpleOkHttpClient client2) {
        // ......
    }
```

<br>
<br>
<br>

# 运行时调整配置

* 部分配置未支持用YML配置, 必须通过手动配置(例如: 设置CookieJar, 设置Proxy等)

```text
客户端所有配置均可以在运行时调整, set系列方法均为线程安全. 但是, 配置的调整是异步生效的, 即不会在执行set方法的同时生效. 
例如, 在发送请求前修改服务端地址(hosts), 请求仍然会被发往老的服务端地址. 
正确的方式是: 开发一个控制台, 在控制台中调整参数时, 调用客户端的set系列方法调整配置; 使用Apollo配置中心, 监听到配置发生变化时, 
调用客户端的set系列方法调整配置. 
错误的方式是: 在每次发送请求前调用set系列方法调整配置. 
```

```text
@Component
public class MyHttpTransport implements InitializingBean {

    private SimpleOkHttpClient client1;
    
    /**
     * 使用构造注入, 保证simpleOkHttpClient优先注入, 使用时不会为null
     */
    @Autowired
    public HttpClientConfigChangeListener(HttpClients httpClients) {
        this.client1 = httpClients.get("client1");
    }

    /**
     * 示例1:
     * 在管理平台设置新参数时, 调用SimpleOkHttpClient的set系列方法调整客户端的配置
     * 更多配置请看SimpleOkHttpClient和MultiHostOkHttpClient类的方法注释
     */
    public void setHosts(......) {
        client1.setHosts(......);
    }
    
    /**
     * 示例2:
     * 可以在afterPropertiesSet方法中, 给客户端添加代理/SSL连接工厂等高级配置
     * 更多配置请看SimpleOkHttpClient和MultiHostOkHttpClient类的方法注释
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        client1
                .setProxy(......)
                .setSSLSocketFactory(......);
    }

}
```

<br>
<br>
<br>

# 使用启动参数调整配置

* HttpClients支持用启动参数调整配置, 但必须重启应用才会生效
* 启动参数格式: -Dslate.httpclients.`客户端标识`.`配置名`=`配置值`

* 例如: 修改client2的hosts为http://127.0.0.1:8083,http://127.0.0.1:8084

> -Dslate.httpclients.client2.hosts=http://127.0.0.1:8083,http://127.0.0.1:8084<br>

* 例如: 给client1添加两个Http请求头 [键值对格式](https://github.com/shepherdviolet/thistle/blob/master/docs/kvencoder/guide.md)

> -Dslate.httpclients.client1.headers=User-Agent=SlateHttpClient,Referer=http://github.com<br>

* 其他说明

> 如果`客户端标识`是新增的, 应用会创建一个新的HttpClient实例<br>

<br>
<br>
<br>

# 使用Apollo配置中心实时调整配置

* 启用Apollo, 配置正确的namespace

```text
@EnableApolloConfig({
    "application"
})
@SpringBootApplication
public class BootApplication {
}
```

* 在application.yml/application-profile.yml中增加配置

```yaml
slate:
  httpclient:
    apollo-support: true
```

* 进入Apollo配置中心控制台, 新增或修改应用的`私有配置(namespace=application)`, 应用端的HttpClient配置就会实时调整
* 注意: 通过YML方式启用, 只能配置在应用的`私有配置(namespace=application)`中, 配在`公共配置或非默认配置(namespace!=application)`中无效
* Key格式: slate.httpclients.`客户端标识`.`配置名`

* 例如: 修改client2的hosts为http://127.0.0.1:8083,http://127.0.0.1:8084

> Key: `slate.httpclients.client2.hosts`<br>
> Value: `http://127.0.0.1:8083,http://127.0.0.1:8084`<br>

* 例如: 给client1添加两个Http请求头 [键值对格式](https://github.com/shepherdviolet/thistle/blob/master/docs/kvencoder/guide.md)

> Key: `slate.httpclients.client1.headers`<br>
> Value: `User-Agent=SlateHttpClient,Referer=http://github.com`<br>

* 其他说明

> 如果`客户端标识`是新增的, 应用端会实时创建一个新的HttpClient实例<br>
> 在日志中搜索`HttpClients`关键字可以观察到配置实时调整的情况<br>

* (可选) 如果想要配置在`公共配置或非默认配置(namespace!=application)`中, 请添加如下配置类, 并指定namespace

```text
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfig;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import org.springframework.beans.factory.annotation.Autowired;
import sviolet.slate.common.x.net.loadbalance.springboot.HttpClients;
import sviolet.slate.common.x.net.loadbalance.springboot.apollo.HttpClientsApolloOverrideSettings;

@Configuration
public class HttpClientsApolloConfig {

    private HttpClients httpClients;

    //构造注入确保第一时间获得实例
    @Autowired
    public HttpClientsApolloConfig(HttpClients httpClients) {
        this.httpClients = httpClients;
    }

    //获得Apollo配置实例, 注意配置正确的namespace
    @ApolloConfig("it.common")
    private Config config;

    //监听Apollo配置变化, 注意配置正确的namespace
    @ApolloConfigChangeListener("it.common")
    public void onApolloConfigChanged(ConfigChangeEvent configChangeEvent){
        //实时调整HttpClient配置
        httpClients.settingsOverride(new HttpClientsApolloOverrideSettings(config));
    }

}
```

<br>
<br>
<br>

# 依赖

* gradle

```text
//version替换为具体版本, 另外需要依赖spring库
dependencies {
    compile 'com.github.shepherdviolet:slate-http-client:version'
}
```

* maven

```maven
    <!--version替换为具体版本, 另外需要依赖spring库-->
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-http-client</artifactId>
        <version>version</version>
    </dependency>
```
