# Slate 20.1.0

* Comprehensive Java library for spring framework (Java8+)
* [Github Home](https://github.com/shepherdviolet/slate)
* [Search in Maven Central](https://search.maven.org/search?q=g:com.github.shepherdviolet.slate20)
* [PGP Key](http://pool.sks-keyservers.net/pks/lookup?op=vindex&fingerprint=on&search=0x90998B78AABD6E96)
* [Special thanks to JetBrains for the free open source license, it is very helpful for our project!](https://www.jetbrains.com/?from=slate)

<br>
<br>

## Module 'slate-common'

[![Depends](https://img.shields.io/badge/Depends-thistle--common-dc143c.svg?style=flat)](https://github.com/shepherdviolet/thistle)
[![Depends](https://img.shields.io/badge/Depends-spring--context-dc143c.svg?style=flat)](https://search.maven.org/search?q=g:org.springframework%20a:spring-context)
[![Depends](https://img.shields.io/badge/Depends-slf4j--api-dc143c.svg?style=flat)](https://search.maven.org/search?q=g:org.slf4j%20a:slf4j-api)

> Core module of slate

| Auto Configurations |
| ------------------- |
| [SlateCommonAutoConfiguration](https://github.com/shepherdviolet/slate/tree/master/slate-common/src/main/java/sviolet/slate/common/springboot/autoconfig/SlateCommonAutoConfiguration.java) |

### Spring utils

* [InterfaceInstantiation : Instantiate interfaces into Spring context](https://github.com/shepherdviolet/slate/blob/master/docs/interfaceinst/guide.md)
* [MemberProcessor : Process all beans in Spring context (To implement custom injection / method binding...)](https://github.com/shepherdviolet/slate/tree/master/slate-common/src/main/java/sviolet/slate/common/x/bean/mbrproc)
* [YamlPropertySourceFactory : Load YAML by @PropertySource](https://github.com/shepherdviolet/slate/tree/master/slate-common/src/main/java/sviolet/slate/common/spring/property)
* [LambdaBuilder(Buildable) : New object in lambda way](https://github.com/shepherdviolet/slate/tree/master/slate-common/src/main/java/sviolet/slate/common/util/common)
* [...](https://github.com/shepherdviolet/slate/tree/master/slate-common/src/main/java/sviolet/slate/common/util)

### Helpers

* [DynamicDataSource : Dynamic datasource for Spring Boot](https://github.com/shepherdviolet/slate/tree/master/slate-common/src/main/java/sviolet/slate/common/helper/data/datasource/DynamicDataSource.java)
* [...](https://github.com/shepherdviolet/slate/tree/master/slate-common/src/main/java/sviolet/slate/common/helper)

<br>

## Module 'slate-txtimer'

[![Depends](https://img.shields.io/badge/Depends-slate--common-6a5acd.svg?style=flat)](https://github.com/shepherdviolet/slate)

> The module for statistic

### TxTimer

* [TxTimer : RT Statistic API](https://github.com/shepherdviolet/slate/blob/master/docs/txtimer/guide.md)

<br>

## Module 'slate-helper'

[![Depends](https://img.shields.io/badge/Depends-slate--common-6a5acd.svg?style=flat)](https://github.com/shepherdviolet/slate)
![Depends](https://img.shields.io/badge/Depends-...-dc143c.svg?style=flat)

> Helpers for third-party libraries

### Helpers

* [RocketMQ : Subscribe message by annotation](https://github.com/shepherdviolet/slate/blob/master/docs/rocketmq/guide.md)
* [Sentinel : Another way to config rules](https://github.com/shepherdviolet/slate/blob/master/docs/ezsentinel/guide.md)
* [Apollo : ApolloRefreshableProperties : The 'Properties' dynamically updated by Apollo](https://github.com/shepherdviolet/slate/tree/master/slate-helper/src/main/java/sviolet/slate/common/helper/apollo/ApolloRefreshableProperties.java)
* [Hessianlite : HessianLiteSerializeUtils : Serialize util with hessianlite](https://github.com/shepherdviolet/slate/tree/master/slate-helper/src/main/java/sviolet/slate/common/helper/hessianlite/HessianLiteSerializeUtils.java)
* [JetCache : SyncRedisLettuceCacheBuilder : Connect to redis in a synchronous manner for JetCache](https://github.com/shepherdviolet/slate/tree/master/slate-helper/src/main/java/sviolet/slate/common/helper/jetcache/lettuce/SyncRedisLettuceCacheBuilder.java)
* [...](https://github.com/shepherdviolet/slate/tree/master/slate-helper/src/main/java/sviolet/slate/common/helper)

<br>

## Module 'slate-http-client'

[![Depends](https://img.shields.io/badge/Depends-slate--common-6a5acd.svg?style=flat)](https://github.com/shepherdviolet/slate)
[![Depends](https://img.shields.io/badge/Depends-slate--txtimer-6a5acd.svg?style=flat)](https://github.com/shepherdviolet/slate)
[![Depends](https://img.shields.io/badge/Depends-okhttp-dc143c.svg?style=flat)](https://search.maven.org/search?q=g:com.squareup.okhttp3%20a:okhttp)

> Provides a solution for http client

### Http client

* [MultiHostOkHttpClient : A HTTP client supporting load balancing](https://github.com/shepherdviolet/slate/blob/master/docs/loadbalance/guide.md)

<br>

## Module 'slate-mapxbean'

[![Depends](https://img.shields.io/badge/Depends-slate--common-6a5acd.svg?style=flat)](https://github.com/shepherdviolet/slate)

> Map - Bean converter

### MapXBean

* MapXBean : Convert between Map and Bean

<br>
<br>

# Import dependencies from maven repository

```gradle

repositories {
    //Slate in mavenCentral
    mavenCentral()
}
dependencies {
    compile 'com.github.shepherdviolet.slate20:slate-common:?'
    compile 'com.github.shepherdviolet.slate20:slate-txtimer:?'
    compile 'com.github.shepherdviolet.slate20:slate-helper:?'
    compile 'com.github.shepherdviolet.slate20:slate-http-client:?'
    compile 'com.github.shepherdviolet.slate20:slate-mapxbean:?'
}

```

```maven
    <dependency>
        <groupId>com.github.shepherdviolet.slate20</groupId>
        <artifactId>slate-common</artifactId>
        <version>?</version>
    </dependency>
    <dependency>
        <groupId>com.github.shepherdviolet.slate20</groupId>
        <artifactId>slate-txtimer</artifactId>
        <version>?</version>
    </dependency>
    <dependency>
        <groupId>com.github.shepherdviolet.slate20</groupId>
        <artifactId>slate-helper</artifactId>
        <version>?</version>
    </dependency>
    <dependency>
        <groupId>com.github.shepherdviolet.slate20</groupId>
        <artifactId>slate-http-client</artifactId>
        <version>?</version>
    </dependency>
    <dependency>
        <groupId>com.github.shepherdviolet.slate20</groupId>
        <artifactId>slate-mapxbean</artifactId>
        <version>?</version>
    </dependency>
```
