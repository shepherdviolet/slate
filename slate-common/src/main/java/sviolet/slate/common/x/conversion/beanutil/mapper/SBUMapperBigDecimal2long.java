package sviolet.slate.common.x.conversion.beanutil.mapper;

import sviolet.slate.common.x.conversion.beanutil.PropMapper;

import java.math.BigDecimal;

public class SBUMapperBigDecimal2long implements PropMapper {

    @Override
    public Class<?> fromType() {
        return BigDecimal.class;
    }

    @Override
    public Class<?> toType() {
        return long.class;
    }

    @Override
    public Object map(Object from) {
        return ((BigDecimal)from).longValue();
    }

}
