package sviolet.slate.common.x.conversion.beanutil.safe.date;

import org.slf4j.Logger;
import sviolet.slate.common.x.conversion.beanutil.PropMapper;

public class SBUMapperAllDate2UtilDate implements PropMapper {

    private static final Class[] FROM = new Class[]{
            java.util.Date.class,
            java.sql.Date.class,
            java.sql.Timestamp.class
    };

    private static final Class[] TO = new Class[]{
            java.util.Date.class,
    };

    @Override
    public Object map(Object from, Class<?> toType, Logger logger, boolean logEnabled) {
        return new java.util.Date(((java.util.Date)from).getTime());
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
