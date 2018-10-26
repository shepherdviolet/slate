# HttpClient配置方法(注解)

* `Maven/Gradle依赖配置`在本文最后

<br>
<br>
<br>

# 简化版配置(推荐)

* SimpleOkHttpClient继承了MultiHostOkHttpClient, 同时封装了LoadBalancedHostManager和LoadBalancedInspectManager, 简化了配置, 免去了配置三个Bean的麻烦
* 配置被简化, 如需高度定制, 请使用LoadBalancedHostManager + LoadBalancedInspectManager + MultiHostOkHttpClient
* 内置的LoadBalancedInspectManager采用TELNET方式探测后端, 可以改为HttpGet方式
* 屏蔽了setHostManager()方法, 调用会抛出异常
* 实现了DisposableBean, 在Spring容器中会自动销毁
* 若SimpleOkHttpClient在Spring中注册为Bean, 主动探测器会在Spring启动后自动开始. 否则需要手动调用SimpleOkHttpClient.start()方法开始主动探测

```text
@Configuration
public class MyConfiguration {

    @Value("${http.client.hosts}")
    private String hosts;
    
    /**
     * 更多配置请看SimpleOkHttpClient和MultiHostOkHttpClient类的方法注释
     */
    @Bean
    public SimpleOkHttpClient simpleOkHttpClient() {
        return (SimpleOkHttpClient) new SimpleOkHttpClient()
                .setHosts(hosts)//配置后端列表
                .setInitiativeInspectInterval(5000L)//健康主动探测间隔为5000ms
                .setPassiveBlockDuration(30000L)//健康被动探测阻断时长为30000ms, 被动阻断时间建议与所有超时时间加起来接近
                .setConnectTimeout(3000L)//连接超时时间, 单位ms
                .setWriteTimeout(10000L)//写超时时间, 单位ms
                .setReadTimeout(10000L)//读超时时间, 单位ms
                .setMaxReadLength(10L * 1024L * 1024L)//数据最大读取长度, 单位字节
                .setDataConverter(new GsonDataConverter())//设置数据转换器
                .setVerboseLog(true)//为true时会输出更多日志
                //.setVerboseLogConfig(0x00000110)//微调输出的日志内容(详见源码)
                //.setHttpGetInspector("/health")//启用HTTP Get方式进行主动健康探测, URL为http://127.0.0.1:8083/health和http://127.0.0.1:8084/health, (设置+telnet+改回TELNET方式)
                .setTxTimerEnabled(true);//启用TxTimer对请求耗时的统计(目前只支持同步方式)
    }

}
```

<br>
<br>
<br>

# 标准配置(不推荐)

* `LoadBalancedInspectManager需要配置@Bean(destroyMethod = "close")`

```text
@Configuration
public class MyConfiguration {

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
     * 更多配置请看LoadBalancedInspectManager类的方法注释
     */
    @Bean(destroyMethod = "close")
    public LoadBalancedInspectManager loadBalancedInspectManager(LoadBalancedHostManager loadBalancedHostManager) {
        return new LoadBalancedInspectManager()
                //.setInspector(new HttpGetLoadBalanceInspector("/health", 1000L))//启用HTTP Get方式进行主动健康探测, URL为http://127.0.0.1:8083/health和http://127.0.0.1:8084/health, 超时时间建议为主动探测间隔的四分之一
                .setHostManager(loadBalancedHostManager)//持有LoadBalancedHostManager
                .setInspectInterval(5000L);//主动探测间隔    
    }
    
    /**
     * HTTP请求客户端
     * 调用该实例发送请求
     * 更多配置请看SimpleOkHttpClient和MultiHostOkHttpClient类的方法注释
     */ 
    @Bean
    public MultiHostOkHttpClient multiHostOkHttpClient(LoadBalancedHostManager loadBalancedHostManager) {
        return new MultiHostOkHttpClient()
                .setHostManager(loadBalancedHostManager)//持有LoadBalancedHostManager
                .setPassiveBlockDuration(30000L)//健康被动探测阻断时长为30000ms, 被动阻断时间建议与所有超时时间加起来接近
                .setConnectTimeout(3000L)//连接超时时间, 单位ms
                .setWriteTimeout(10000L)//写超时时间, 单位ms
                .setReadTimeout(10000L)//读超时时间, 单位ms
                .setMaxReadLength(10L * 1024L * 1024L)//数据最大读取长度, 单位字节
                .setVerboseLog(true)//为true时会输出更多日志
                .setTxTimerEnabled(true);//启用TxTimer对请求耗时的统计(目前只支持同步方式)
    }

}
```

<br>
<br>
<br>

# 运行时调整配置

```text
为了复杂的服务端场景, SimpleOkHttpClient/MultiHostOkHttpClient所有的配置均可以在运行时调整, set系列方法均为线程安全. 
但是要注意, 有些配置的调整是异步生效的(例如setHosts), 即不保证在执行set方法的同时生效. 
因此, 当用户需要调整客户端配置时, 程序必须主动调用set系列方法调整配置, 而不是等到客户端发送请求之前被动调用. 
如果在发送请求之前才调整配置, 客户端很可能会使用老配置发起请求!!!
正确的方式应该是: 开发一个管理平台, 在前端设置新参数时, 后端立刻调用客户端的set系列方法调整配置; 使用Apollo配置中心, 
监听配置发生变化时, 立刻调用客户端的set系列方法调整配置. 
```

