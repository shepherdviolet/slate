package sviolet.slate.common.x.conversion.beanutil.safe.date;

import org.slf4j.Logger;

public class SBUMapperString2SqlTimestamp extends SBUMapperString2UtilDate {

    private static final Class[] FROM = new Class[]{
            String.class
    };

    private static final Class[] TO = new Class[]{
            java.sql.Timestamp.class,
    };

    @Override
    public Object map(Object from, Class<?> toType, Logger logger, boolean logEnabled) {
        return new java.sql.Timestamp(((java.util.Date)super.map(from, toType, logger, logEnabled)).getTime());
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
