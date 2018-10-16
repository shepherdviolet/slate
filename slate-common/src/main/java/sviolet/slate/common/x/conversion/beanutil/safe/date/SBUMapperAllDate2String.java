package sviolet.slate.common.x.conversion.beanutil.safe.date;

import org.slf4j.Logger;
import sviolet.slate.common.x.conversion.beanutil.PropMapper;
import sviolet.thistle.util.judge.CheckUtils;

import java.text.SimpleDateFormat;

public class SBUMapperAllDate2String implements PropMapper {

    private static final Class[] FROM = new Class[]{
            java.util.Date.class,
            java.sql.Date.class,
            java.sql.Timestamp.class
    };

    private static final Class[] TO = new Class[]{
            String.class,
    };

    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    private ThreadLocal<SimpleDateFormat> dateFormats = new ThreadLocal<>();
    private String dateFormat;

    public SBUMapperAllDate2String(String dateFormat) {
        //注意这个值可能为空
        if (CheckUtils.isEmptyOrBlank(dateFormat)) {
            dateFormat = DEFAULT_DATE_FORMAT;
        }
        this.dateFormat = dateFormat;
        //pre check format
        dateFormats.set(new SimpleDateFormat(dateFormat));
    }

    @Override
    public Object map(Object from, Class<?> toType, Logger logger, boolean logEnabled) {
        SimpleDateFormat format = dateFormats.get();
        if (format == null) {
            format = new SimpleDateFormat(dateFormat);
            dateFormats.set(format);
        }
        return format.format((java.util.Date)from);
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
