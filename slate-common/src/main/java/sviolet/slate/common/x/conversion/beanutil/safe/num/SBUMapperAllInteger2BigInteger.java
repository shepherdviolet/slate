package sviolet.slate.common.x.conversion.beanutil.safe.num;

import sviolet.slate.common.x.conversion.beanutil.PropMapper;

import java.math.BigInteger;

public class SBUMapperAllInteger2BigInteger implements PropMapper {

    private static final Class[] FROM = new Class[]{
            int.class,
            Integer.class,
            long.class,
            Long.class,
            short.class,
            Short.class,
    };

    private static final Class[] TO = new Class[]{
            BigInteger.class,
    };

    @Override
    public Class<?>[] fromType() {
        return FROM;
    }

    @Override
    public Class<?>[] toType() {
        return TO;
    }

    @Override
    public Object map(Object from, Class<?> toType) {
        return new BigInteger(String.valueOf(from));
    }

}
