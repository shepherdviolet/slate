package sviolet.slate.common.x.conversion.beanutil.safe.num;

import org.slf4j.Logger;
import sviolet.slate.common.x.conversion.beanutil.PropMapper;

import java.math.BigDecimal;

public class SBUMapperAllNumber2BigDecimal implements PropMapper {

    private static final Class[] FROM = new Class[]{
            Short.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
    };

    private static final Class[] TO = new Class[]{
            BigDecimal.class,
    };

    @Override
    public Object map(Object from, Class<?> toType, Logger logger, boolean logEnabled) {
        return new BigDecimal(String.valueOf(from));
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
