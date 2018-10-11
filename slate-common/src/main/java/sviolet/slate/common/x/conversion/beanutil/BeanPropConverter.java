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
            return convert(from, toType);
        } catch (MappingException e) {
            //补上field名
            String fieldName = BeanMethodNameUtils.methodToField(String.valueOf(setMethodName));
            e.setField(fieldName);
            throw e;
        }
    }

    /**
     * 实现类型转换
     * @param from 参数
     * @param toType 目的类型
     * @return 转换后的参数
     */
    protected abstract Object convert(Object from, Class toType);

}
