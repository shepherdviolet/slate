package sviolet.slate.common.x.conversion.beanutil;

/**
 * <p>SlateBeanUtils Bean工具 扩展点</p>
 *
 * <p>DefaultBeanConverter的扩展点, 在BeanConverter未被替换实现时有效</p>
 *
 * <p>实现:指定接受的源类型和目的类型</p>
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
    Object map(Object from, Class<?> toType);

    /**
     * @return 处理的源类型
     */
    Class<?>[] fromType();

    /**
     * @return 处理的目的类型
     */
    Class<?>[] toType();

}
