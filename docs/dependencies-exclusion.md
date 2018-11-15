# How to exclude dependencies (optional)

```gradle
    repositories {
    	//Slate in mavenCentral
        mavenCentral()
    }
    dependencies {
        compile ('com.github.shepherdviolet:slate-common:version') {
            exclude group:'com.google.code.gson'
            exclude group:'org.bouncycastle'
            exclude group:'org.springframework'
            exclude group:'javax.servlet'
            exclude group:'com.squareup.okhttp3'
            exclude group:'com.jcraft', module:'jsch'
            exclude group:'redis.clients'
        }
        compile ('com.github.shepherdviolet:slate-springboot:version') {
            exclude group:'org.springframework.boot'
            exclude group:'javax.servlet'
        }
    }
```

```maven
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-common</artifactId>
        <version>version</version>
        <exclusions>
             <exclusion>
                 <groupId>com.google.code.gson</groupId>
                 <artifactId>*</artifactId>
             </exclusion>
             <exclusion>
                 <groupId>org.bouncycastle</groupId>
                 <artifactId>*</artifactId>
             </exclusion>
             <exclusion>
                 <groupId>org.springframework</groupId>
                 <artifactId>*</artifactId>
             </exclusion>
             <exclusion>
                 <groupId>javax.servlet</groupId>
                 <artifactId>*</artifactId>
             </exclusion>
             <exclusion>
                 <groupId>com.squareup.okhttp3</groupId>
                 <artifactId>*</artifactId>
             </exclusion>
             <exclusion>
                 <groupId>com.jcraft</groupId>
                 <artifactId>jsch</artifactId>
             </exclusion>
             <exclusion>
                 <groupId>redis.clients</groupId>
                 <artifactId>*</artifactId>
             </exclusion>
        </exclusions>
    </dependency>
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-springboot</artifactId>
        <version>version</version>
        <exclusions>
             <exclusion>
                 <groupId>org.springframework.boot</groupId>
                 <artifactId>*</artifactId>
             </exclusion>
             <exclusion>
                 <groupId>javax.servlet</groupId>
                 <artifactId>*</artifactId>
             </exclusion>
        </exclusions>
    </dependency>
```

# How to exclude all dependencies (optional)

```gradle
    repositories {
    	//Slate in mavenCentral
        mavenCentral()
    }
    dependencies {
        compile ('com.github.shepherdviolet:slate-common:version') {
            transitive = false
        }
        compile ('com.github.shepherdviolet:slate-springboot:version') {
            transitive = false
        }
    }
```

```maven
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-common</artifactId>
        <version>version</version>
        <exclusions>
             <exclusion>
                 <groupId>*</groupId>
                 <artifactId>*</artifactId>
             </exclusion>
        </exclusions>
    </dependency>
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-springboot</artifactId>
        <version>version</version>
        <exclusions>
             <exclusion>
                 <groupId>*</groupId>
                 <artifactId>*</artifactId>
             </exclusion>
    </dependency>
```

