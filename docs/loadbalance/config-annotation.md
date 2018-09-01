# Http请求客户端配置方法(注解)

# 依赖

* gradle

```text

//依赖
dependencies {
    compile 'com.github.shepherdviolet:slate-common:9.13'
}
```

* gradle(最少依赖)

```text
dependencies {
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
        <artifactId>slate-common</artifactId>
        <version>9.13</version>
    </dependency>
```

* maven(最少依赖)

```maven
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

## 标准配置(不推荐)

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
                .setPassiveBlockDuration(6000L)
                .setConnectTimeout(3000L)
                .setWriteTimeout(10000L)
                .setReadTimeout(10000L)
                //.setDataConverter(new GsonDataConverter())
                //.setVerboseLogConfig(MultiHostOkHttpClient.VERBOSE_LOG_CONFIG_RAW_URL|MultiHostOkHttpClient.VERBOSE_LOG_CONFIG_REQUEST_STRING_BODY)
                .setVerboseLog(true);
    }
    
```

## 简化版配置(推荐)

* 在MultiHostOkHttpClient的基础上, 封装了LoadBalancedHostManager和LoadBalancedInspectManager, 简化了配置, 免去了配置三个Bean的麻烦
* 配置被简化, 如需高度定制, 请使用LoadBalancedHostManager + LoadBalancedInspectManager + MultiHostOkHttpClient
* 内置的LoadBalancedInspectManager采用TELNET方式探测后端<br>
* 屏蔽了setHostManager()方法, 调用会抛出异常<br>
* 实现了DisposableBean, 在Spring容器中会自动销毁<br>

```text
@Value("${http.client.hosts}")
private String hosts;

@Bean
public SimpleOkHttpClient simpleOkHttpClient() {
    return (SimpleOkHttpClient) new SimpleOkHttpClient()
            .setHosts(hosts)
            .setInitiativeInspectInterval(5000L)
            .setMaxThreads(200)
            .setMaxThreadsPerHost(200)
            .setPassiveBlockDuration(6000L)
            .setConnectTimeout(3000L)
            .setWriteTimeout(10000L)
            .setReadTimeout(10000L)
            //.setDataConverter(new GsonDataConverter())
            //.setHttpGetInspector("/health")
            //.setVerboseLogConfig(MultiHostOkHttpClient.VERBOSE_LOG_CONFIG_RAW_URL|MultiHostOkHttpClient.VERBOSE_LOG_CONFIG_REQUEST_STRING_BODY)
            .setVerboseLog(true);
}
```

## 使用Apollo配置中心实时调整配置

* 虽然Apollo配置中心新版本能实时更新XML属性和@Value注解中的${...}参数
* 但是使用@Bean方式声明的类, 只在实例化时进行参数赋值
* 因此必须通过其他方式实现, 方法有两种

* 方法1: @Value注解在方法上, 每次参数变化都会调用该方法, 以此实时改变参数

```text
@Component
public class HttpClientConfigChangeListener {

    @Autowired
    private SimpleOkHttpClient simpleOkHttpClient;

    @Value("http.client.hosts")
    public void setHosts(String hosts) {
        if (simpleOkHttpClient != null) {
            simpleOkHttpClient.setHosts(hosts);
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

    @Autowired
    private SimpleOkHttpClient simpleOkHttpClient;

    @ApolloConfigChangeListener
    private void onHttpClientChanged(ConfigChangeEvent configChangeEvent){
        if (configChangeEvent.isChanged("http.client.hosts")){
            simpleOkHttpClient.setHosts(apolloConfig.getProperty("http.client.hosts", ""));
        }
    }

}
```
