# Slate 20.0.1

* Comprehensive Java library for spring framework (Java8+)
* [Github Home](https://github.com/shepherdviolet/slate)
* [Search in Maven Central](https://search.maven.org/search?q=g:com.github.shepherdviolet.slate20)
* [PGP Key](http://pool.sks-keyservers.net/pks/lookup?op=vindex&fingerprint=on&search=0x90998B78AABD6E96)
* [Special thanks to JetBrains for the free open source license, it is very helpful for our project!](https://www.jetbrains.com/?from=slate)

# slate-common

> Core module of slate

* [InterfaceInstantiation | Interface instantiation tool, supporting proxy](https://github.com/shepherdviolet/slate/blob/master/docs/interfaceinst/guide.md)
* [SlateBeanUtils | Bean to Bean / Bean to Map Conversion tool](https://github.com/shepherdviolet/slate/blob/master/docs/beanutils/guide.md)
* [TxTimer | RT Statistic API](https://github.com/shepherdviolet/slate/blob/master/docs/txtimer/guide.md)
* [RocketMQ Helper | Subscribe message by annotation](https://github.com/shepherdviolet/slate/blob/master/docs/rocketmq/guide.md)
* [Sentinel Helper | Another way to config rules](https://github.com/shepherdviolet/slate/blob/master/docs/ezsentinel/guide.md)
* [Helpers | Helper of third party libraries](https://github.com/shepherdviolet/slate/tree/develop/slate-common/src/main/java/sviolet/slate/common/helper)
* [Various Utils | Various utils are here](https://github.com/shepherdviolet/slate/tree/develop/slate-common/src/main/java/sviolet/slate/common/util)

# slate-http-client

> Provides a solution for http client

* [MultiHostOkHttpClient | HTTP client supporting load balancing](https://github.com/shepherdviolet/slate/blob/master/docs/loadbalance/guide.md)

# Import dependencies from maven repository

```gradle

repositories {
    //Slate in mavenCentral
    mavenCentral()
}
dependencies {
    // slate-common
    compile 'com.github.shepherdviolet.slate20:slate-common:version'
    // slate-http-client
    compile 'com.github.shepherdviolet.slate20:slate-http-client:version'
}

```

```maven
    <!-- slate-common -->
    <dependency>
        <groupId>com.github.shepherdviolet.slate20</groupId>
        <artifactId>slate-common</artifactId>
        <version>?</version>
    </dependency>
    <!-- slate-http-client -->
    <dependency>
        <groupId>com.github.shepherdviolet.slate20</groupId>
        <artifactId>slate-http-client</artifactId>
        <version>?</version>
    </dependency>
```
