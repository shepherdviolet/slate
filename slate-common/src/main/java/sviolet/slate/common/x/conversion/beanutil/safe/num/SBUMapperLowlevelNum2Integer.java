package sviolet.slate.common.x.conversion.beanutil.safe.num;

import sviolet.slate.common.x.conversion.beanutil.PropMapper;

public class SBUMapperLowlevelNum2Integer implements PropMapper {

    private static final Class[] FROM = new Class[]{
            short.class,
            Short.class,
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
        return Integer.valueOf(String.valueOf(from));
    }

}
