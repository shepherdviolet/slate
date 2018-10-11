package sviolet.slate.common.x.conversion.beanutil.mapper.num;

import sviolet.slate.common.x.conversion.beanutil.PropMapper;

import java.math.BigDecimal;

public class SBUMapperBigDecimal2Long implements PropMapper {

    @Override
    public Class<?> fromType() {
        return BigDecimal.class;
    }

    @Override
    public Class<?> toType() {
        return Long.class;
    }

    @Override
    public Object map(Object from) {
        return ((BigDecimal)from).longValue();
    }

}
