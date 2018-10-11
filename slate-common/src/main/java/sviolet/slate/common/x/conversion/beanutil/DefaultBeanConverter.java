package sviolet.slate.common.x.conversion.beanutil;

/**
 * <p>默认Bean参数类型转换器</p>
 *
 * @author S.Violet
 */
public class DefaultBeanConverter extends BeanConverter {

    @Override
    protected Object onConvert(Type type, Object from, Class... toTypes) {
        return from;
    }

}
