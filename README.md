# slate-common 7.2
* JavaEE common library for private use
* https://github.com/shepherdviolet/slate-common

### Import dependencies from maven repository

```gradle

repositories {
    // maven central or jitpack.io
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
dependencies {
    compile 'com.github.shepherdviolet:slate-common:7.2'
}

```

### Import dependencies from local repository

```gradle

repositories {
    //replace by your path
    maven { url 'file:C:/m2repository/repository' }
}

```

# How to exclude dependencies (optional)

```gradle
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        compile ('com.github.shepherdviolet:slate-common:7.2') {
            exclude group:'com.jcraft', module:'jsch'
            exclude group:'com.squareup.okhttp3'
        }
    }
```