package sviolet.slate.common.x.conversion.beanutil.mapper.num;

import sviolet.slate.common.x.conversion.beanutil.PropMapper;

import java.math.BigDecimal;

public class SBUMapperBigDecimal2Integer implements PropMapper {

    private static final Class[] FROM = new Class[]{
            BigDecimal.class,
    };

    private static final Class[] TO = new Class[]{
            int.class,
            Integer.class,
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
        return ((BigDecimal)from).intValue();
    }

}
