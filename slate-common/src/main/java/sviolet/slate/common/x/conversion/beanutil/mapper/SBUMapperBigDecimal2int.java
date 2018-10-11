package sviolet.slate.common.x.conversion.beanutil.mapper;

import sviolet.slate.common.x.conversion.beanutil.PropMapper;

import java.math.BigDecimal;

public class SBUMapperBigDecimal2int implements PropMapper {

    @Override
    public Class<?> fromType() {
        return BigDecimal.class;
    }

    @Override
    public Class<?> toType() {
        return int.class;
    }

    @Override
    public Object map(Object from) {
        return ((BigDecimal)from).intValue();
    }

}
