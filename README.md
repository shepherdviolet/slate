# slate-common 7.3
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
    compile 'com.github.shepherdviolet:slate-common:7.3'
}

```

```maven
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-common</artifactId>
        <version>7.3</version>
    </dependency>
```

# How to exclude dependencies (optional)

```gradle
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        compile ('com.github.shepherdviolet:slate-common:7.3') {
            exclude group:'com.jcraft', module:'jsch'
            exclude group:'com.squareup.okhttp3'
        }
    }
```

```maven
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-common</artifactId>
        <version>7.3</version>
        <exclusions>
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