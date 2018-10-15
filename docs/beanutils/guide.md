# SlateBeanUtils JavaBean转换工具

## 功能

* Bean转Bean
* Bean转Map
* Map转Bean

## 特点

* 线程安全, 内置缓存提高性能
* 浅复制, 只复制Bean和Map的第一层参数
* 内部采用CGLIB的BeanCopier和BeanMap实现
* Bean转Bean Map转Bean 提供内置类型转换器, 处理类型不匹配的问题

# 用法

## Bean转Bean

```text
        From from = new From();
        To to = SlateBeanUtils.copy(from, To.class);
```

```text
        From from = new From();
        To to = new To();
        SlateBeanUtils.copy(from, to);
```

* 注意: copy方法较容易抛出MappingRuntimeException异常, 有需要可以捕获异常, 并用getFromType/getToType/getFieldName方法获得出问题的类型和参数名

## Bean转Map

```text
        Bean bean = new Bean();
        Map<String, Object> map = SlateBeanUtils.toMap(bean);
```

```text
        Bean bean = new Bean();
        Map<String, Object> map = new HashMap();
        SlateBeanUtils.toMap(bean, map);
```

* toMap不容易抛出MappingRuntimeException异常

## Map转Bean

```text
        Map<String, Object> map = new HashMap<>();
        Bean bean = SlateBeanUtils.fromMap(map, Bean.class, true);
```

```text
        Map<String, Object> map = new HashMap<>();
        Bean bean = new Bean();
        SlateBeanUtils.fromMap(map, bean, true);
```

* 注意: fromMap方法较容易抛出MappingRuntimeException异常, 有需要可以捕获异常, 并用getFromType/getToType/getFieldName方法获得出问题的类型和参数名

# ThistleSpi扩展点

* 使用扩展点之前, 请先仔细阅读文档: https://github.com/shepherdviolet/thistle/blob/master/docs/thistlespi/guide.md

## 完全自定义实现类型转换逻辑(不推荐)

* 扩展点接口:sviolet.slate.common.x.conversion.beanutil.BeanConverter
* 采用这种方式, 会使默认类型转换逻辑失效, 使PropMapper扩展点失效

## 增加/删除类型转换器(推荐)

* 扩展点接口:sviolet.slate.common.x.conversion.beanutil.PropMapper

### 默认提供的转换器

* 默认提供的类型转换器优先级为100000-199999, 默认`启用`无需声明
* 其声明在slate-common包的`META-INF/thistle-spi/plugin.properties`中, 节选部分内容:

```text
# SlateBeanUtils: bean property mappers: safe num
sviolet.slate.common.x.conversion.beanutil.PropMapper>101001=sviolet.slate.common.x.conversion.beanutil.safe.num.SBUMapperAllNumber2String
sviolet.slate.common.x.conversion.beanutil.PropMapper>101002=sviolet.slate.common.x.conversion.beanutil.safe.num.SBUMapperAllNumber2BigDecimal
sviolet.slate.common.x.conversion.beanutil.PropMapper>101003=sviolet.slate.common.x.conversion.beanutil.safe.num.SBUMapperAllInteger2BigInteger
sviolet.slate.common.x.conversion.beanutil.PropMapper>101004=sviolet.slate.common.x.conversion.beanutil.safe.num.SBUMapperLowlevelNum2Double
sviolet.slate.common.x.conversion.beanutil.PropMapper>101005=sviolet.slate.common.x.conversion.beanutil.safe.num.SBUMapperLowlevelNum2Float
sviolet.slate.common.x.conversion.beanutil.PropMapper>101006=sviolet.slate.common.x.conversion.beanutil.safe.num.SBUMapperLowlevelNum2Long
sviolet.slate.common.x.conversion.beanutil.PropMapper>101007=sviolet.slate.common.x.conversion.beanutil.safe.num.SBUMapperLowlevelNum2Integer
......

# SlateBeanUtils: bean property mappers: safe date
sviolet.slate.common.x.conversion.beanutil.PropMapper>102001=sviolet.slate.common.x.conversion.beanutil.safe.date.SBUMapperUtilDate2String(yyyy-MM-dd HH:mm:ss.SSS)
......
```

* 另外优先级200000-299999为`不安全`的类型转换器保留, 默认`不启用`, 见包路径`sviolet.slate.common.x.conversion.beanutil.unsafe`
* 如需启用, 请自行声明在`META-INF/thistle-spi/plugin.properties`中, 示例如下:

