package sviolet.slate.common.x.conversion.beanutil.unsafe.num;

import org.slf4j.Logger;
import sviolet.slate.common.x.conversion.beanutil.PropMapper;

import java.math.BigInteger;

public class SBUMapperBigInteger2Double implements PropMapper {

    private static final Class[] FROM = new Class[]{
            BigInteger.class,
    };

    private static final Class[] TO = new Class[]{
            Double.class,
    };

    @Override
    public Object map(Object from, Class<?> toType, Logger logger, boolean logEnabled) {
        return ((BigInteger)from).doubleValue();
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
