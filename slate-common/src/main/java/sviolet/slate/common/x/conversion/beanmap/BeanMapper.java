package sviolet.slate.common.x.conversion.beanmap;

import org.springframework.cglib.beans.BeanCopier;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.objenesis.SpringObjenesis;
import sviolet.thistle.model.concurrent.lock.UnsafeSpinLock;
import sviolet.thistle.x.common.thistlespi.ThistleSpi;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BeanMapper {

    @SuppressWarnings("deprecation")
    private static final UnsafeSpinLock spinLock = new UnsafeSpinLock();

    private static volatile SpringObjenesis objenesis;
    private static volatile MapperConverter converter;

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
            copier.copy(from, to, getConverter());
        } catch (Exception e) {
            throw new MappingException("BeanMapper: Error while copying " + copierName, e, from.getClass().getName(), to.getClass().getName(), null);
        }
    }

    public static <T> T copy(Object from, Class<T> toType) {
        if (toType == null) {
            return null;
        }
        T to;
        try {
            to = getObjenesis().newInstance(toType, false);
        } catch (Exception e) {
            throw new InstantiationException("BeanMapper: Error while " + toType.getName() + " instantiation", e, toType.getName());
        }
        if (from == null) {
            return to;
        }

        copy(from, to);
        return to;
    }

    public static void toMap(Object fromBean, Map<String, Object> toMap) {
        if (fromBean == null || toMap == null) {
            return;
        }
        try {
            BeanMap beanMap = BeanMap.create(fromBean);
            for (Object key : beanMap.keySet()) {
                toMap.put(String.valueOf(key), beanMap.get(key));
            }
        } catch (Exception e) {
            throw new MappingException("BeanMapper: Error while mapping " + fromBean.getClass().getName() + " to Map", e, fromBean.getClass().getName(), "Map", null);
        }
    }

    public static Map<String, Object> toMap(Object fromBean) {
        Map<String, Object> map = new HashMap<>();
        toMap(fromBean, map);
        return map;
    }

    public static void fromMap(Map<String, Object> fromMap, Object toBean) {
        if (fromMap == null || toBean == null) {
            return;
        }
        BeanMap beanMap;
        try {
            beanMap = BeanMap.create(toBean);
        } catch (Exception e) {
            throw new MappingException("BeanMapper: Error while mapping Map to " + toBean.getClass().getName(), e, "Map", toBean.getClass().getName(), null);
        }
        for (Object key : beanMap.keySet()) {
            try {
                beanMap.put(key, fromMap.get(String.valueOf(key)));
            } catch (Exception e) {
                throw new MappingException("BeanMapper: Error while mapping Map to " + toBean.getClass().getName() + ", putting \"" + key + "\" failed", e, "Map", toBean.getClass().getName(), String.valueOf(key));
            }
        }
    }

    public static <T> T fromMap(Map<String, Object> fromMap, Class<T> toType) {
        if (toType == null) {
            return null;
        }
        T to;
        try {
            to = getObjenesis().newInstance(toType, false);
        } catch (Exception e) {
            throw new InstantiationException("BeanMapper: Error while " + toType.getName() + " instantiation", e, toType.getName());
        }
        if (fromMap == null || fromMap.size() == 0) {
            return to;
        }
        fromMap(fromMap, to);
        return to;
    }

    private static MapperConverter getConverter(){
        if (converter == null) {
            try {
                spinLock.lock();
                if (converter == null) {
                    //spi loading
                    converter = ThistleSpi.getLoader().loadService(MapperConverter.class);
                    //default
                    if (converter == null) {
                        converter = new MapperConverter() {
                            @Override
                            public Object convert(Object from, Class toType, Object setMethodName) {
                                return from;
                            }
                        };
                    }
                }
            } finally {
                spinLock.unlock();
            }
        }
        return converter;
    }

    private static SpringObjenesis getObjenesis(){
        if (objenesis == null) {
            try {
                spinLock.lock();
                if (objenesis == null) {
                    objenesis = new SpringObjenesis();
                }
            } finally {
                spinLock.unlock();
            }
        }
        return objenesis;
    }

    public static class InstantiationException extends RuntimeException {
        private String beanType;
        private InstantiationException(String message, Throwable cause, String beanType) {
            super(message, cause);
            this.beanType = beanType;
        }
        public String getBeanType() {
            return beanType;
        }
    }

    public static class MappingException extends RuntimeException {
        private String from;
        private String to;
        private String field;
        private MappingException(String message, Throwable cause, String from, String to, String field) {
            super(message, cause);
        }
        public String getFrom() {
            return from;
        }
        public String getTo() {
            return to;
        }
        public String getField() {
            return field;
        }
    }

}
