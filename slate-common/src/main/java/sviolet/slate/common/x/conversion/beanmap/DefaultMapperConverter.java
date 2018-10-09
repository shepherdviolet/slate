package sviolet.slate.common.x.conversion.beanmap;

public class DefaultMapperConverter implements MapperConverter {

    @Override
    public Object convert(Object from, Class toType, Object setMethodName) {
        return from;
    }

}
