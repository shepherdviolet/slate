# slate 9.14
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
    compile 'com.github.shepherdviolet:slate-springboot:9.14'
    // For others
    compile 'com.github.shepherdviolet:slate-common:9.14'
}

```

```maven
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-springboot</artifactId>
        <version>9.14</version>
    </dependency>
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-common</artifactId>
        <version>9.14</version>
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

# Manifest

* [MultiHostOkHttpClient](https://github.com/shepherdviolet/slate/blob/master/docs/loadbalance/guide.md)
* InterfaceInstantiation
