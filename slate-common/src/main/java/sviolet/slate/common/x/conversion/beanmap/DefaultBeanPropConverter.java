package sviolet.slate.common.x.conversion.beanmap;

public class DefaultBeanPropConverter implements BeanPropConverter {

    @Override
    public Object convert(Object from, Class toType, Object setMethodName) {
        return from;
    }

}
