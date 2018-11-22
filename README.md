# Slate 12.2
* Comprehensive Java library for spring framework (Java7+)
* https://github.com/shepherdviolet/slate

# slate-common

> Core module of slate

* [InterfaceInstantiation | Interface instantiation tool, supporting proxy](https://github.com/shepherdviolet/slate/blob/master/docs/interfaceinst/guide.md)
* [SlateBeanUtils | Bean to Bean / Bean to Map Conversion tool](https://github.com/shepherdviolet/slate/blob/master/docs/beanutils/guide.md)
* [TxTimer | Simple transaction elapse statistics tool](https://github.com/shepherdviolet/slate/blob/master/docs/txtimer/guide.md)
* [Helpers | Helper of third party libraries](https://github.com/shepherdviolet/slate/tree/develop/slate-common/src/main/java/sviolet/slate/common/helper)
* [Various utils | Various utils are here](https://github.com/shepherdviolet/slate/tree/develop/slate-common/src/main/java/sviolet/slate/common/util)

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
    compile 'com.github.shepherdviolet:slate-common:version'
    // slate-http-client
    compile 'com.github.shepherdviolet:slate-http-client:version'
}

```

```maven
    <!-- slate-common -->
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-common</artifactId>
        <version>version</version>
    </dependency>
    <!-- slate-http-client -->
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-http-client</artifactId>
        <version>version</version>
    </dependency>
```

* [Dependencies exclusion](https://github.com/shepherdviolet/slate/blob/master/docs/dependencies-exclusion.md)
