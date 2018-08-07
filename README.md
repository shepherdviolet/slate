# slate 9.11
* JavaEE common library
* https://github.com/shepherdviolet/slate

### Import dependencies from maven repository

```gradle

repositories {
    //local repository
    //maven { url 'file:C:/m2repository/repository' }
    //maven central or jitpack.io
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
dependencies {
    compile 'com.github.shepherdviolet:slate-common:9.11'
    compile 'com.github.shepherdviolet:slate-springboot:9.11'
}

```

```maven
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-common</artifactId>
        <version>9.11</version>
    </dependency>
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-springboot</artifactId>
        <version>9.11</version>
    </dependency>
```

# How to exclude dependencies (optional)

```gradle
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        compile ('com.github.shepherdviolet:slate-common:9.11') {
            exclude group:'com.jcraft', module:'jsch'
            exclude group:'com.squareup.okhttp3'
            exclude group:'org.jetbrains.kotlin', module:'kotlin-stdlib-jre7'
            exclude group:'com.google.code.gson'
            exclude group:'org.bouncycastle'
            exclude group:'redis.clients'
            exclude group:'org.springframework'
        }
        compile ('com.github.shepherdviolet:slate-common:9.11') {
            exclude group:'org.springframework.boot'
        }
    }
```

```maven
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-common</artifactId>
        <version>9.10</version>
        <exclusions>
             <exclusion>
                 <groupId>com.jcraft</groupId>
                 <artifactId>jsch</artifactId>
             </exclusion>
             <exclusion>
                 <groupId>com.squareup.okhttp3</groupId>
                 <artifactId>*</artifactId>
             </exclusion>
             <exclusion>
                 <groupId>org.jetbrains.kotlin</groupId>
                 <artifactId>kotlin-stdlib-jre7</artifactId>
             </exclusion>
             <exclusion>
                 <groupId>com.google.code.gson</groupId>
                 <artifactId>*</artifactId>
             </exclusion>
             <exclusion>
                 <groupId>org.bouncycastle</groupId>
                 <artifactId>*</artifactId>
             </exclusion>
             <exclusion>
                 <groupId>redis.clients</groupId>
                 <artifactId>*</artifactId>
             </exclusion>
             <exclusion>
                 <groupId>org.springframework</groupId>
                 <artifactId>*</artifactId>
             </exclusion>
        </exclusions>
    </dependency>
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-common</artifactId>
        <version>9.10</version>
        <exclusions>
             <exclusion>
                 <groupId>org.springframework.boot</groupId>
                 <artifactId>*</artifactId>
             </exclusion>
        </exclusions>
    </dependency>
```

# Register SlateServletContextListener (optional)

* Spring MVC: Register in web.xml

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

* Spring Boot: Register in class

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
