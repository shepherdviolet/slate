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
 * JavaBean转换工具
 *
 * @author S.Violet
 */
public class SlateBeanUtils {

    @SuppressWarnings("deprecation")
    private static final UnsafeSpinLock SPIN_LOCK = new UnsafeSpinLock();

    private static volatile SpringObjenesis objenesis;
    private static volatile BeanConverter converter;

    private static final Map<String, BeanCopier> COPIER = new ConcurrentHashMap<>(256);
    private static final Map<String, BeanizationFactory> BEANIZATION_FACTORYS = new ConcurrentHashMap<>(256);

    /**
     * <p>JavaBean参数拷贝</p>
     * <p>参数类型不匹配时一般不会抛出异常, 会跳过不匹配的参数</p>
     * <p>内置类型转换器, 可使用ThistleSpi扩展</p>
     * @param from 从这个JavaBean复制
     * @param to 复制到这个JavaBean
     * @throws MappingRuntimeException 拷贝出错(异常概率:中)
     */
    public static void copy(Object from, Object to) {
        if (from == null || to == null) {
            return;
        }

        String copierName = from.getClass().getName() + "->" + to.getClass().getName();
        try {
            BeanCopier copier = COPIER.get(copierName);
            if (copier == null) {
                copier = BeanCopier.create(from.getClass(), to.getClass(), true);
                COPIER.put(copierName, copier);
            }
            copier.copy(from, to, getConverter());
        } catch (MappingRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new MappingRuntimeException("SlateBeanUtils: Error while copying " + copierName, e, from.getClass().getName(), to.getClass().getName(), null);
        }
    }

    /**
     * <p>JavaBean参数拷贝, 目的JavaBean自动实例化</p>
     * <p>参数类型不匹配时一般不会抛出异常, 会跳过不匹配的参数</p>
     * <p>内置类型转换器, 可使用ThistleSpi扩展</p>
     * @param from 从这个JavaBean复制
     * @param toType 目的JavaBean类型
     * @throws MappingRuntimeException 拷贝出错(异常概率:中)
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
     * @throws MappingRuntimeException 转换出错(异常概率:低)
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
            throw new MappingRuntimeException("SlateBeanUtils: Error while mapping " + fromBean.getClass().getName() + " to Map", e, fromBean.getClass().getName(), "java.util.Map", null);
        }
    }

    /**
     * <p>JavaBean转Map, 目的Map自动创建</p>
     * <p>一般不会抛出异常</p>
     * <p>无类型转换器, 因为Bean转Map不存在类型不匹配</p>
     * @param fromBean 从这个JavaBean复制
     * @throws MappingRuntimeException 转换出错(异常概率:低)
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
     * @param convert true:转换参数类型使之符合要求, false:不转换参数类型, 不符合就直接报错
     * @throws MappingRuntimeException 转换出错(异常概率:高), Map中字段类型与Bean参数类型不匹配很容易抛出异常
     */
    public static void fromMap(Map<String, Object> fromMap, Object toBean, boolean convert) {
        if (fromMap == null || toBean == null) {
            return;
        }
        fromMap = mapBeanization(fromMap, toBean.getClass(), convert);
        BeanMap beanMap;
        try {
            beanMap = BeanMap.create(toBean);
        } catch (Exception e) {
            throw new MappingRuntimeException("SlateBeanUtils: Error while mapping Map to " + toBean.getClass().getName() + ", map data:" + fromMap,
                    e, "java.util.Map", toBean.getClass().getName(), null);
        }
        for (Object key : beanMap.keySet()) {
            try {
                beanMap.put(key, fromMap.get(String.valueOf(key)));
            } catch (Exception e) {
                throw new MappingRuntimeException("SlateBeanUtils: Error while mapping Map to " + toBean.getClass().getName() + ", putting \"" + key + "\" failed, map data:" + fromMap,
                        e, "java.util.Map", toBean.getClass().getName(), String.valueOf(key));
            }
        }
    }

    /**
     * <p>Map转JavaBean</p>
     * <p>容易因为Map中字段类型与Bean参数类型不匹配抛出异常</p>
     * <p>内置类型转换器, 可使用ThistleSpi扩展</p>
     * @param fromMap 从这个Map取值
     * @param toType 目的JavaBean类型
     * @param convert true:转换参数类型使之符合要求, false:不转换参数类型, 不符合就直接报错
     * @throws MappingRuntimeException 转换出错(异常概率:高), Map中字段类型与Bean参数类型不匹配很容易抛出异常
     */
    public static <T> T fromMap(Map<String, Object> fromMap, Class<T> toType, boolean convert) {
        if (toType == null) {
            return null;
        }
        T to = getObjenesis().newInstance(toType, false);
        if (fromMap == null || fromMap.size() == 0) {
            return to;
        }
        fromMap(fromMap, to, convert);
        return to;
    }

    /**
     * 用于Map转换为Bean前的预处理. 依据指定的JavaBean类型(templateType), 检查Map的参数类型是否符合要求, 若不符合要求,
     * 则尝试进行类型转换使之符合要求, 若还是不符合要求, 则抛出异常.
     * (SlateBeanUtils.fromMap方法内部已调用该方法, 在使用fromMap时无需手动调用该方法)
     * @param map 需要进行参数矫正的Map
     * @param templateType JavaBean类型
     * @param convert true:转换参数类型使之符合要求, false:不转换参数类型, 不符合就直接报错
     * @return 返回矫正后的Map(仅保留需要的参数, 且类型已经过转换)
     * @throws MappingRuntimeException 矫正出错(异常概率:高), Map中字段类型与Bean参数类型不匹配很容易抛出异常
     */
    public static Map<String, Object> mapBeanization(Map<String, Object> map, Class<?> templateType, boolean convert){
        if (map == null || map.size() <= 0 || templateType == null) {
            return new HashMap<>();
        }
        String factoryName = templateType.getName();
        try {
            BeanizationFactory factory = BEANIZATION_FACTORYS.get(factoryName);
            if (factory == null) {
                factory = new BeanizationFactory(templateType, getConverter());
                BEANIZATION_FACTORYS.put(factoryName, factory);
            }
            return factory.beanization(map, convert);
        } catch (MappingRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new MappingRuntimeException("SlateBeanUtils: Error while pre-mapping (check and conversion) Map to " + templateType.getName() + ", map data:" + map, e, "java.util.Map", templateType.getName(), null);
        }
    }

    private static BeanConverter getConverter(){
        if (converter == null) {
            try {
                SPIN_LOCK.lock();
                if (converter == null) {
                    //spi loading
                    converter = ThistleSpi.getLoader().loadService(BeanConverter.class);
                    //default
                    if (converter == null) {
                        converter = new BeanConverter() {
                            @Override
                            protected Object onConvert(Cause cause, Object from, Class... toTypes) {
                                return from;
                            }
                        };
                    }
                }
            } finally {
                SPIN_LOCK.unlock();
            }
        }
        return converter;
    }

    private static SpringObjenesis getObjenesis(){
        if (objenesis == null) {
            try {
                SPIN_LOCK.lock();
                if (objenesis == null) {
                    objenesis = new SpringObjenesis();
                }
            } finally {
                SPIN_LOCK.unlock();
            }
        }
        return objenesis;
    }

}
