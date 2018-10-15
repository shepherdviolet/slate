package sviolet.slate.common.x.conversion.beanutil.safe.date;

import org.slf4j.Logger;
import sviolet.slate.common.x.conversion.beanutil.MappingRuntimeException;
import sviolet.slate.common.x.conversion.beanutil.PropMapper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SBUMapperString2UtilDate implements PropMapper {

    private static final Class[] FROM = new Class[]{
            String.class
    };

    private static final Class[] TO = new Class[]{
            java.util.Date.class,
    };

    private Map<Integer, List<PatternAndFormat>> patternAndFormatMap = new HashMap<>();

    public SBUMapperString2UtilDate() {
        createFormat("yyyy-MM-dd HH:mm:ss.SSS", "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}$");
        createFormat("yyyy-MM-dd HH:mm:ss", "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$");
        createFormat("yyyy-MM-dd", "^\\d{4}-\\d{2}-\\d{2}$");
        createFormat("yyyyMMddHHmmss", "^\\d{14}$");
        createFormat("yyyyMMdd", "^\\d{8}$");
        createFormat("yyyy-MM-dd HH:mm:ss,SSS", "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3}$");
    }

    private void createFormat(String dateFormat, String pattern){
        List<PatternAndFormat> list = patternAndFormatMap.get(dateFormat.length());
        if (list == null) {
            list = new ArrayList<>();
            patternAndFormatMap.put(dateFormat.length(), list);
        }
        list.add(new PatternAndFormat(pattern, dateFormat));
    }

    @Override
    public Object map(Object from, Class<?> toType, Logger logger, boolean logEnabled) {
        String fromStr = (String) from;
        List<PatternAndFormat> list = patternAndFormatMap.get(fromStr.length());
        if (list == null) {
            throw new MappingRuntimeException("Cannot find corresponding formatter for " + fromStr, null, from.getClass().getName(), toType.getName(), null);
        }
        for (PatternAndFormat patternAndFormat : list) {
            if (patternAndFormat.check(fromStr)) {
                try {
                    return patternAndFormat.parse(fromStr);
                } catch (Exception e) {
                    throw new MappingRuntimeException("Parse date string '" + fromStr + "' to 'Date' object by format " + patternAndFormat.dateFormat + " failed", e, from.getClass().getName(), toType.getName(), null);
                }
            }
        }
        throw new MappingRuntimeException("Cannot find corresponding formatter for " + fromStr, null, from.getClass().getName(), toType.getName(), null);
    }

    @Override
    public Class<?>[] fromType() {
        return FROM;
    }

    @Override
    public Class<?>[] toType() {
        return TO;
    }

    private static class PatternAndFormat {
        private Pattern pattern;
        private SimpleDateFormat dateFormat;

        private PatternAndFormat(String pattern, String dateFormat) {
            this.pattern = Pattern.compile(pattern);
            this.dateFormat = new SimpleDateFormat(dateFormat);
        }

        private boolean check(String value) {
            return pattern.matcher(value).matches();
        }

        private java.util.Date parse(String value) throws ParseException {
            return dateFormat.parse(value);
        }
    }

}
