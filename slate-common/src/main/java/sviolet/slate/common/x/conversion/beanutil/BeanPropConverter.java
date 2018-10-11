package sviolet.slate.common.x.conversion.beanutil;

import org.springframework.cglib.core.Converter;
import sviolet.thistle.util.conversion.BeanMethodNameUtils;

/**
 * <p>ThistleSpi扩展接口</p>
 *
 * <p>Bean参数类型转换器</p>
 *
 * @author S.Violet
 */
public abstract class BeanPropConverter implements Converter {

    @Override
    public final Object convert(Object from, Class toType, Object setMethodName) {
        try {
            return onConvert(Type.COPY, from, new Class[]{toType});
        } catch (MappingRuntimeException e) {
            //补上field名
            String fieldName = BeanMethodNameUtils.methodToField(String.valueOf(setMethodName));
            e.setFieldName(fieldName);
            throw e;
        }
    }

    /**
     * 实现类型转换
     * @param type 转换类型
     * @param from 待转换参数
     * @param toTypes 目的类型(符合其中的一个类型即可)
     * @return 转换后的参数
     */
    protected abstract Object onConvert(Type type, Object from, Class[] toTypes);

    public enum Type {
        COPY,
        BEANIZATION
    }

}
