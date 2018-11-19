# How to exclude dependencies (optional)

```gradle
    repositories {
    	//Slate in mavenCentral
        mavenCentral()
    }
    dependencies {
        compile ('com.github.shepherdviolet:slate-common:version') {
        }
        compile ('com.github.shepherdviolet:slate-http-client:version') {
            exclude group:'com.google.code.gson'
            exclude group:'com.squareup.okhttp3'
        }
    }
```

```maven
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-common</artifactId>
        <version>version</version>
        <exclusions>
        </exclusions>
    </dependency>
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-http-client</artifactId>
        <version>version</version>
        <exclusions>
             <exclusion>
                 <groupId>com.google.code.gson</groupId>
                 <artifactId>*</artifactId>
             </exclusion>
             <exclusion>
                 <groupId>com.squareup.okhttp3</groupId>
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
        compile ('com.github.shepherdviolet:slate-http-client:version') {
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
        <artifactId>slate-http-client</artifactId>
        <version>version</version>
        <exclusions>
             <exclusion>
                 <groupId>*</groupId>
                 <artifactId>*</artifactId>
             </exclusion>
    </dependency>
```

