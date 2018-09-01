# Http请求客户端配置方法(SpringBoot自动配置)

# 依赖

* gradle

```text

//依赖
dependencies {
    compile 'com.github.shepherdviolet:slate-springboot:9.13'
}
```

* gradle(最少依赖)

```text
dependencies {
    compile ('com.github.shepherdviolet:slate-springboot:9.13') {
        transitive = false
    }
    compile ('com.github.shepherdviolet:slate-common:9.13') {
        transitive = false
    }
    compile ('com.github.shepherdviolet:thistle:9.9') {
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
        <artifactId>slate-springboot</artifactId>
        <version>9.13</version>
    </dependency>
```

* maven(最少依赖)

```maven
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-springboot</artifactId>
        <version>9.13</version>
        <exclusions>
             <exclusion>
                 <groupId>*</groupId>
                 <artifactId>*</artifactId>
             </exclusion>
        </exclusions>
    </dependency>
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-common</artifactId>
        <version>9.13</version>
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
        <version>9.9</version>
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

## YML配置(推荐)

* 在application.yml/application-profile.yml中增加配置

```yaml
slate:
  httpclients:
    client1:
      hostList:
        - http://127.0.0.1:8081
        - http://127.0.0.1:8082
      initiativeInspectInterval: 5000
      maxThreads: 200
      maxThreadsPerHost: 200
      passiveBlockDuration: 6000
      connectTimeout: 3000
      writeTimeout: 10000
      readTimeout: 10000
      verboseLog: true
    client2:
      hosts: http://127.0.0.1:8083,http://127.0.0.1:8084
      initiativeInspectInterval: 5000
      maxThreads: 200
      maxThreadsPerHost: 200
      passiveBlockDuration: 6000
      connectTimeout: 3000
      writeTimeout: 10000
      readTimeout: 10000
      verboseLog: true
```

* 以上文为例, 配置了client1和client2两个HTTP请求客户端
* client1有两个主机http://127.0.0.1:8081和http://127.0.0.1:8082
* client2有两个主机http://127.0.0.1:8083和http://127.0.0.1:8084
* hosts配置的优先级比hostList高, 同时配置时只有hosts生效
* 健康主动探测间隔(initiativeInspectInterval)为5000ms
* 异步方式最大线程数(maxThreads)为200
* 异步方式每个后端最大线程数(maxThreadsPerHost)为200
* 健康被动探测阻断时长(passiveBlockDuration)为6000ms
* connectTimeout/writeTimeout/readTimeout分别为连接/写/读超时时间, 单位ms
* verboseLog为true时会输出更多日志

## 手动配置

* 除了YML配置方式, 也可以手动配置, 参考:https://github.com/shepherdviolet/slate/blob/master/docs/loadbalance/config-annotation.md

## 注入

* YML只配置了一个客户端时, 可以直接获得SimpleOkHttpClient

```text
@Autowired
private SimpleOkHttpClient simpleOkHttpClient;
```

* YML配置了多个客户端时, 需要从HttpClients对象中获得

```text
private SimpleOkHttpClient client1;

@Autowired
public void client1(HttpClients httpClients) {
    client1 = httpClients.get("client1");
}
```

## 使用Apollo配置中心实时调整配置

* 方法1: @Value注解在方法上, 每次参数变化都会调用该方法, 以此实时改变参数

```text
@Component
public class HttpClientConfigChangeListener {

    private SimpleOkHttpClient client1;
    
    @Autowired
    public void client1(HttpClients httpClients) {
        client1 = httpClients.get("client1");
    }

    @Value("http.client1.hosts")
    public void setHosts(String hosts) {
        if (client1 != null) {
            client1.setHosts(hosts);
        }
    }

}
```

* 方法2: Apollo配置变化监听器

```text
@Component
public class ApolloConfigChangeService {

    @ApolloConfig
    private Config apolloConfig;

    private SimpleOkHttpClient client1;
    
    @Autowired
    public void client1(HttpClients httpClients) {
        client1 = httpClients.get("client1");
    }

    @ApolloConfigChangeListener
    private void onHttpClientChanged(ConfigChangeEvent configChangeEvent){
        if (configChangeEvent.isChanged("http.client1.hosts")){
            client1.setHosts(apolloConfig.getProperty("http.client1.hosts", ""));
        }
    }

}
```
