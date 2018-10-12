package sviolet.slate.common.x.conversion.beanutil.unsafe.num;

import sviolet.slate.common.x.conversion.beanutil.PropMapper;

import java.math.BigInteger;

public class SBUMapperBigInteger2Float implements PropMapper {

    private static final Class[] FROM = new Class[]{
            BigInteger.class,
    };

    private static final Class[] TO = new Class[]{
            float.class,
            Float.class,
    };

    @Override
    public Object map(Object from, Class<?> toType) {
        return ((BigInteger)from).floatValue();
    }

    @Override
    public Class<?>[] fromType() {
        return FROM;
    }

    @Override
    public Class<?>[] toType() {
        return TO;
    }

}