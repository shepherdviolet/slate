package sviolet.slate.common.x.conversion.beanutil.safe.date;

import sviolet.slate.common.x.conversion.beanutil.PropMapper;

import java.text.SimpleDateFormat;

public class SBUMapperUtilDate2String implements PropMapper {

    private static final Class[] FROM = new Class[]{
            java.util.Date.class,
    };

    private static final Class[] TO = new Class[]{
            String.class,
    };

    private ThreadLocal<SimpleDateFormat> dateFormats = new ThreadLocal<>();
    private String dateFormat;

    public SBUMapperUtilDate2String() {
        this("yyyy-MM-dd HH:mm:ss.SSS");
    }

    public SBUMapperUtilDate2String(String dateFormat) {
        this.dateFormat = dateFormat;
        //pre check format
        dateFormats.set(new SimpleDateFormat(dateFormat));
    }

    @Override
    public Object map(Object from, Class<?> toType) {
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
