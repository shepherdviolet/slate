package sviolet.slate.common.x.conversion.beanutil.safe.num;

import org.slf4j.Logger;
import sviolet.slate.common.x.conversion.beanutil.PropMapper;

public class SBUMapperLowlevelNum2Integer implements PropMapper {

    private static final Class[] FROM = new Class[]{
            Short.class,
    };

    private static final Class[] TO = new Class[]{
            Integer.class,
    };

    @Override
    public Object map(Object from, Class<?> toType, Logger logger, boolean logEnabled) {
        return Integer.valueOf(String.valueOf(from));
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
