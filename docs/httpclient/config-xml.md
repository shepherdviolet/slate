# Http请求客户端配置方法(XML)

## 依赖

* gradle

```text

//依赖
dependencies {
    compile 'com.github.shepherdviolet:slate-common:10.1'
}
```

* gradle(最少依赖)

```text
dependencies {
    compile ('com.github.shepherdviolet:slate-common:10.1') {
        transitive = false
    }
    compile ('com.github.shepherdviolet:thistle:10.1') {
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
        <version>10.1</version>
    </dependency>
```

* maven(最少依赖)

```maven
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-common</artifactId>
        <version>10.1</version>
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
        <version>10.1</version>
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

* `LoadBalancedInspectManager需要配置destroy-method="close"`

```text

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
    <bean id="multiHostOkHttpClient" class="sviolet.slate.common.modelx.loadbalance.classic.MultiHostOkHttpClient">
        <property name="hostManager" ref="loadBalancedHostManager"/>
        <property name="maxIdleConnections" value="20"/>
        <property name="maxThreads" value="200"/><!-- 仅在异步方式有效, 同步无限制 -->
        <property name="maxThreadsPerHost" value="200"/><!-- 仅在异步方式有效, 同步无限制 -->
        <property name="passiveBlockDuration" value="30000"/><!-- 被动阻断时间建议与所有超时时间加起来接近 -->
        <property name="connectTimeout" value="3000"/>
        <property name="writeTimeout" value="10000"/>
        <property name="readTimeout" value="10000"/>
        <property name="verboseLog" value="true"/>
        <!--<property name="verboseLogConfig" value="0x00000110"/>-->
        <!--<property name="dataConverter" ref="dataConverter"/> 默认提供GsonDataConverter-->
        <!--<property name="txTimerEnabled" value="true"/>-->
    </bean>
    
```

## 简化版配置(推荐)

* 在MultiHostOkHttpClient的基础上, 封装了LoadBalancedHostManager和LoadBalancedInspectManager, 简化了配置, 免去了配置三个Bean的麻烦
* 配置被简化, 如需高度定制, 请使用LoadBalancedHostManager + LoadBalancedInspectManager + MultiHostOkHttpClient
* 内置的LoadBalancedInspectManager采用TELNET方式探测后端<br>
* 屏蔽了setHostManager()方法, 调用会抛出异常<br>
* 实现了DisposableBean, 在Spring容器中会自动销毁<br>

```text
    <bean id="simpleOkHttpClient" class="sviolet.slate.common.modelx.loadbalance.classic.SimpleOkHttpClient">
        <property name="hosts" value="http://127.0.0.1:8081,http://127.0.0.1:8082"/>
        <property name="initiativeInspectInterval" value="5000"/>
        <property name="maxIdleConnections" value="20"/>
        <property name="maxThreads" value="200"/><!-- 仅在异步方式有效, 同步无限制 -->
        <property name="maxThreadsPerHost" value="200"/><!-- 仅在异步方式有效, 同步无限制 -->
        <property name="passiveBlockDuration" value="30000"/><!-- 被动阻断时间建议与所有超时时间加起来接近 -->
        <property name="connectTimeout" value="3000"/>
        <property name="writeTimeout" value="10000"/>
        <property name="readTimeout" value="10000"/>
        <property name="verboseLog" value="true"/>
        <!--<property name="verboseLogConfig" value="0x00000110"/>-->
        <!--<property name="dataConverter" ref="dataConverter"/> 默认提供GsonDataConverter-->
        <!--<property name="httpGetInspector" ref="/health"/> 将主动探测方式从telnet改为http get方式-->
        <!--<property name="txTimerEnabled" value="true"/>-->
    </bean>
```

### 使用Apollo配置中心实时调整配置

* Apollo配置中心新版本能实时更新XML属性和@Value注解中的${...}参数
* 因此只需要按如下方式使用, 即可根据Apollo实时调整配置

```text

    <!-- 启用apollo(任意一个XML中声明过一次即可) -->
    <apollo:config/>

    <!-- 使用${...}应用apollo参数 -->
    <bean id="simpleOkHttpClient" class="sviolet.slate.common.modelx.loadbalance.classic.SimpleOkHttpClient">
        <property name="hosts" value="${http.client.hosts}"/>
        ......
    </bean>
```
