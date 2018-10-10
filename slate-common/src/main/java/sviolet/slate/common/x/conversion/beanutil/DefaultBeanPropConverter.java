package sviolet.slate.common.x.conversion.beanutil;

public class DefaultBeanPropConverter implements BeanPropConverter {

    @Override
    public Object convert(Object from, Class toType, Object setMethodName) {
        return from;
    }

}
