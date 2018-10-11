package sviolet.slate.common.x.conversion.beanutil;

import org.springframework.cglib.beans.BeanCopier;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.objenesis.SpringObjenesis;
import sviolet.thistle.model.concurrent.lock.UnsafeSpinLock;
import sviolet.thistle.x.common.thistlespi.ThistleSpi;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bean工具
 *
 * @author S.Violet
 */
public class SlateBeanUtils {

    @SuppressWarnings("deprecation")
    private static final UnsafeSpinLock spinLock = new UnsafeSpinLock();

    private static volatile SpringObjenesis objenesis;
    private static volatile BeanPropConverter converter;

    private static final Map<String, BeanCopier> copiers = new ConcurrentHashMap<>(256);

    /**
     * <p>JavaBean参数拷贝</p>
     * <p>参数类型不匹配时一般不会抛出异常, 会跳过不匹配的参数</p>
     * <p>内置类型转换器, 可使用ThistleSpi扩展</p>
     * @param from 从这个JavaBean复制
     * @param to 复制到这个JavaBean
     * @throws MappingException 拷贝出错(异常概率:中)
     */
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
            throw new MappingException("SlateBeanUtils: Error while copying " + copierName, e, from.getClass().getName(), to.getClass().getName(), null);
        }
    }

    /**
     * <p>JavaBean参数拷贝, 目的JavaBean自动实例化</p>
     * <p>参数类型不匹配时一般不会抛出异常, 会跳过不匹配的参数</p>
     * <p>内置类型转换器, 可使用ThistleSpi扩展</p>
     * @param from 从这个JavaBean复制
     * @param toType 目的JavaBean类型
     * @throws MappingException 拷贝出错(异常概率:中)
     */
    public static <T> T copy(Object from, Class<T> toType) {
        if (toType == null) {
            return null;
        }
        T to = getObjenesis().newInstance(toType, false);
        if (from == null) {
            return to;
        }

        copy(from, to);
        return to;
    }

    /**
     * <p>JavaBean转Map</p>
     * <p>一般不会抛出异常</p>
     * <p>无类型转换器, 因为Bean转Map不存在类型不匹配</p>
     * @param fromBean 从这个JavaBean复制
     * @param toMap 复制到这个Map
     * @throws MappingException 转换出错(异常概率:低)
     */
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
            throw new MappingException("SlateBeanUtils: Error while mapping " + fromBean.getClass().getName() + " to Map", e, fromBean.getClass().getName(), "Map", null);
        }
    }

    /**
     * <p>JavaBean转Map, 目的Map自动创建</p>
     * <p>一般不会抛出异常</p>
     * <p>无类型转换器, 因为Bean转Map不存在类型不匹配</p>
     * @param fromBean 从这个JavaBean复制
     * @throws MappingException 转换出错(异常概率:低)
     */
    public static Map<String, Object> toMap(Object fromBean) {
        Map<String, Object> map = new HashMap<>();
        toMap(fromBean, map);
        return map;
    }

    /**
     * <p>Map转JavaBean</p>
     * <p>容易因为Map中字段类型与Bean参数类型不匹配抛出异常</p>
     * <p>内置类型转换器, 可使用ThistleSpi扩展</p>
     * @param fromMap 从这个Map取值
     * @param toBean 复制到这个JavaBean
     * @throws MappingException 转换出错(异常概率:高), Map中字段类型与Bean参数类型不匹配很容易抛出异常
     */
    public static void fromMap(Map<String, Object> fromMap, Object toBean) {
        if (fromMap == null || toBean == null) {
            return;
        }
        BeanMap beanMap;
        try {
            beanMap = BeanMap.create(toBean);
        } catch (Exception e) {
            throw new MappingException("SlateBeanUtils: Error while mapping Map to " + toBean.getClass().getName(), e, "Map", toBean.getClass().getName(), null);
        }
        for (Object key : beanMap.keySet()) {
            try {
                beanMap.put(key, fromMap.get(String.valueOf(key)));
            } catch (Exception e) {
                throw new MappingException("SlateBeanUtils: Error while mapping Map to " + toBean.getClass().getName() + ", putting \"" + key + "\" failed", e, "Map", toBean.getClass().getName(), String.valueOf(key));
            }
        }
    }

    /**
     * <p>Map转JavaBean</p>
     * <p>容易因为Map中字段类型与Bean参数类型不匹配抛出异常</p>
     * <p>内置类型转换器, 可使用ThistleSpi扩展</p>
     * @param fromMap 从这个Map取值
     * @param toType 目的JavaBean类型
     * @throws MappingException 转换出错(异常概率:高), Map中字段类型与Bean参数类型不匹配很容易抛出异常
     */
    public static <T> T fromMap(Map<String, Object> fromMap, Class<T> toType) {
        if (toType == null) {
            return null;
        }
        T to = getObjenesis().newInstance(toType, false);
        if (fromMap == null || fromMap.size() == 0) {
            return to;
        }
        fromMap(fromMap, to);
        return to;
    }

    private static BeanPropConverter getConverter(){
        if (converter == null) {
            try {
                spinLock.lock();
                if (converter == null) {
                    //spi loading
                    converter = ThistleSpi.getLoader().loadService(BeanPropConverter.class);
                    //default
                    if (converter == null) {
                        converter = new BeanPropConverter() {
                            @Override
                            protected Object convert(Object from, Class toType) {
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

}
