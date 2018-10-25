# HttpClient配置方法(SpringBoot自动配置)

* `Maven/Gradle依赖配置`在本文最后

<br>
<br>
<br>

# YML配置(推荐)

* 在application.yml/application-profile.yml中增加配置

```yaml
slate:
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

* 以上文为例, 配置了client1和client2两个HTTP请求客户端
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
      # Http请求头
      headers: 
        User-Agent: SlateHttpClient
        Referer: http://github.com
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
* 注意: 有一部分配置未在YML中提供, 必须通过手动配置(例如: 设置CookieJar, 设置Proxy等)

<br>
<br>
<br>

# 获得HttpClient

* YML只配置了一个客户端时, 可以直接获得SimpleOkHttpClient

```text
    private SimpleOkHttpClient simpleOkHttpClient;
    
    /**
     * 使用构造注入, 保证在操作时simpleOkHttpClient已经注入
     */
    @Autowired
    public Constructor(SimpleOkHttpClient simpleOkHttpClient) {
        this.simpleOkHttpClient = simpleOkHttpClient;
    }
```

* YML配置了多个客户端时, 需要从HttpClients对象中获得

```text
    private SimpleOkHttpClient client1;
    
    /**
     * 使用构造注入, 保证在操作时simpleOkHttpClient已经注入
     */
    @Autowired
    public Constructor(HttpClients httpClients) {
        this.client1 = httpClients.get("cliente1");
    }
}
```

<br>
<br>
<br>

# 运行时调整配置

```text
为了复杂的服务端场景, SimpleOkHttpClient/MultiHostOkHttpClient
所有的配置均可以在运行时调整, 所有Set方法均为线程安全, 但有些配置的调整是异步生效的(不保证在执行set方法时生效).
```

```text
@Component
public class MyComponent {

    private SimpleOkHttpClient simpleOkHttpClient;
    
    /**
     * 使用构造注入, 保证在setter操作时simpleOkHttpClient已经注入
     */
    @Autowired
    public HttpClientConfigChangeListener(SimpleOkHttpClient simpleOkHttpClient) {
        this.simpleOkHttpClient = simpleOkHttpClient;
    }

    /**
     * 示例, 运行时调整SimpleOkHttpClient的配置, 准实时生效
     * 更多配置请看SimpleOkHttpClient和MultiHostOkHttpClient类的方法注释
     */
    public void changeSettings(......) {
        simpleOkHttpClient
                .setHosts(......)
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

* 确保Apollo配置启用, 并配置正确的namespace

```text
@EnableApolloConfig(
        {"application", "it.common"}
)
@SpringBootApplication
public class BootApplication {
}
```

* 添加一个配置类, 监听Apollo配置变化并实时调整HttpClient的配置(甚至能够新增客户端)

```text
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfig;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import sviolet.slate.springboot.x.net.loadbalance.auto.HttpClients;
import java.util.Set;

@Configuration
public class HttpClientsApolloConfig {

    private HttpClients httpClients;

    //构造注入确保第一时间获得实例
    @Autowired
    public HttpClientsApolloConfig(HttpClients httpClients) {
        this.httpClients = httpClients;
    }

    //获得Apollo配置实例, 注意配置正确的namespace
    @ApolloConfig("application")
    private Config config;

    //监听Apollo配置变化
    @ApolloConfigChangeListener("application")
    private void onApolloConfigChanged(ConfigChangeEvent configChangeEvent){
        //实时调整HttpClient配置
        httpClients.settingsOverride(new ApolloOverrideSettings(config));
    }

    //将Apollo配置包装为OverrideSettings
    private static class ApolloOverrideSettings implements HttpClients.OverrideSettings {

        private Config config;

        private ApolloOverrideSettings(Config config) {
            //持有Apollo配置
            this.config = config;
        }

        @Override
        public Set<String> getKeys() {
            //获取所有配置key
            return config.getPropertyNames();
        }

        @Override
        public String getValue(String key) {
            //根据key返回配置value, 不存在返回null
            return config.getProperty(key, null);
        }

    }

}
```

* 在Apollo配置中心添加配置并发布, 应用端的HttpClient配置就会实时调整
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

<br>
<br>
<br>

# 依赖

* gradle

```text
//version替换为具体版本, 另外需要依赖spring库
dependencies {
    compile 'com.github.shepherdviolet:slate-springboot:version'
}
```

* gradle(最少依赖)

```text
//version替换为具体版本, 另外需要依赖spring库
dependencies {
    compile ('com.github.shepherdviolet:slate-springboot:version') {
        transitive = false
    }
    compile ('com.github.shepherdviolet:slate-common:version') {
        transitive = false
    }
    compile ('com.github.shepherdviolet:thistle:version') {
        transitive = false
    }
    compile 'com.squareup.okhttp3:okhttp:3.9.0'
    compile 'com.google.code.gson:gson:2.8.1'
    compile 'ch.qos.logback:logback-classic:1.2.3'
}
```

* maven

```maven
    <!--version替换为具体版本, 另外需要依赖spring库-->
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-springboot</artifactId>
        <version>version</version>
    </dependency>
```

* maven(最少依赖)

```maven
    <!--version替换为具体版本, 另外需要依赖spring库-->
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-springboot</artifactId>
        <version>version</version>
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
        <version>version</version>
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
        <version>version</version>
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
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.8.1</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.2.3</version>
    </dependency>
```
