# slate 10.0
* JavaEE common library
* https://github.com/shepherdviolet/slate

### Import dependencies from maven repository

```gradle

repositories {
    //Slate in mavenCentral
    mavenCentral()
}
dependencies {
    // For spring boot
    compile 'com.github.shepherdviolet:slate-springboot:10.0'
    // For others
    compile 'com.github.shepherdviolet:slate-common:10.0'
}

```

```maven
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-springboot</artifactId>
        <version>10.0</version>
    </dependency>
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-common</artifactId>
        <version>10.0</version>
    </dependency>
```

* [Dependencies exclusion](https://github.com/shepherdviolet/slate/blob/master/docs/dependencies-exclusion.md)

# Register SlateServletContextListener (optional)

### Spring Boot: Automatically

* It will be registered automatically, if you add dependency `com.github.shepherdviolet:slate-springboot:version`

> compile `com.github.shepherdviolet:slate-springboot:version`

### Spring Boot: Register in class manually

```gradle
    @Configuration
    public class AppConf {
        @Bean
        public ServletContextListener slateServletContextListener() {
            return new SlateServletContextListener();
        }
        ......
    }
```

### Spring MVC: Register in web.xml manually

```gradle
    <web-app ......>
        <listener>
            <listener-class>sviolet.slate.common.util.common.SlateServletContextListener</listener-class>
        </listener>
        <listener>
            ......
        </listener>
        ......
    </web-app>
```

# Contents

* [MultiHostOkHttpClient](https://github.com/shepherdviolet/slate/blob/master/docs/loadbalance/guide.md)
* [InterfaceInstantiation](https://github.com/shepherdviolet/slate/tree/develop/slate-common/src/main/java/sviolet/slate/common/utilx/interfaceinst)
* [Utils](https://github.com/shepherdviolet/slate/tree/develop/slate-common/src/main/java/sviolet/slate/common/util)

# Support for thistle

* ThistleSpi slf4j logger support