```text
# SlateBeanUtils: bean property mappers: unsafe num
sviolet.slate.common.x.conversion.beanutil.PropMapper>201001=sviolet.slate.common.x.conversion.beanutil.unsafe.num.SBUMapperBigInteger2Integer
sviolet.slate.common.x.conversion.beanutil.PropMapper>201002=sviolet.slate.common.x.conversion.beanutil.unsafe.num.SBUMapperBigInteger2Long
sviolet.slate.common.x.conversion.beanutil.PropMapper>201003=sviolet.slate.common.x.conversion.beanutil.unsafe.num.SBUMapperBigInteger2Float
sviolet.slate.common.x.conversion.beanutil.PropMapper>201004=sviolet.slate.common.x.conversion.beanutil.unsafe.num.SBUMapperBigInteger2Double
sviolet.slate.common.x.conversion.beanutil.PropMapper>201005=sviolet.slate.common.x.conversion.beanutil.unsafe.num.SBUMapperBigInteger2Short
sviolet.slate.common.x.conversion.beanutil.PropMapper>201006=sviolet.slate.common.x.conversion.beanutil.unsafe.num.SBUMapperBigDecimal2Float
sviolet.slate.common.x.conversion.beanutil.PropMapper>201007=sviolet.slate.common.x.conversion.beanutil.unsafe.num.SBUMapperBigDecimal2Double
```

* 优先级数字越小, 优先级越高. 遇到冲突时, 高优先级的插件生效.
* 如果自定义实现的类型转换器需要覆盖默认实现, 优先级请小于100000
* 同一个配置文件中, 优先级不要重复!!!

### 调整日期格式(Date->String)

* Date转String默认格式为`yyyy-MM-dd HH:mm:ss.SSS`, 示例中改为`yyyy-MM-dd HH:mm:ss`
* 创建文件`META-INF/thistle-spi/plugin.properties`
* 编辑文件:

```text
sviolet.slate.common.x.conversion.beanutil.PropMapper>2001=sviolet.slate.common.x.conversion.beanutil.safe.date.SBUMapperUtilDate2String(yyyy-MM-dd HH:mm:ss)
```

* 注意优先级(示例中为`2001`)在同一个配置文件中不能重复, 且要比100000小(用来覆盖默认实现)

### 增加类型转换器

* 实现PropMapper接口
* 以默认转换器SBUMapperAllNumber2String为例

```text
public class SBUMapperAllNumber2String implements PropMapper {

    /**
     * 声明该转换器用于将如下类型转换成别的类型
     */
    private static final Class[] FROM = new Class[]{
            short.class,
            Short.class,
            int.class,
            Integer.class,
            long.class,
            Long.class,
            float.class,
            Float.class,
            double.class,
            Double.class,
            BigInteger.class,
            BigDecimal.class,
    };

    /**
     * 声明该转换器用于将FROM指定的类型转换成如下类型
     */
    private static final Class[] TO = new Class[]{
            String.class,
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
* 创建文件`META-INF/thistle-spi/plugin.properties`
* 编辑文件:

```text
sviolet.slate.common.x.conversion.beanutil.PropMapper>1001=template.conversion.beanutil.num.SBUMapperXxx2Xxx
```

* 上述示例新增了一个类型转换器, 优先级1001, 若与默认转换器冲突, 该自定义转换器生效

### 删除类型转换器

* 创建文件`META-INF/thistle-spi/plugin-ignore.properties`
* 编辑文件:

```text
sviolet.slate.common.x.conversion.beanutil.PropMapper=template.conversion.beanutil.num.SBUMapperXxx2Xxx
```

* 上述示例将实现类为`template.conversion.beanutil.num.SBUMapperXxx2Xxx`的类型转换器排除

### 关闭默认类型转换器的日志

#### 方法1

* 使用SLF4J的机制关闭, 包路径`sviolet.slate.common.x.conversion.beanutil`

#### 方法2

* 创建文件`META-INF/thistle-spi/service.properties`
* 编辑文件:

```text
sviolet.slate.common.x.conversion.beanutil.BeanConverter>yourapp>application=sviolet.slate.common.x.conversion.beanutil.DefaultBeanConverter(logDisabled)
```

* 其中, 服务优先级(示例中的application)的选用, 请参考ThistleSpi文档, https://github.com/shepherdviolet/thistle/blob/master/docs/thistlespi/guide.md#%E5%A3%B0%E6%98%8E%E6%9C%8D%E5%8A%A1%E7%9A%84%E5%AE%9E%E7%8E%B0
