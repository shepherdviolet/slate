# SlateBeanUtils JavaBean转换工具

* `Maven/Gradle依赖配置`在本文最后

## 功能

* Bean转Bean
* Bean转Map
* Map转Bean

## 特点

* 线程安全, 内置缓存提高性能
* 内部采用CGLIB的BeanCopier和BeanMap实现

<br>
<br>
<br>

# 用法

## Bean转Bean

* 浅复制, 只复制Bean和Map的第一层参数
* 参数类型不匹配时一般不会抛出异常, 会跳过不匹配的参数(参数留空)
* 内置类型转换器, 当类型不匹配时会尝试转换, 可使用Glaciion SPI扩展
* throws MappingRuntimeException 异常概率:低, 触发原因: 拷贝器创建失败 / 拷贝过程出错, 可使用getFromType/getToType/getFieldName方法获得出问题的类型和参数名
* throws ObjenesisException 异常概率:低, 触发原因: 目标Bean实例化失败

```text
        From from = new From();
        To to = SlateBeanUtils.beanToBean(from, To.class);
```

```text
        From from = new From();
        To to = new To();
        SlateBeanUtils.beanToBean(from, to);
```

## Bean转Map

* 浅复制, 只复制Bean和Map的第一层参数
* 一般不会抛出异常
* 无内置类型转换器, 因为Bean转Map不存在类型不匹配的情况
* throws MappingRuntimeException 异常概率:低, 触发原因: 映射器创建失败

```text
        Bean bean = new Bean();
        Map<String, Object> map = SlateBeanUtils.beanToMap(bean);
```

```text
        Bean bean = new Bean();
        Map<String, Object> map = new HashMap();
        SlateBeanUtils.beanToMap(bean, map);
```

## Map转Bean

* 浅复制, 只复制Bean和Map的第一层参数
* 当Map中字段类型与Bean参数类型不匹配时会抛出异常(若设置throwExceptionIfFails为false, 则不会抛出异常, 失败的参数留空)
* 内置类型转换器, 当类型不匹配时会尝试转换, 可使用Glaciion SPI扩展
* `convert` true: 尝试转换参数类型使之符合要求, false: 不转换参数类型
* `throwExceptionIfFails` true: 如果参数的类型不匹配或转换失败, 则抛出异常, false: 如果参数的类型不匹配或转换失败, 不会抛出异常, 失败的参数留空
* throws MappingRuntimeException 异常概率:高, 触发原因: Map中字段类型与Bean参数类型不匹配(当throwExceptionIfFails=true) / 给目的Bean赋值时出错(当throwExceptionIfFails=true) / Bean映射器创建失败(无论throwExceptionIfFails为何值, 均抛异常)
* throws ObjenesisException 异常概率:低, 触发原因: 目标Bean实例化失败

```text
        Map<String, Object> map = new HashMap<>();
        Bean bean = SlateBeanUtils.mapToBean(map, Bean.class, true, true);
```

```text
        Map<String, Object> map = new HashMap<>();
        Bean bean = new Bean();
        SlateBeanUtils.mapToBean(map, bean, true, true);
```

## Bean转Map(递归深复制)

* 递归, 递归复制多层参数直到`不可分割`的类型(可以额外指定`不可分割`的类型)
* 一般不会抛出异常
* 无内置类型转换器, 因为Bean转Map不存在类型不匹配的情况
* `fromBean` 从这个Bean复制(必须是个Bean或Map, 无法复制List对象)
* throws MappingRuntimeException 异常概率:低, 触发原因: 映射器创建失败

```text
    Bean bean = new Bean();
    Map<String, Object> map = SlateBeanUtils.beanOrMapToMapRecursively(bean);
```

```text
    Map<Class<?>, IndivisibleJudge.JudgeType> extraIndivisibleTypes = new HashMap<>();
    //遇见BeanA时不再拆解
    extraIndivisibleTypes.put(BeanA.class, IndivisibleJudge.JudgeType.EQUALS);
    //遇见BeanB和它的子类时不再拆解
    extraIndivisibleTypes.put(BeanB.class, IndivisibleJudge.JudgeType.IS_ASSIGNABLE_FROM);
    Bean bean = new Bean();
    Map<String, Object> map = SlateBeanUtils.beanOrMapToMapRecursively(bean);
```

<br>
<br>
<br>

# Glaciion SPI扩展点1: 类型转换

* 以下示例均为精简案例, 更多高级用法详见文档:https://github.com/shepherdviolet/glaciion/blob/master/docs/guide.md

## 完全自定义实现类型转换逻辑(不推荐)

* 扩展点接口:sviolet.slate.common.x.conversion.beanutil.BeanConverter
* 采用这种方式, 会使默认类型转换逻辑失效, 使PropMapper扩展点失效

<br>

## 增加/删除类型转换器(推荐)

* 扩展点接口:sviolet.slate.common.x.conversion.beanutil.PropMapper

### 默认提供的转换器

* 默认提供的类型转换器已经`启用`无需声明
* 它们声明在slate-common包的`META-INF/glaciion/services/multiple/sviolet.slate.common.x.conversion.beanutil.PropMapper`中, 节选部分内容:

