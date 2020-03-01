/*
 * Copyright (C) 2015-2020 S.Violet
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

package sviolet.slate.common.x.conversion.mapxbean;

import org.springframework.cglib.beans.BeanMap;
import sviolet.slate.common.x.conversion.mapxbean.strategy.InflateUntilIndivisible;
import sviolet.thistle.util.reflect.BeanInfoUtils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static sviolet.slate.common.x.conversion.mapxbean.MxbConstants.*;

/**
 * [Main processor] Bean -> Map
 *
 * @author S.Violet
 * @see MapXBean
 */
public class BeanToMapConverterImpl implements BeanToMapConverter, ConversionExceptionThrower {

    private final boolean throwExceptionIfFails;
    private final ConversionExceptionCollector exceptionCollector;
    private final BeanToMapInflateStrategy inflateStrategy;

    private BeanToMapConverterImpl(boolean throwExceptionIfFails, ConversionExceptionCollector exceptionCollector, BeanToMapInflateStrategy inflateStrategy) {
        this.throwExceptionIfFails = throwExceptionIfFails;
        this.exceptionCollector = !throwExceptionIfFails ? exceptionCollector : null;
        this.inflateStrategy = inflateStrategy;
    }

    /**
     * @inheritDoc
     */
    @Override
    public Map<String, Object> convert(Object fromBean) {
        Class<?> fromType = fromBean != null ? fromBean.getClass() : null;
        try {
            if (exceptionCollector != null) {
                exceptionCollector.onStart(fromBean, HashMap.class);
            }
            return convert0(fromBean, new ConversionPath(null, fromType, HashMap.class, HashMap.class, null));
        } finally {
            if (exceptionCollector != null) {
                exceptionCollector.onFinish(fromBean, HashMap.class);
            }
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public void convert(Object fromBean, Map<String, Object> toMap) {
        Class<?> fromType = fromBean != null ? fromBean.getClass() : null;
        Class<?> toType = toMap != null ? toMap.getClass() : null;
        try {
            if (exceptionCollector != null) {
                exceptionCollector.onStart(fromBean, toType);
            }
            convert0(fromBean, toMap, new ConversionPath(null, fromType, toType, toType, null));
        } finally {
            if (exceptionCollector != null) {
                exceptionCollector.onFinish(fromBean, toType);
            }
        }
    }

    /**
     * Create instance and convert
     */
    private Map<String, Object> convert0(Object fromBean, ConversionPath conversionPath) {
        Map<String, Object> toMap = new HashMap<>();
        convert0(fromBean, toMap, conversionPath);
        return toMap;
    }

    /**
     * Convert
     */
    public void convert0(Object fromBean, Map<String, Object> toMap, ConversionPath conversionPath){
        if (fromBean == null || toMap == null) {
            return;
        }
        //Create BeanMap of fromBean, and get propertyInfos of fromBean
        BeanMap fromBeanMap;
        Map<String, BeanInfoUtils.PropertyInfo> fromBeanPropertyInfos;
        try {
            fromBeanMap = BeanMap.create(fromBean);
            fromBeanPropertyInfos = BeanInfoUtils.getPropertyInfos(fromBean.getClass());
        } catch (Throwable e) {
            throwConversionException("MapXBean: Error while mapping " + fromBean.getClass().getName() + " to Map" +
                    ", bean data:" + fromBean, e, conversionPath);
            return;
        }
        //Handle all properties in fromBean
        for (Object keyObj : fromBeanMap.keySet()) {
            //K
            String key = String.valueOf(keyObj);

            //Property info
            BeanInfoUtils.PropertyInfo fromBeanPropertyInfo = fromBeanPropertyInfos.get(key);
            //Check read method
            if (fromBeanPropertyInfo.getReadMethod() == null) {
                continue;
            }

            Object value = fromBeanMap.get(keyObj);
            //Keep null value
            if (value == null) {
                toMap.put(key, null);
                continue;
            }

            //From this type (to this type too)
            Class<?> valueClass = value.getClass();
            Type valueType = fromBeanPropertyInfo.getPropertyType();

            //Create sub ConversionPath for property
            //NO type mapping case in Bean -> Map scene. So the source type and destination type are the same
            ConversionPath subConversionPath = new ConversionPath(key, valueClass, valueClass, valueType, conversionPath);

            //Convert property
            Object convertedValue;
            try {
                //NO type mapping case in Bean -> Map scene. So the source type and destination type are the same
                convertedValue = propertyConverter.convert(value, valueClass, valueClass, valueType, subConversionPath);
            } catch (Throwable e) {
                throwConversionException("MapXBean: Error while mapping " + fromBean.getClass().getName() +
                        " to Map, property \"" + key + "\" mapping failed, bean data:" + fromBean, e, subConversionPath);
                continue;
            }
            //Skip if null
            if (convertedValue == null) {
                continue;
            }

            //Put into map
            toMap.put(key, convertedValue);
        }
    }

    /**
     * Convert property of Bean.
     * This method is independent, so that it can be called in the sub processor
     */
    private final PropertyOrElementConverter propertyConverter = new PropertyOrElementConverter() {
        @Override
        public Object convert(Object value, Class<?> valueClass, Class<?> expectClass, Type expectType, ConversionPath conversionPath) throws Exception {
            //valueClass == expectClass here

            // 1> Collection to collection ////////////////////////////////////////////////////////////////////////

            Object convertedValue;
            try {
                //NO type mapping case in Bean -> Map scene. So the source type and destination type are the same
                convertedValue = MTB_COLLECTION_MAPPER.onConvert(value, valueClass, expectClass, expectType, this, OBJECT_INSTANTIATOR, BeanToMapConverterImpl.this, conversionPath);
            } catch (Throwable e) {
                throw new ConversionRuntimeException("Property convert failed, Convert from " + valueClass.getName() +
                        " to " + expectType.getTypeName() + ", in collection to collection mode", e, conversionPath);
            }
            if (convertedValue == null) {
                // Is collection to collection, but value treated as null
                return null;
            }
            if (convertedValue != MxbCollectionMapper.RESULT_NOT_COLLECTION_TO_COLLECTION) {
                //Convert succeed
                return convertedValue;
            }

            // 4> Bean to Map ///////////////////////////////////////////////////////////////////////////////////

            //If it is a readable Bean, and the inflateStrategy is not null
            if (inflateStrategy != null &&
                    !Map.class.isAssignableFrom(valueClass) &&
                    TYPE_JUDGER.isBean(valueClass, true, false)) {
                //Judge by inflateStrategy
                if (inflateStrategy.needToBeInflated(value, valueClass, TYPE_JUDGER, conversionPath)) {
                    try {
                        // Mark as inflated node
                        conversionPath.setInflated(true);
                        // Recursive call
                        return BeanToMapConverterImpl.this.convert0(value, conversionPath);
                    } catch (Throwable e) {
                        throw new ConversionRuntimeException("Property convert failed, Convert from " + valueClass.getName() +
                                " to Map, in inflate mode (bean to map)", e, conversionPath);
                    }
                }
            }

            //Put value to Map directly
            return value;
        }

    };

    /**
     * @inheritDoc
     */
    @Override
    public void throwConversionException(String message, Throwable cause, ConversionPath conversionPath) throws ConversionRuntimeException {
        if (throwExceptionIfFails) {
            throw new ConversionRuntimeException(message, cause, conversionPath);
        } else if (exceptionCollector != null) {
            exceptionCollector.onException(new ConversionRuntimeException(message, cause, conversionPath));
        }
    }

    /**
     * Builder
     */
    public static class Builder {

        private boolean throwExceptionIfFails = false;
        private ConversionExceptionCollector exceptionCollector;
        private BeanToMapInflateStrategy inflateStrategy;

        Builder() {
        }

        /**
         * Whether to throw an exception when the field mapping fails
         *
         * @param throwExceptionIfFails true: Throw exception false: Field left null (default)
         */
        public Builder throwExceptionIfFails(boolean throwExceptionIfFails) {
            this.throwExceptionIfFails = throwExceptionIfFails;
            return this;
        }

        public Builder setThrowExceptionIfFails(boolean throwExceptionIfFails) {
            this.throwExceptionIfFails = throwExceptionIfFails;
            return this;
        }

        /**
         * Exception collector.
         * Only valid when throwExceptionIfFails = false (that is, "error skip" mode).
         * Used to collect exceptions that were ignored during the conversion process (can be used to print logs and troubleshooting).
         * *
         * @param exceptionCollector Exception collector. null by default.
         */
        public Builder exceptionCollector(ConversionExceptionCollector exceptionCollector) {
            this.exceptionCollector = exceptionCollector;
            return this;
        }

        public Builder setExceptionCollector(ConversionExceptionCollector exceptionCollector) {
            this.exceptionCollector = exceptionCollector;
            return this;
        }

        /**
         * <p>Decide whether to continue inflating property to Map</p><br>
         *
         * <p>'Inflate' means convert a property of Java Bean (or element of Collection) to a Map.
         * In Bean -> Map scene, all the properties of Java Bean will keep the original type by default,
         * unless BeanToMapInflateStrategy tells the program that it needs to be inflated (i.e. the method returns true)</p><br>
         *
         * <p>If there is no {@link BeanToMapInflateStrategy}, Bean's properties will be put directly to a Map,
         * which is equivalent to shallow cloning.</p><br>
         *
         * <p>{@link InflateUntilIndivisible} can help you converting a 'Java Bean' to 'Map consisting of Map and
         * Collection nesting', all the properties or elements will be inflate until indivisible (Decide by
         * {@link InflateUntilIndivisible}). </p><br>
         *
         * <p>You can customize a {@link BeanToMapInflateStrategy}, to decide whether to continue inflating property to Map</p><br>
         */
        public Builder inflateStrategy(BeanToMapInflateStrategy inflateStrategy) {
            this.inflateStrategy = inflateStrategy;
            return this;
        }

        public Builder setInflateStrategy(BeanToMapInflateStrategy inflateStrategy) {
            this.inflateStrategy = inflateStrategy;
            return this;
        }

        public BeanToMapConverter build(){
            return new BeanToMapConverterImpl(throwExceptionIfFails, exceptionCollector, inflateStrategy);
        }

    }

}
