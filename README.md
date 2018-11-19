# Slate 11.4
* Comprehensive Java library for spring framework (Java7+)
* https://github.com/shepherdviolet/slate

# Contents

* [MultiHostOkHttpClient | HTTP client supporting load balancing](https://github.com/shepherdviolet/slate/blob/master/docs/loadbalance/guide.md)
* [InterfaceInstantiation | Interface instantiation tool, supporting proxy](https://github.com/shepherdviolet/slate/blob/master/docs/interfaceinst/guide.md)
* [SlateBeanUtils | Bean to Bean / Bean to Map Convertion tool](https://github.com/shepherdviolet/slate/blob/master/docs/beanutils/guide.md)
* [TxTimer | Simple transaction elapse statistics tool](https://github.com/shepherdviolet/slate/blob/master/docs/txtimer/guide.md)
* [Utils | Other utils are here](https://github.com/shepherdviolet/slate/tree/develop/slate-common/src/main/java/sviolet/slate/common/util)
* SLF4J logger support for [ThistleSpi](https://github.com/shepherdviolet/thistle)

# Import dependencies from maven repository

```gradle

repositories {
    //Slate in mavenCentral
    mavenCentral()
}
dependencies {
    // For spring boot
    compile 'com.github.shepherdviolet:slate-springboot:version'
    // For spring
    compile 'com.github.shepherdviolet:slate-common:version'
}

```

```maven
    <!-- for spring boot -->
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-springboot</artifactId>
        <version>version</version>
    </dependency>
    <!-- for spring -->
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-common</artifactId>
        <version>version</version>
    </dependency>
```

* [Dependencies exclusion](https://github.com/shepherdviolet/slate/blob/master/docs/dependencies-exclusion.md)
