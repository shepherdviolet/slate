package sviolet.slate.common.x.conversion.beanutil.mapper.num;

import sviolet.slate.common.x.conversion.beanutil.PropMapper;

import java.math.BigDecimal;

public class SBUMapperBigDecimal2Integer implements PropMapper {

    @Override
    public Class<?> fromType() {
        return BigDecimal.class;
    }

    @Override
    public Class<?> toType() {
        return Integer.class;
    }

    @Override
    public Object map(Object from) {
        return ((BigDecimal)from).intValue();
    }

}
