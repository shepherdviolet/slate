package sviolet.slate.common.x.conversion.beanutil;

import org.slf4j.Logger;

/**
 * <p>SlateBeanUtils 默认参数类型转换器DefaultBeanConverter的扩展点, 在BeanConverter未被替换实现时有效</p>
 *
 * <p>实现:声明自身能处理什么类型的数据, 实现对应的类型转换逻辑</p>
 *
 * <p>使用扩展点之前, 请先仔细阅读文档: https://github.com/shepherdviolet/thistle/blob/master/docs/thistlespi/guide.md</p>
 *
 * @see DefaultBeanConverter
 * @see SlateBeanUtils
 * @author S.Violet
 */
public interface PropMapper {

    /**
     * 类型转换, 将源类型的数据转换成目的类型
     * @param from 源类型的对象
     * @param toType 目的类型
     * @return 目的类型的对象, 若转换失败, 可以抛出MappingRuntimeException异常或返回null
     */
    Object map(Object from, Class<?> toType, Logger logger, boolean logEnabled);

    /**
     * @return 处理的源类型
     */
    Class<?>[] fromType();

    /**
     * @return 处理的目的类型
     */
    Class<?>[] toType();

}
