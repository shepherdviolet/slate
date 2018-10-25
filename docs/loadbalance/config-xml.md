# HttpClient配置方法(XML)

* `Maven/Gradle依赖配置`在本文最后

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
        <property name="txTimerEnabled" value="true"/><!-- 启用TxTimer对请求耗时的统计(目前只支持同步方式) -->
        <!--<property name="verboseLogConfig" value="0x00000110"/> 微调输出的日志内容(详见源码)-->
        <!--<property name="httpGetInspector" ref="/health"/> 启用HTTP Get方式进行主动健康探测, URL为http://127.0.0.1:8083/health和http://127.0.0.1:8084/health, (设置+telnet+改回TELNET方式)-->
    </bean>
```

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
        <property name="verboseLog" value="true"/><!-- 为true时会输出更多日志 -->
        <property name="txTimerEnabled" value="true"/><!-- 启用TxTimer对请求耗时的统计(目前只支持同步方式) -->
        <!--<property name="verboseLogConfig" value="0x00000110"/> 微调输出的日志内容(详见源码)-->
    </bean>
    
```

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
