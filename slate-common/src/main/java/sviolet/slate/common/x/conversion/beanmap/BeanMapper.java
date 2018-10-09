package sviolet.slate.common.x.conversion.beanmap;

import org.springframework.cglib.beans.BeanCopier;
import org.springframework.objenesis.SpringObjenesis;
import sviolet.thistle.x.common.thistlespi.ThistleSpi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BeanMapper {

    private static final MapperConverter converter;
    private static final SpringObjenesis objenesis = new SpringObjenesis();

    static {
        MapperConverter c = ThistleSpi.getLoader().loadService(MapperConverter.class);
        if (c == null) {
            c = new MapperConverter() {
                @Override
                public Object convert(Object from, Class toType, Object setMethodName) {
                    return from;
                }
            };
        }
        converter = c;
    }

    private static final Map<String, BeanCopier> copiers = new ConcurrentHashMap<>(256);

    public static void copy(Object from, Object to) {
        if (from == null || to == null) {
            return;
        }

        String copierName = from.getClass().getName() + "->" + to.getClass().getName();
        try {
            BeanCopier copier = copiers.get(copierName);
            if (copier == null) {
                copier = BeanCopier.create(from.getClass(), to.getClass(), true);
                copiers.put(copierName, copier);
            }
            copier.copy(from, to, converter);
        } catch (Exception e) {
            throw new RuntimeException("BeanMapper: Error while copying " + copierName, e);
        }
    }

    public static <T> T copy(Object from, Class<T> toType) {
        if (toType == null) {
            return null;
        }
        T to;
        try {
            to = objenesis.newInstance(toType, false);
        } catch (Exception e) {
            throw new RuntimeException("BeanMapper: Error while " + toType.getName() + " instantiation", e);
        }
        if (from == null) {
            return to;
        }

        copy(from, to);
        return to;
    }

}
