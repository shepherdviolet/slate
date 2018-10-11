package sviolet.slate.common.x.conversion.beanutil.safe.num;

import sviolet.slate.common.x.conversion.beanutil.PropMapper;

import java.math.BigDecimal;

public class SBUMapperAllNumber2BigDecimal implements PropMapper {

    private static final Class[] FROM = new Class[]{
            int.class,
            Integer.class,
            long.class,
            Long.class,
            float.class,
            Float.class,
            double.class,
            Double.class,
            short.class,
            Short.class,
    };

    private static final Class[] TO = new Class[]{
            BigDecimal.class,
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
        return new BigDecimal(String.valueOf(from));
    }

}
