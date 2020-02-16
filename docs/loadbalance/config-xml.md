# HttpClient配置方法(XML)

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

# 简化版配置(推荐)

* SimpleOkHttpClient继承了MultiHostOkHttpClient, 同时封装了LoadBalancedHostManager和LoadBalancedInspectManager, 简化了配置, 免去了配置三个Bean的麻烦
* 配置被简化, 如需高度定制, 请使用LoadBalancedHostManager + LoadBalancedInspectManager + MultiHostOkHttpClient
* 内置的LoadBalancedInspectManager采用TELNET方式探测后端, 可以改为HttpGet方式
* 屏蔽了setHostManager()方法, 调用会抛出异常
* 实现了DisposableBean, 在Spring容器中会自动销毁
* 若SimpleOkHttpClient在Spring中注册为Bean, 主动探测器会在Spring启动后自动开始. 否则需要手动调用SimpleOkHttpClient.start()方法开始主动探测

```text
    <bean id="simpleOkHttpClient" class="sviolet.slate.common.x.net.loadbalance.classic.SimpleOkHttpClient">
        <property name="hosts" value="http://127.0.0.1:8081,http://127.0.0.1:8082"/>
        <property name="initiativeInspectInterval" value="5000"/><!-- 健康主动探测间隔为5000ms -->
        <property name="passiveBlockDuration" value="30000"/><!-- 健康被动探测阻断时长为30000ms, 被动阻断时间建议与所有超时时间加起来接近 -->
        <property name="connectTimeout" value="3000"/><!-- 连接超时时间, 单位ms -->
        <property name="writeTimeout" value="10000"/><!-- 写超时时间, 单位ms -->
        <property name="readTimeout" value="10000"/><!-- 读超时时间, 单位ms -->
        <property name="maxReadLength" value="10485760"/><!-- 数据最大读取长度, 单位字节 -->
        <property name="dataConverter" ref="dataConverter"/><!-- 设置数据转换器 -->
        <property name="verboseLog" value="true"/><!-- 为true时会输出更多日志 -->
        <!--<property name="verboseLogConfig" value="0x00000110"/> 微调输出的日志内容(详见源码)-->
        <!--<property name="httpGetInspector" ref="/health"/> 启用HTTP Get方式进行主动健康探测, URL为http://127.0.0.1:8083/health和http://127.0.0.1:8084/health, (设置+telnet+改回TELNET方式)-->
    </bean>
```

<br>
<br>
<br>

# 标准配置(不推荐)

* `LoadBalancedInspectManager需要配置destroy-method="close"`

```text

    <!-- 后端管理器 -->
    <!-- 配置管理后端地址和状态 -->
    <bean id="loadBalancedHostManager" class="sviolet.slate.common.x.net.loadbalance.LoadBalancedHostManager">
        <property name="hosts" value="http://127.0.0.1:8081,http://127.0.0.1:8082"/>
    </bean>
    
    <!-- 主动探测管理器 -->
    <!-- 定时探测后端状态(默认Telnet方式) -->
    <bean id="loadBalancedInspector" class="sviolet.slate.common.x.net.loadbalance.LoadBalancedInspectManager"
        destroy-method="close">
        <property name="hostManager" ref="loadBalancedHostManager"/><!-- 持有LoadBalancedHostManager -->
        <property name="inspectInterval" value="5000"/><!-- 健康主动探测间隔为5000ms -->
    </bean>
    
    <!-- HTTP请求客户端 -->
    <!-- 调用该实例发送请求 -->
    <bean id="multiHostOkHttpClient" class="sviolet.slate.common.x.net.loadbalance.classic.MultiHostOkHttpClient">
        <property name="hostManager" ref="loadBalancedHostManager"/><!-- 持有LoadBalancedHostManager -->
        <property name="passiveBlockDuration" value="30000"/><!-- 健康被动探测阻断时长为30000ms, 被动阻断时间建议与所有超时时间加起来接近 -->
        <property name="connectTimeout" value="3000"/><!-- 连接超时时间, 单位ms -->
        <property name="writeTimeout" value="10000"/><!-- 写超时时间, 单位ms -->
        <property name="readTimeout" value="10000"/><!-- 读超时时间, 单位ms -->
        <property name="maxReadLength" value="10485760"/><!-- 数据最大读取长度, 单位字节 -->
        <property name="dataConverter" ref="dataConverter"/><!-- 设置数据转换器 -->
        <property name="verboseLog" value="true"/><!-- true: INFO级别可打印更多的日志(请求报文/响应码等), 默认false -->
        <!--<property name="verboseLogConfig" value="0x00000110"/> 微调输出的日志内容(详见源码)-->
    </bean>
    
```

<br>
<br>
<br>

# 运行时调整配置

```text
客户端所有配置均可以在运行时调整, set系列方法均为线程安全. 但是, 配置的调整是异步生效的, 即不会在执行set方法的同时生效. 
例如, 在发送请求前修改服务端地址(hosts), 请求仍然会被发往老的服务端地址. 
正确的方式是: 开发一个控制台, 在控制台中调整参数时, 调用客户端的set系列方法调整配置; 使用Apollo配置中心, 监听到配置发生变化时, 
调用客户端的set系列方法调整配置. 
错误的方式是: 在每次发送请求前调用set系列方法调整配置. 
```

```text
public class MyHttpTransport implements InitializingBean {

    private SimpleOkHttpClient simpleOkHttpClient;
    
    public void setSimpleOkHttpClient(SimpleOkHttpClient simpleOkHttpClient) {
        this.simpleOkHttpClient = simpleOkHttpClient;
    }

    /**
     * 示例1:
     * 在管理平台设置新参数时, 调用SimpleOkHttpClient的set系列方法调整客户端的配置
     * 更多配置请看SimpleOkHttpClient和MultiHostOkHttpClient类的方法注释
     */
    public void setHosts(......) {
        if (simpleOkHttpClient != null) {
            simpleOkHttpClient.setHosts(......);
        }
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

<br>
<br>
<br>

# 使用Apollo配置中心实时调整配置

* Apollo配置中心新版本能实时更新XML属性和@Value注解中的${...}参数
* 因此只需要按如下方式使用, 即可根据Apollo实时调整配置

```text

    <!-- 启用apollo(任意一个XML中声明过一次即可) -->
    <apollo:config/>

    <!-- 使用${...}应用apollo参数 -->
    <bean id="simpleOkHttpClient" class="sviolet.slate.common.x.net.loadbalance.classic.SimpleOkHttpClient">
        <property name="hosts" value="${http.client.hosts}"/>
        ......
    </bean>
```

<br>
<br>
<br>

# 依赖

* gradle

```text
//version替换为具体版本, 另外需要依赖spring库
dependencies {
    compile 'com.github.shepherdviolet.slate20:slate-http-client:version'
}
```

* maven

```maven
    <!--version替换为具体版本, 另外需要依赖spring库-->
    <dependency>
        <groupId>com.github.shepherdviolet.slate20</groupId>
        <artifactId>slate-http-client</artifactId>
        <version>?</version>
    </dependency>
```
