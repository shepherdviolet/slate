package sviolet.slate.common.x.conversion.beanutil.safe.num;

import sviolet.slate.common.x.conversion.beanutil.PropMapper;

import java.math.BigInteger;

public class SBUMapperAllInteger2BigInteger implements PropMapper {

    private static final Class[] FROM = new Class[]{
            short.class,
            Short.class,
            int.class,
            Integer.class,
            long.class,
            Long.class,
    };

    private static final Class[] TO = new Class[]{
            BigInteger.class,
    };

    @Override
    public Object map(Object from, Class<?> toType) {
        return new BigInteger(String.valueOf(from));
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
