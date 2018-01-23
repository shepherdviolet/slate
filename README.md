# slate-common 7.6
* JavaEE common library for private use
* https://github.com/shepherdviolet/slate-common

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
    compile 'com.github.shepherdviolet:slate-common:7.6'
}

```

```maven
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-common</artifactId>
        <version>7.6</version>
    </dependency>
```

# How to exclude dependencies (optional)

```gradle
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        compile ('com.github.shepherdviolet:slate-common:7.6') {
            exclude group:'com.jcraft', module:'jsch'
            exclude group:'com.squareup.okhttp3'
        }
    }
```

```maven
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-common</artifactId>
        <version>7.6</version>
        <exclusions>
             <exclusion>
                 <groupId>javax.servlet</groupId>
                 <artifactId>javax.servlet-api</artifactId>
             </exclusion>
             <exclusion>
                 <groupId>com.jcraft</groupId>
                 <artifactId>jsch</artifactId>
             </exclusion>
             <exclusion>
                 <groupId>com.squareup.okhttp3</groupId>
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
          <listener-class>sviolet.slate.common.helperx.servlet.SlateServletContextListener</listener-class>
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
