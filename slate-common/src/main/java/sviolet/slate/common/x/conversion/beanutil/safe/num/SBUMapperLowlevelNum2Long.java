package sviolet.slate.common.x.conversion.beanutil.safe.num;

import sviolet.slate.common.x.conversion.beanutil.PropMapper;

public class SBUMapperLowlevelNum2Long implements PropMapper {

    private static final Class[] FROM = new Class[]{
            short.class,
            Short.class,
            int.class,
            Integer.class,
    };

    private static final Class[] TO = new Class[]{
            long.class,
            Long.class,
    };

    @Override
    public Object map(Object from, Class<?> toType) {
        return Long.valueOf(String.valueOf(from));
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
