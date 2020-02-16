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

import sviolet.thistle.util.conversion.BeanMethodNameUtils;
import sviolet.thistle.util.conversion.PrimitiveUtils;
import sviolet.thistle.util.judge.CheckUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bean矫正器
 *
 * @author S.Violet
 */
class BeanizationFactory {

    //Bean矫正器缓存
    private static final Map<String, BeanizationFactory> BEANIZATION_FACTORYS = new ConcurrentHashMap<>(256);

    private Class<?> templateType;
    private BeanConverter converter;

    private Map<String, Class<?>[]> templateProperties;
    private int propertiesSize;

    /**
     * 根据Bean类型创建/获取Bean矫正器
     * @param templateType Bean类型(必须是个Bean, 无法处理Map/List对象)
     * @param converter Bean参数类型转换器
     * @return Bean矫正器
     */
    static BeanizationFactory getFactory(Class<?> templateType, BeanConverter converter){
        String factoryName = templateType.getName();
        BeanizationFactory factory = BEANIZATION_FACTORYS.get(factoryName);
        if (factory == null) {
            factory = new BeanizationFactory(templateType, converter);
            BEANIZATION_FACTORYS.put(factoryName, factory);
        }
        return factory;
    }

    BeanizationFactory(Class<?> templateType, BeanConverter converter) {
        this.templateType = templateType;
        this.converter = converter;
        init();
    }

    /**
     * 初始化, 遍历Bean类获取它的参数和参数类型
     */
    private void init(){
        if (templateType == null) {
            throw new NullPointerException("templateType is null");
        }
        if (converter == null) {
            throw new NullPointerException("converter is null");
        }
        Map<String, Set<Class<?>>> properties = new HashMap<>();
        Map<String, Class<?>> definiteProperties = new HashMap<>();
        //遍历所有方法
        Method[] methods = templateType.getMethods();
        if (methods != null) {
            for (Method method : methods) {
                String methodName = method.getName();
                String fieldName = BeanMethodNameUtils.methodToField(methodName);
                if (CheckUtils.isEmptyOrBlank(fieldName)) {
                    continue;
                }
                Class<?>[] paramTypes = method.getParameterTypes();
                if (methodName.startsWith("set")) {
                    //setter
                    if (paramTypes.length != 1) {
                        continue;
                    }
                    Set<Class<?>> classSet = properties.get(fieldName);
                    if (classSet == null) {
                        classSet = new HashSet<>();
                        properties.put(fieldName, classSet);
                    }
                    classSet.add(PrimitiveUtils.toWrapperType(paramTypes[0]));
                } else if (methodName.startsWith("get") || methodName.startsWith("is")) {
                    //getter
                    if (paramTypes.length != 0) {
                        continue;
                    }
                    if (void.class.isAssignableFrom(method.getReturnType())) {
                        continue;
                    }
                    definiteProperties.put(fieldName, PrimitiveUtils.toWrapperType(method.getReturnType()));
                }
            }
        }
        //如果同时有getter和setter方法, 类型以getter方法返回值为准
        for (Map.Entry<String, Class<?>> entry : definiteProperties.entrySet()) {
            if (properties.get(entry.getKey()) != null) {
                Set<Class<?>> classSet = new HashSet<>(1);
                classSet.add(entry.getValue());
                properties.put(entry.getKey(), classSet);
            }
        }
        //装入成员变量
        this.templateProperties = new HashMap<>(properties.size());
        for (Map.Entry<String, Set<Class<?>>> entry : properties.entrySet()) {
            Class<?>[] classArray = new Class[entry.getValue().size()];
            this.templateProperties.put(entry.getKey(), entry.getValue().toArray(classArray));
        }
        propertiesSize = templateProperties.size();
    }