```text
+sviolet.slate.common.x.conversion.beanutil.safe.num.SBUMapperAllNumber2String
+sviolet.slate.common.x.conversion.beanutil.safe.num.SBUMapperAllNumber2BigDecimal
+sviolet.slate.common.x.conversion.beanutil.safe.num.SBUMapperAllInteger2BigInteger
+sviolet.slate.common.x.conversion.beanutil.safe.num.SBUMapperLowlevelNum2Double
+sviolet.slate.common.x.conversion.beanutil.safe.num.SBUMapperLowlevelNum2Float
+sviolet.slate.common.x.conversion.beanutil.safe.num.SBUMapperLowlevelNum2Long
+sviolet.slate.common.x.conversion.beanutil.safe.num.SBUMapperLowlevelNum2Integer
+sviolet.slate.common.x.conversion.beanutil.safe.date.SBUMapperAllDate2String
+sviolet.slate.common.x.conversion.beanutil.safe.date.SBUMapperAllDate2SqlDate
+sviolet.slate.common.x.conversion.beanutil.safe.date.SBUMapperAllDate2SqlTimestamp
+sviolet.slate.common.x.conversion.beanutil.safe.date.SBUMapperAllDate2UtilDate
+sviolet.slate.common.x.conversion.beanutil.safe.date.SBUMapperString2SqlDate
+sviolet.slate.common.x.conversion.beanutil.safe.date.SBUMapperString2SqlTimestamp
+sviolet.slate.common.x.conversion.beanutil.safe.date.SBUMapperString2UtilDate
......
```

### 调整日期格式(Date->String)

* Date转String默认格式为`yyyy-MM-dd HH:mm:ss.SSS`, 示例中改为`yyyy-MM-dd HH:mm:ss`
* 创建文件`META-INF/glaciion/properties/sviolet.slate.common.x.conversion.beanutil.safe.date.SBUMapperAllDate2String`
* 编辑文件:

```text
#如果有其他工程也创建了该配置文件, 则新增一个@priority参数, 使得@priority比其他工程的数值更大
#priority=1
dateFormat=yyyy-MM-dd HH:mm:ss
```

### 增加类型转换器

* 实现PropMapper接口

```text
package template.conversion.beanutil.num;

public class SBUMapperXxx2Xxx implements PropMapper {

    /**
     * 声明该转换器用于将如下类型转换成别的类型
     */
    private static final Class[] FROM = new Class[]{
            From.class
    };

    /**
     * 声明该转换器用于将FROM指定的类型转换成如下类型
     */
    private static final Class[] TO = new Class[]{
            To.class,
    };

    @Override
    public Class<?>[] fromType() {
        return FROM;
    }

    @Override
    public Class<?>[] toType() {
        return TO;
    }

    /**
     * 实现转换逻辑: 将from转换后返回
     * 若转换失败, 请抛出sviolet.slate.common.x.conversion.beanutil.MappingRuntimeException异常, 或简单地返回null
     */
    @Override
    public Object map(Object from, Class<?> toType) {
        //简单示例
        return String.valueOf(from);
        
        //异常处理示例, MappingRuntimeException异常的后三个参数有助于前端显示问题原因
        //try {
        //    ......
        //} catch (Exception e) {
        //    throw new MappingRuntimeException("Convert error", e, from.getClass().getName(), toType.getName(), null);
        //}
    }

}
```

* 声明插件
* 创建文件`META-INF/glaciion/services/multiple/sviolet.slate.common.x.conversion.beanutil.PropMapper`
* 编辑文件:

```text
+template.conversion.beanutil.num.SBUMapperXxx2Xxx
```

* 上述示例添加了一个类型转换器`template.conversion.beanutil.num.SBUMapperXxx2Xxx`

### 删除类型转换器

* 创建文件`META-INF/glaciion/services/multiple/sviolet.slate.common.x.conversion.beanutil.PropMapper`
* 编辑文件:

```text
-template.conversion.beanutil.num.SBUMapperXxx2Xxx
```

* 上述示例将实现类为`template.conversion.beanutil.num.SBUMapperXxx2Xxx`的类型转换器排除

### 关闭默认类型转换器的日志

#### 方法1

* 使用SLF4J的机制关闭, 包路径`sviolet.slate.common.x.conversion.beanutil`

#### 方法2

* 创建文件`META-INF/glaciion/properties/sviolet.slate.common.x.conversion.beanutil.DefaultBeanConverter`
* 编辑文件:

```text
#如果有其他工程也创建了该配置文件, 则新增一个@priority参数, 使得@priority比其他工程的数值更大
#priority=1
logEnabled=false
```

#### 方法3

* 添加启动参数`-Dslate.beanutils.log=false`

<br>
<br>
<br>

# Glaciion SPI扩展点2: 不可分割类型判断

* 以下示例均为精简案例, 更多高级用法详见文档:https://github.com/shepherdviolet/glaciion/blob/master/docs/guide.md

## 完全自定义实现不可分割类型判断器(不推荐)

* 扩展点接口:sviolet.slate.common.x.conversion.beanutil.IndivisibleJudge

<br>

## 修改默认的不可分割类型

* 创建文件`META-INF/glaciion/properties/sviolet.slate.common.x.conversion.beanutil.DefaultIndivisibleJudge`, 并编辑
* 编辑文件: 

```text
#如果有其他工程也创建了该配置文件, 则新增一个@priority参数, 使得@priority比其他工程的数值更大
#priority=1
types=sample.BeanA,sample.BeanB
typeGroups=sample.BeanC,sample.BeanD
```

* 这样就设置了BeanA/BeanB(不包含子类)和BeanC/BeanD(包含子类)为不可分割类型

<br>
<br>
<br>

# 依赖

* gradle

```text
//version替换为具体版本
dependencies {
    compile 'com.github.shepherdviolet:slate-common:version'
}
```

* maven

```maven
    <!--version替换为具体版本-->
    <dependency>
        <groupId>com.github.shepherdviolet</groupId>
        <artifactId>slate-common</artifactId>
        <version>version</version>
    </dependency>
```
