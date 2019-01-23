/*
 * Copyright (C) 2015-2018 S.Violet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project GitHub: https://github.com/shepherdviolet/slate
 * Email: shepherdviolet@163.com
 */

package sviolet.slate.common.x.conversion.beanutil;

import com.github.shepherdviolet.glaciion.Glaciion;
import org.springframework.cglib.beans.BeanCopier;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.cglib.core.Converter;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.SpringObjenesis;
import sviolet.thistle.model.concurrent.lock.UnsafeSpinLock;
import sviolet.thistle.util.conversion.BeanMethodNameUtils;
import sviolet.thistle.util.conversion.PrimitiveUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bean转换工具
 *
 * @author S.Violet
 */
public class SlateBeanUtils {

    @SuppressWarnings("deprecation")
    private static final UnsafeSpinLock SPIN_LOCK = new UnsafeSpinLock();

    private static volatile SpringObjenesis objenesis;
    private static volatile BeanConverter converter;
    private static volatile Converter beanCopierConverter;
    private static volatile IndivisibleJudge judger;

    private static final Map<String, BeanCopier> COPIER = new ConcurrentHashMap<>(256);
    private static final Map<String, BeanizationFactory> BEANIZATION_FACTORYS = new ConcurrentHashMap<>(256);

    /**
     * <p>浅克隆, 只拷贝第一层参数</p>
     * <p>Bean参数拷贝, Bean -> Bean</p>
     * <p>参数类型不匹配时一般不会抛出异常, 会跳过不匹配的参数(参数留空)</p>
     * <p>内置类型转换器, 当类型不匹配时会尝试转换, 可使用ThistleSpi扩展</p>
     * @param from 从这个Bean复制(必须是个Bean, 无法复制Map/List对象)
     * @param to 复制到这个Bean(必须是个Bean, 无法复制Map/List对象)
     * @throws MappingRuntimeException 异常概率:低, 触发原因: 拷贝器创建失败 / 拷贝过程出错
     */
    public static void beanToBean(Object from, Object to) {
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
            copier.copy(from, to, getBeanCopierConverter());
        } catch (MappingRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new MappingRuntimeException("SlateBeanUtils: Error while copying " + from.getClass().getName() + " to " + to.getClass().getName(), e, from.getClass().getName(), to.getClass().getName(), null);
        }
    }

    /**
     * <p>浅克隆, 只拷贝第一层参数</p>
     * <p>Bean参数拷贝, Bean -> Bean</p>
     * <p>参数类型不匹配时一般不会抛出异常, 会跳过不匹配的参数(参数留空)</p>
     * <p>内置类型转换器, 当类型不匹配时会尝试转换, 可使用ThistleSpi扩展</p>
     * @param from 从这个Bean复制(必须是个Bean, 无法复制Map/List对象)
     * @param toType 目的Bean类型(必须是个Bean, 无法复制Map/List对象)
     * @throws MappingRuntimeException 异常概率:低, 触发原因: 拷贝器创建失败 / 拷贝过程出错
     * @throws ObjenesisException 异常概率:低, 触发原因: 目标Bean实例化失败
     */
    public static <T> T beanToBean(Object from, Class<T> toType) {
        if (toType == null) {
            return null;
        }
        T to = getObjenesis().newInstance(toType, false);
        if (from == null) {
            return to;
        }

        beanToBean(from, to);
        return to;
    }

    /**
     * <p>浅克隆, 只拷贝第一层参数</p>
     * <p>Bean转Map</p>
     * <p>一般不会抛出异常</p>
     * <p>无内置类型转换器, 因为Bean转Map不存在类型不匹配的情况</p>
     * @param fromBean 从这个Bean复制(必须是个Bean, 无法复制Map/List对象)
     * @param toMap 复制到这个Map
     * @throws MappingRuntimeException 异常概率:低, 触发原因: 映射器创建失败
     */
    public static void beanToMap(Object fromBean, Map<String, Object> toMap) {
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
     * <p>浅克隆, 只拷贝第一层参数</p>
     * <p>Bean转Map</p>
     * <p>一般不会抛出异常</p>
     * <p>无内置类型转换器, 因为Bean转Map不存在类型不匹配的情况</p>
     * @param fromBean 从这个Bean复制(必须是个Bean, 无法复制Map/List对象)
     * @throws MappingRuntimeException 异常概率:低, 触发原因: 映射器创建失败
     */
    public static Map<String, Object> beanToMap(Object fromBean) {
        Map<String, Object> map = new HashMap<>();
        beanToMap(fromBean, map);
        return map;
    }

    /**
     * <p>浅克隆, 只拷贝第一层参数</p>
     * <p>Map转Bean</p>
     * <p>当Map中字段类型与Bean参数类型不匹配时会抛出异常(若设置throwExceptionIfFails为false, 则不会抛出异常, 失败的参数留空)</p>
     * <p>内置类型转换器, 当类型不匹配时会尝试转换, 可使用ThistleSpi扩展</p>
     * @param fromMap 从这个Map取值
     * @param toBean 复制到这个Bean(必须是个Bean, 无法复制Map/List对象)
     * @param convert true: 尝试转换参数类型使之符合要求, false: 不转换参数类型
     * @param throwExceptionIfFails true: 如果参数的类型不匹配或转换失败, 则抛出异常, false: 如果参数的类型不匹配或转换失败, 不会抛出异常, 失败的参数留空
     * @throws MappingRuntimeException 异常概率:高, 触发原因: Map中字段类型与Bean参数类型不匹配(当throwExceptionIfFails=true) / 给目的Bean赋值时出错(当throwExceptionIfFails=true) / Bean映射器创建失败(无论throwExceptionIfFails为何值, 均抛异常)
     */
    public static void mapToBean(Map<String, Object> fromMap, Object toBean, boolean convert, boolean throwExceptionIfFails) {
        if (fromMap == null || toBean == null) {
            return;
        }
        fromMap = mapBeanization(fromMap, toBean.getClass(), convert, throwExceptionIfFails);
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
                if (throwExceptionIfFails) {
                    throw new MappingRuntimeException("SlateBeanUtils: Error while mapping Map to " + toBean.getClass().getName() + ", putting \"" + key + "\" failed, map data:" + fromMap,
                            e, "java.util.Map", toBean.getClass().getName(), String.valueOf(key));
                }
            }
        }
    }

    /**
     * <p>浅克隆, 只拷贝第一层参数</p>
     * <p>Map转Bean</p>
     * <p>当Map中字段类型与Bean参数类型不匹配时会抛出异常(若设置throwExceptionIfFails为false, 则不会抛出异常, 失败的参数留空)</p>
     * <p>内置类型转换器, 当类型不匹配时会尝试转换, 可使用ThistleSpi扩展</p>
     * @param fromMap 从这个Map取值
     * @param toType 目的Bean类型(必须是个Bean, 无法复制Map/List对象)
     * @param convert true: 尝试转换参数类型使之符合要求, false: 不转换参数类型
     * @param throwExceptionIfFails true: 如果参数的类型不匹配或转换失败, 则抛出异常, false: 如果参数的类型不匹配或转换失败, 不会抛出异常, 失败的参数留空
     * @throws MappingRuntimeException 异常概率:高, 触发原因: Map中字段类型与Bean参数类型不匹配(当throwExceptionIfFails=true) / 给目的Bean赋值时出错(当throwExceptionIfFails=true) / Bean映射器创建失败(无论throwExceptionIfFails为何值, 均抛异常)
     * @throws ObjenesisException 异常概率:低, 触发原因: 目标Bean实例化失败
     */
    public static <T> T mapToBean(Map<String, Object> fromMap, Class<T> toType, boolean convert, boolean throwExceptionIfFails) {
        if (toType == null) {
            return null;
        }
        T to = getObjenesis().newInstance(toType, false);
        if (fromMap == null || fromMap.size() == 0) {
            return to;
        }
        mapToBean(fromMap, to, convert, throwExceptionIfFails);
        return to;
    }

    /**
     * <p>特殊, 只处理第一层参数</p>
     * <p>用于Map转换为Bean前的预处理. 依据指定的Bean类型(templateType), 检查Map的参数类型是否符合要求, 若不符合要求,
     * 则尝试进行类型转换, 若还是不符合, 则根据throwExceptionIfFails参数处理(true:抛出异常, false:从Map中剔除不匹配的参数).</p>
     * <p>(SlateBeanUtils.fromMap方法内部已调用该方法, 在使用fromMap时无需手动调用该方法)</p>
     * @param map 需要进行参数矫正的Map
     * @param templateType Bean类型(必须是个Bean, 无法处理Map/List对象)
     * @param convert true:转换参数类型使之符合要求, false:不转换参数类型
     * @param throwExceptionIfFails true:如果参数的类型不匹配或转换失败, 则抛出异常, false:如果参数的类型不匹配或转换失败, 则从Map中剔除该参数
     * @return 返回矫正后的Map(仅保留需要的参数, 且类型已经过转换, 不匹配的参数会被剔除)
     * @throws MappingRuntimeException 异常概率:高, 触发原因: Map中字段类型与Bean参数类型不匹配(当throwExceptionIfFails=true) / 特殊情况(概率极低, 例如BeanizationFactory有内部漏洞等, 无论throwExceptionIfFails是何值, 均抛出异常)
     */
    public static Map<String, Object> mapBeanization(Map<String, Object> map, Class<?> templateType, boolean convert, boolean throwExceptionIfFails){
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
            return factory.beanization(map, convert, throwExceptionIfFails);
        } catch (MappingRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new MappingRuntimeException("SlateBeanUtils: Error while pre-mapping (check and conversion) Map to " + templateType.getName() + ", map data:" + map, e, "java.util.Map", templateType.getName(), null);
        }
    }

    /**
     * <p>递归, 递归复制多层参数直到不可分割的类型</p>
     * <p>Bean转Map</p>
     * <p>一般不会抛出异常</p>
     * <p>无内置类型转换器, 因为Bean转Map不存在类型不匹配的情况</p>
     * @param fromBean 从这个Bean复制(必须是个Bean或Map, 无法复制List对象)
     * @throws MappingRuntimeException 异常概率:低, 触发原因: 映射器创建失败
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> beanOrMapToMapRecursively(Object fromBean) {
        return beanOrMapToMapRecursively(fromBean, null);
    }

    /**
     * <p>递归, 递归复制多层参数直到不可分割的类型</p>
     * <p>Bean转Map</p>
     * <p>一般不会抛出异常</p>
     * <p>无内置类型转换器, 因为Bean转Map不存在类型不匹配的情况</p>
     * @param fromBean 从这个Bean复制(必须是个Bean或Map, 无法复制List对象)
     * @param extraIndivisibleTypes 默认请设null, 指定额外的不可分割的类型(当遍历到这个类型的对象, 直接保留在Map中不再拆解)
     * @throws MappingRuntimeException 异常概率:低, 触发原因: 映射器创建失败
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> beanOrMapToMapRecursively(Object fromBean, Map<Class<?>, IndivisibleJudge.JudgeType> extraIndivisibleTypes) {
        if (fromBean == null) {
            return new HashMap<>();
        }
        if (fromBean instanceof List) {
            throw new MappingRuntimeException("SlateBeanUtils: Root node cannot be a list, data:" + String.valueOf(fromBean), null, fromBean.getClass().getName(), "java.util.Map", null);
        }
        return (Map<String, Object>) beanOrMapToMapRecursivelyInner(fromBean, fromBean, extraIndivisibleTypes, "");
    }

    private static Object beanOrMapToMapRecursivelyInner(Object root, Object fromBean, Map<Class<?>, IndivisibleJudge.JudgeType> extraIndivisibleTypes, String path) {
        if (fromBean == null) {
            return null;
        }
        //handle list
        if (fromBean instanceof List) {
            List<?> fromList = (List) fromBean;
            List<Object> toList = new ArrayList<>(fromList.size());
            for (Object value : fromList) {
                //Recursive if it is not indivisible
                if (getJudger().isIndivisible(value, extraIndivisibleTypes)) {
                    toList.add(value);
                } else {
                    toList.add(beanOrMapToMapRecursivelyInner(root, value, extraIndivisibleTypes, path + "=>"));
                }
            }
            return toList;
        }
        Map<?, ?> fromMap;
        Map<String, Object> toMap;
        try {
            if (fromBean instanceof Map) {
                //handle map
                fromMap = (Map<?, ?>) fromBean;
            } else {
                //handle bean
                fromMap = BeanMap.create(fromBean);
                //return bean if no properties
                if (fromMap.size() <= 0) {
                    return fromBean;
                }
            }
            toMap = new HashMap<>(fromMap.size());
            for (Object key : fromMap.keySet()) {
                String keyStr = String.valueOf(key);
                Object value = fromMap.get(key);
                //Recursive if it is not indivisible
                if (getJudger().isIndivisible(value, extraIndivisibleTypes)) {
                    toMap.put(keyStr, value);
                } else {
                    toMap.put(keyStr, beanOrMapToMapRecursivelyInner(root, value, extraIndivisibleTypes, path + "->" + key));
                }
            }
        } catch (MappingRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new MappingRuntimeException("SlateBeanUtils: Error while mapping " + root.getClass().getName() + " to Map (" + fromBean.getClass().getName() +
                    " to Map), recursively path:" + path, e, root.getClass().getName(), "java.util.Map", null);
        }
        return toMap;
    }

    private static BeanConverter getConverter(){
        if (converter == null) {
            try {
                SPIN_LOCK.lock();
                if (converter == null) {
                    //spi loading
                    converter = Glaciion.loadSingleService(BeanConverter.class).get();
                    //default
                    if (converter == null) {
                        converter = new BeanConverter() {
                            @Override
                            public Object onConvert(Cause cause, Object from, Class... toTypes) {
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

    private static Converter getBeanCopierConverter(){
        //no lock
        if (beanCopierConverter == null) {
            beanCopierConverter = new BeanCopierConverter(getConverter());
        }
        return beanCopierConverter;
    }

    private static IndivisibleJudge getJudger(){
        if (judger == null) {
            try {
                SPIN_LOCK.lock();
                if (judger == null) {
                    //spi loading
                    judger = Glaciion.loadSingleService(IndivisibleJudge.class).get();
                    //default
                    if (judger == null) {
                        judger = new DefaultIndivisibleJudge();
                    }
                }
            } finally {
                SPIN_LOCK.unlock();
            }
        }
        return judger;
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

    /**
     * BeanCopier的Converter包装类, 适配成自定义的BeanConverter
     */
    private static class BeanCopierConverter implements Converter {

        private BeanConverter converter;

        public BeanCopierConverter(BeanConverter converter) {
            this.converter = converter;
        }

        @Override
        public final Object convert(Object from, Class toType, Object setMethodName) {
            try {
                return converter.onConvert(BeanConverter.Cause.BEAN_TO_BEAN, from, new Class[]{PrimitiveUtils.toWrapperType(toType)});
            } catch (MappingRuntimeException e) {
                //补上field名
                String fieldName = BeanMethodNameUtils.methodToField(String.valueOf(setMethodName));
                e.setFieldName(fieldName);
                throw e;
            }
        }

    }

}