    /**
     * 矫正
     */
    Map<String, Object> beanization(Map<String, Object> map, boolean convert, boolean throwExceptionIfFails){
        Map<String, Object> result = new HashMap<>(propertiesSize);
        for (Map.Entry<String, Class<?>[]> entry : templateProperties.entrySet()) {
            String entryKey = entry.getKey();
            Object value = map.get(entryKey);
            if (value == null) {
                continue;
            }
            Class<?> valueType = value.getClass();
            try {
                boolean found = false;
                //判断类型是否匹配
                for (Class<?> type : entry.getValue()) {
                    if (type.isAssignableFrom(valueType)) {
                        //有可能需要同类型转换
                        if (convert) {
                            value = converter.onConvert(BeanConverter.Cause.BEANIZATION, value, new Class[]{type});
                            if (value == null) {
                                throw new MappingRuntimeException("SlateBeanUtils: Error while pre-mapping (check and conversion) Map to " + templateType.getName() + ", field \"" + entryKey + "\" convert failed (In PropMapper for " + valueType.getName() + " to " + type.getName() + "), map data:" + map, null, "java.util.Map", templateType.getName(), entryKey);
                            }
                        }
                        result.put(entryKey, value);
                        found = true;
                        break;
                    }
                }
                if (found) {
                    continue;
                }
                //类型不匹配需要转换
                if (convert) {
                    //不同类型转换
                    value = converter.onConvert(BeanConverter.Cause.BEANIZATION, value, entry.getValue());
                    if (value == null) {
                        throw new MappingRuntimeException("SlateBeanUtils: Error while pre-mapping (check and conversion) Map to " + templateType.getName() + ", field \"" + entryKey + "\" convert failed (No PropMapper for " + valueType.getName() + " to" + getClassNames(entry.getValue()) + "), map data:" + map, null, "java.util.Map", templateType.getName(), entryKey);
                        /*
                         * 这个方案不完善, 只能针对子元素mapToBean的情况, 会导致mapToBean与beanToMap/beanOrMapToMapRecursively的转换口径都不同, 容易产生误解
                         */
//                        //尝试用通用方法转换
//                        try {
//                            value = commonConvert(entry.getValue(), map.get(entryKey), throwExceptionIfFails);
//                        } catch (Throwable t) {
//                            throw new MappingRuntimeException("SlateBeanUtils: Error while pre-mapping (check and conversion) Map to " + templateType.getName() + ", field \"" + entryKey + "\" convert failed (Error while convert by common way " + valueType.getName() + " to" + getClassNames(entry.getValue()) + "), map data:" + map, t, "java.util.Map", templateType.getName(), entryKey);
//                        }
//                        if (value == null) {
//                            throw new MappingRuntimeException("SlateBeanUtils: Error while pre-mapping (check and conversion) Map to " + templateType.getName() + ", field \"" + entryKey + "\" convert failed (No PropMapper for " + valueType.getName() + " to" + getClassNames(entry.getValue()) + "), map data:" + map, null, "java.util.Map", templateType.getName(), entryKey);
//                        }
                    }
                    result.put(entryKey, value);
                } else if (throwExceptionIfFails){
                    throw new MappingRuntimeException("SlateBeanUtils: Error while pre-mapping (check and conversion) Map to " + templateType.getName() + ", field \"" + entryKey + "\" convert failed (No PropMapper for " + valueType.getName() + " to" + getClassNames(entry.getValue()) + "), map data:" + map, null, "java.util.Map", templateType.getName(), entryKey);
                }
            } catch (MappingRuntimeException e) {
                if (throwExceptionIfFails) {
                    //补上field名
                    String fieldName = BeanMethodNameUtils.methodToField(entryKey);
                    e.setFieldName(fieldName);
                    throw e;
                }
            } catch (Exception e) {
                if (throwExceptionIfFails) {
                    throw new MappingRuntimeException("SlateBeanUtils: Error while pre-mapping (check and conversion) Map to " + templateType.getName() + ", problem property \"" + entryKey + "\" (No PropMapper for " + valueType.getName() + " to" + getClassNames(entry.getValue()) + "), map data:" + map, e, "java.util.Map", templateType.getName(), entryKey);
                }
            }
        }
        return result;
    }

    /*
     * 这个方案不完善, 只能针对子元素mapToBean的情况, 会导致mapToBean与beanToMap/beanOrMapToMapRecursively的转换口径都不同, 容易产生误解
     */
//    private Object commonConvert(Class<?>[] expectTypes, Object value, boolean throwExceptionIfFails){
//        //只有确定类型的情况才尝试通用转换
//        if (expectTypes.length != 1) {
//            return null;
//        }
//        if (value instanceof Map) {
//            BeanizationFactory beanizationFactory = BeanizationFactory.getFactory(expectTypes[0], converter);
//            if (beanizationFactory.getPropertiesSize() > 0) {
//                //Map -> Bean
//                return SlateBeanUtils.mapToBean((Map<String, Object>) value, expectTypes[0], true, throwExceptionIfFails);
//            }
//        }
//        return null;
//    }

    private String getClassNames(Class<?>[] classes) {
        if (classes.length == 1) {
            return " " + classes[0].getName();
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (Class<?> clazz : classes) {
            stringBuilder.append(" ");
            stringBuilder.append(clazz.getSimpleName());
        }
        return stringBuilder.toString();
    }

    int getPropertiesSize() {
        return propertiesSize;
    }

}