```text
@Component
public class MyHttpTransport implements InitializingBean {

    private SimpleOkHttpClient simpleOkHttpClient;
    
    /**
     * 使用构造注入, 保证simpleOkHttpClient优先注入, 使用时不会为null
     */
    @Autowired
    public HttpClientConfigChangeListener(SimpleOkHttpClient simpleOkHttpClient) {
        this.simpleOkHttpClient = simpleOkHttpClient;
    }

    /**
     * 示例1:
     * 在管理平台设置新参数时, 调用SimpleOkHttpClient的set系列方法调整客户端的配置
     * 更多配置请看SimpleOkHttpClient和MultiHostOkHttpClient类的方法注释
     */
    public void setHosts(......) {
        simpleOkHttpClient.setHosts(......);
    }
    
    /**
     * 示例2:
     * 可以在afterPropertiesSet方法中, 给客户端添加代理/SSL连接工厂等高级配置
     * 更多配置请看SimpleOkHttpClient和MultiHostOkHttpClient类的方法注释
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        simpleOkHttpClient
                .setProxy(......)
                .setSSLSocketFactory(......);
    }

}
```

* 错误示范, 请勿模仿!!!

```text
    @Value("${hosts:}")
    private String hosts;

    /**
     * 错误示范, 请勿模仿!!! 错误示范, 请勿模仿!!! 错误示范, 请勿模仿!!! 
     * 在发送请求前, 才被动地设置最新的hosts, 客户端最终会使用旧的hosts发送请求, 新的hosts不生效!!!
     */
    public byte[] send(byte[] request) {
        client.setHosts("http://127.0.0.1:8080");//错误点
        return client.post("/post/json")
                .body(request)
                .sendForBytes();
    }
```

* 错误示范, 请勿模仿!!!

```text
    /**
     * 错误示范, 请勿模仿!!! 错误示范, 请勿模仿!!! 错误示范, 请勿模仿!!! 
     * 一个客户端用于向不同的后端服务发送请求, 后端地址在请求时才指定. 
     * 这种方式有严重的问题, 不仅仅是发送请求时无法使用到刚设置的后端地址, 而且在多线程环境下会把希望发往A的请求发往B/C/D...!!!
     * 本请求客户端的设计思路是, 一个客户端对应一个服务集群, 内部负载均衡逻辑会将请求发往合适的主机. 不同的服务集群应该配置
     * 多个客户端与之对应.
     */
    public byte[] send(String hosts, byte[] request) {
        client.setHosts(hosts);//错误点
        return client.post("/post/json")
                .body(request)
                .sendForBytes();
    }
```

<br>
<br>
<br>

# 使用Apollo配置中心实时调整配置

```text
虽然Apollo配置中心新版本能实时更新XML属性和@Value注解中的${...}参数, 但是使用@Bean方式声明的类, 只在实例化时进行参数赋值. 
因此必须通过其他方式实现, 方法有两种. 
```

* 方法1: @Value注解在方法上, 每次参数变化都会调用该方法, 这样可以实时改变参数

```text
@Component
public class HttpClientConfigChangeListener {

    private SimpleOkHttpClient simpleOkHttpClient;
    
    /**
     * 使用构造注入, 保证在setter操作时simpleOkHttpClient已经注入
     */
    @Autowired
    public HttpClientConfigChangeListener(SimpleOkHttpClient simpleOkHttpClient) {
        this.simpleOkHttpClient = simpleOkHttpClient;
    }

    /**
     * 动态调整hosts配置
     * SimpleOkHttpClient所有的配置均可以在运行时调整, 所有Set方法均为线程安全, 
     * 更多配置请看SimpleOkHttpClient和MultiHostOkHttpClient类的方法注释
     */
    @Value("${http.client.hosts:}")
    public void setHosts(String hosts) {
        if (!CheckUtils.isEmptyOrBlank(hosts)) {
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

    private SimpleOkHttpClient simpleOkHttpClient;
    
    /**
     * 使用构造注入, 保证在setter操作时simpleOkHttpClient已经注入
     */
    @Autowired
    public ApolloConfigChangeService(SimpleOkHttpClient simpleOkHttpClient) {
        this.simpleOkHttpClient = simpleOkHttpClient;
    }

    /**
     * 动态调整配置
     * SimpleOkHttpClient所有的配置均可以在运行时调整, 所有Set方法均为线程安全, 
     * 更多配置请看SimpleOkHttpClient和MultiHostOkHttpClient类的方法注释
     */
    @ApolloConfigChangeListener
    private void onHttpClientChanged(ConfigChangeEvent configChangeEvent){
        if (configChangeEvent.isChanged("http.client.hosts")){
            simpleOkHttpClient.setHosts(apolloConfig.getProperty("http.client.hosts", ""));
        }
        if (configChangeEvent.isChanged("http.client.verboseLog")){
            simpleOkHttpClient.setVerboseLog(apolloConfig.getBooleanProperty("http.client.verboseLog", false));
        }
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
    compile 'com.github.shepherdviolet:slate-common:version'
}
```

* gradle(最少依赖)

```text
//version替换为具体版本, 另外需要依赖spring库
dependencies {
    compile ('com.github.shepherdviolet:slate-common:version') {
        transitive = false
    }
    compile ('com.github.shepherdviolet:thistle:version') {
        transitive = false
    }
    compile 'com.squareup.okhttp3:okhttp:3.9.0'
    compile 'ch.qos.logback:logback-classic:1.2.3'
}
```

* maven

```maven
    <!--version替换为具体版本, 另外需要依赖spring库-->
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-common</artifactId>
        <version>version</version>
    </dependency>
```

* maven(最少依赖)

```maven
    <!--version替换为具体版本, 另外需要依赖spring库-->
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
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.2.3</version>
    </dependency>
```
