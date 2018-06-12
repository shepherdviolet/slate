# 支持软负载的Http请求客户端使用手册

* 包路径:sviolet.slate.common.modelx.loadbalance
* 支持配置多个后端地址, 平均分配流量
* 支持被动/主动方式探测后端是否可用, 自动选择可用的后端发送请求
* 支持同步/异步方式请求

# 依赖

* gradle

```gradle

//依赖
dependencies {
    compile 'com.github.shepherdviolet:slate-common:9.5'
}
```

* gradle(最少依赖)

```gradle
dependencies {
    compile ('com.github.shepherdviolet:slate-common:9.5') {
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
        <version>9.5</version>
    </dependency>
```

* maven(最少依赖)

```maven
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-common</artifactId>
        <version>9.5</version>
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

* Spring(XML)

```gradle

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
    <bean id="loadBalancedOkHttpClient" class="sviolet.slate.common.modelx.loadbalance.classic.LoadBalancedOkHttpClient">
        <property name="hostManager" ref="loadBalancedHostManager"/>
        <property name="maxThreads" ref="200"/>
        <property name="maxThreadsPerHost" ref="200"/>
        <property name="passiveBlockDuration" value="3000"/>
        <property name="connectTimeout" value="3000"/>
        <property name="writeTimeout" value="10000"/>
        <property name="readTimeout" value="10000"/>
    </bean>
    
```

* Spring(注解)

```gradle

    /**
     * 后端管理器
     * 配置管理后端地址和状态
     */
    @Bean
    public LoadBalancedHostManager loadBalancedHostManager() {
        LoadBalancedHostManager hostManager = new LoadBalancedHostManager();
        hostManager.setHostArray(new String[]{
            "http://127.0.0.1:8080",
            "http://127.0.0.1:8081"
        });
        return hostManager;
    }
    
    /**
     * 主动探测管理器
     * 定时探测后端状态(默认Telnet方式)
     */
    @Bean
    public LoadBalancedInspectManager loadBalancedInspectManager(LoadBalancedHostManager loadBalancedHostManager) {
        LoadBalancedInspectManager inspectManager = new LoadBalancedInspectManager();
        inspectManager.setHostManager(loadBalancedHostManager);
        inspectManager.setInspectInterval(5000L);
        //inspectManager.setInspector(new TelnetLoadBalanceInspector());
        return inspectManager;
    }
    
    /**
     * HTTP请求客户端
     * 调用该实例发送请求
     */ 
    @Bean
    public LoadBalancedOkHttpClient loadBalancedOkHttpClient(LoadBalancedHostManager loadBalancedHostManager) {
        LoadBalancedOkHttpClient client = new LoadBalancedOkHttpClient();
        client.setHostManager(loadBalancedHostManager);
        client.setMaxThreads(200);
        client.setMaxThreadsPerHost(200);
        client.setPassiveBlockDuration(3000L);
        client.setConnectTimeout(3000L);
        client.setWriteTimeout(10000L);
        client.setReadTimeout(10000L);
        return client;
    }
    
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

### LoadBalancedOkHttpClient配置

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

##### cookieJar (可选)

* 实现请求的Cookie管理, 需自行实现
* 默认:无

##### proxy (可选)

* 代理配置
* 默认:无

##### dns

* DNS配置
* 默认:无


