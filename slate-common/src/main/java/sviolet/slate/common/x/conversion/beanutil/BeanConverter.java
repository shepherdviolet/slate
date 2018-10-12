package sviolet.slate.common.x.conversion.beanutil;

import org.springframework.cglib.core.Converter;
import sviolet.thistle.util.conversion.BeanMethodNameUtils;

/**
 * <p>SlateBeanUtils Bean工具 扩展点</p>
 *
 * <p>Bean参数类型转换器</p>
 *
 * <p>实现:将对象类型转换为指定类型</p>
 *
 * @see SlateBeanUtils
 * @author S.Violet
 */
public abstract class BeanConverter implements Converter {

    @Override
    public final Object convert(Object from, Class toType, Object setMethodName) {
        try {
            return onConvert(Cause.COPY, from, new Class[]{toType});
        } catch (MappingRuntimeException e) {
            //补上field名
            String fieldName = BeanMethodNameUtils.methodToField(String.valueOf(setMethodName));
            e.setFieldName(fieldName);
            throw e;
        }
    }

    /**
     * 实现类型转换
     * @param cause 转换类型
     * @param from 待转换参数
     * @param toTypes 目的类型(符合其中的一个类型即可)
     * @return 转换后的参数, 转换失败返回null
     */
    protected abstract Object onConvert(Cause cause, Object from, Class[] toTypes);

    public enum Cause {
        /**
         * 由SlateBeanUtils.copy触发
         */
        COPY,

        /**
         * 由SlateBeanUtils.fromMap和mapBeanization触发
         */
        BEANIZATION
    }

}
