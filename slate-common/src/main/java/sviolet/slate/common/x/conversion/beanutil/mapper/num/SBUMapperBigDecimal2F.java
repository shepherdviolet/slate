package sviolet.slate.common.x.conversion.beanutil.mapper.num;

import sviolet.slate.common.x.conversion.beanutil.PropMapper;

import java.math.BigDecimal;

public class SBUMapperBigDecimal2F implements PropMapper {

    @Override
    public Class<?> fromType() {
        return BigDecimal.class;
    }

    @Override
    public Class<?> toType() {
        return float.class;
    }

    @Override
    public Object map(Object from) {
        return ((BigDecimal)from).floatValue();
    }

}
