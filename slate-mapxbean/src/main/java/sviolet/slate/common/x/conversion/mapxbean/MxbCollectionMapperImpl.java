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

import com.github.shepherdviolet.glaciion.api.annotation.PropertyInject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sviolet.thistle.util.reflect.GenericClassUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * <p>[Sub Processor] Collection converter (including Map)</p>
 *
 * <p>Purpose: Determine whether it is a collection to collection scenes, and then implement collection conversion logic.
 * Notice that Map is collection here</p>
 *
 * @author S.Violet
 * @see MapXBean
 */
public class MxbCollectionMapperImpl implements MxbCollectionMapper {

    private final Logger logger = LoggerFactory.getLogger(MapXBean.class);

    /**
     * Property inject: Is log enabled
     */
    @PropertyInject(getVmOptionFirst = "slate.mapxbean.log")
    private boolean logEnabled = false;

    /**
     * @inheritDoc
     */
    @Override
    public Object onConvert(Object from,
                            Class<?> fromClass,
                            Class<?> toClass,
                            Type toType,
                            PropertyOrElementConverter elementConverter,
                            MxbObjectInstantiator objectInstantiator,
                            ConversionExceptionThrower exceptionThrower,
                            ConversionPath conversionPath) throws Exception {

        if (Map.class.isAssignableFrom(fromClass) && Map.class.isAssignableFrom(toClass)) {
            //Map
            return convertMap(from, fromClass, toClass, toType, elementConverter, objectInstantiator, exceptionThrower, conversionPath);
        } else if (Collection.class.isAssignableFrom(fromClass) && Collection.class.isAssignableFrom(toClass)) {
            //Collection
            return convertCollection(from, fromClass, toClass, toType, elementConverter, objectInstantiator, exceptionThrower, conversionPath);
        }
        return RESULT_NOT_COLLECTION_TO_COLLECTION;
    }

    /**
     * Map to Map
     */
    public Object convertMap(Object from,
                             Class<?> fromClass,
                             Class<?> toClass,
                             Type toType,
                             PropertyOrElementConverter elementConverter,
                             MxbObjectInstantiator objectInstantiator,
                             ConversionExceptionThrower exceptionThrower,
                             ConversionPath conversionPath) throws Exception {

        // Source Map
        Map<Object, Object> fromMap = (Map<Object, Object>) from;
        // Destination Map
        Map<Object, Object> toMap;

        // Expected element type (Element should convert to this type, get from generic type of Map)
        Class<?> elementKeyClass;
        Class<?> elementValueClass;
        Type elementValueType;

        // Create destination Map
        if (isOrdinaryMap(toClass)) {
            // New ordinary Map, keep order
            toMap = new LinkedHashMap<>(fromMap.size() << 1);
            // Get expected key/value types
            if (toType instanceof ParameterizedType) {
                // Get from generic type
                Type[] typeArguments = ((ParameterizedType) toType).getActualTypeArguments();
                elementKeyClass = GenericClassUtils.typeToRawClass(typeArguments[0]);
                elementValueType = typeArguments[1];
            } else {
                // Default
                elementKeyClass = Object.class;
                elementValueType = Object.class;
            }
        } else {
            // New specified Map
            toMap = (Map<Object, Object>) objectInstantiator.newInstance(toClass, true);
            // Get expected key/value types by seeking generic type of Map
            Map<String, Type> actualTypes = GenericClassUtils.getActualTypes(toClass, Map.class);
            elementKeyClass = GenericClassUtils.typeToRawClass(actualTypes.get("K"));
            elementValueType = actualTypes.get("V");
        }

        elementValueClass = GenericClassUtils.typeToRawClass(elementValueType);

        // Handle each element
        for (Map.Entry<Object, Object> fromEntry : fromMap.entrySet()) {
            Object key = fromEntry.getKey();
            Object value = fromEntry.getValue();

            // Match key type
            if (key != null && !elementKeyClass.isAssignableFrom(key.getClass())) {
                exceptionThrower.throwConversionException("MapXBean: Error while mapping " + fromClass.getName() +
                        " to " + toType.getTypeName() + ", property \"" + key + "\"'s key type mismatch (" +
                        key.getClass().getName() + " to " + elementKeyClass.getName() + "), map data:" + fromMap,
                        null, conversionPath);
                continue;
            }

            // Keep null value, Keep the integrity of the Map
            if (value == null) {
                toMap.put(key, null);
                continue;
            }

            Class<?> valueClass = value.getClass();

            //Create sub ConversionPath for element
            ConversionPath subConversionPath = new ConversionPath(String.valueOf(key), valueClass, elementValueClass, elementValueType, conversionPath);

            // Convert value to expected type
            Object convertedValue;
            try {
                convertedValue = elementConverter.convert(value, valueClass, elementValueClass, elementValueType, subConversionPath);
            } catch (Throwable e) {
                exceptionThrower.throwConversionException("MapXBean: Error while mapping " + fromClass.getName() +
                        " to " + toType.getTypeName() + ", property \"" + key + "\" mapping failed, map data:" +
                        fromMap, e, subConversionPath);
                continue;
            }

            // Put
            toMap.put(key, convertedValue);
        }
        return toMap;
    }

    /**
     * These types are treated as ordinary Map, will convert to LinkedHashMap
     */
    protected boolean isOrdinaryMap(Class<?> toClass) {
        return Map.class.equals(toClass);
    }

    /**
     * Collection to Collection
     */
    public Object convertCollection(Object from,
                                    Class<?> fromClass,
                                    Class<?> toClass,
                                    Type toType,
                                    PropertyOrElementConverter elementConverter,
                                    MxbObjectInstantiator objectInstantiator,
                                    ConversionExceptionThrower exceptionThrower,
                                    ConversionPath conversionPath) throws Exception {

        // Source Collection
        Collection<Object> fromCollection = (Collection<Object>) from;
        // Destination Collection
        Collection<Object> toCollection;

        // Expected element type (Element should convert to this type, get from generic type of Collection)
        Class<?> elementClass;
        Type elementType;

        // Create destination Collection
        if (isOrdinaryList(toClass)) {
            // New ordinary List, keep order
            toCollection = new ArrayList<>(fromCollection.size());
            // Get expected element type
            if (toType instanceof ParameterizedType) {
                // Get from generic type
                Type[] typeArguments = ((ParameterizedType) toType).getActualTypeArguments();
                elementType = typeArguments[0];
            } else {
                // Default
                elementType = Object.class;
            }
        } else if (isOrdinarySet(toClass)) {
            // New ordinary Set, keep order
            toCollection = new LinkedHashSet<>(fromCollection.size() << 1);
            // Get expected element type
            if (toType instanceof ParameterizedType) {
                // Get from generic type
                Type[] typeArguments = ((ParameterizedType) toType).getActualTypeArguments();
                elementType = typeArguments[0];
            } else {
                // Default
                elementType = Object.class;
            }
        } else {
            // New specified Collection
            toCollection = (Collection<Object>) objectInstantiator.newInstance(toClass, true);
            // Get expected element type by seeking generic type of Collection
            Map<String, Type> actualTypes = GenericClassUtils.getActualTypes(toClass, Collection.class);
            elementType = actualTypes.get("E");
        }

        elementClass = GenericClassUtils.typeToRawClass(elementType);

        // Handle each element
        int i = -1;
        for (Object value : fromCollection) {
            //Index of raw collection
            i++;

            // Keep null value, Keep the integrity of the Collection
            if (value == null) {
                toCollection.add(null);
                continue;
            }

            Class<?> valueClass = value.getClass();

            //Create sub ConversionPath for element
            ConversionPath subConversionPath = new ConversionPath("[" + i + "]", valueClass, elementClass, elementType, conversionPath);

            // Convert value to expected type
            Object convertedValue;
            try {
                convertedValue = elementConverter.convert(value, valueClass, elementClass, elementType, subConversionPath);
            } catch (Throwable e) {
                exceptionThrower.throwConversionException("MapXBean: Error while mapping " + fromClass.getName() +
                                " to " + toType.getTypeName() + ", element [" + i + "] mapping failed, collection data:" +
                                from, e, subConversionPath);
                continue;
            }

            // Add
            toCollection.add(convertedValue);
        }
        return toCollection;
    }

    /**
     * These types are treated as ordinary List, will convert to ArrayList
     */
    protected boolean isOrdinaryList(Class<?> toClass) {
        return List.class.equals(toClass) || Collection.class.equals(toClass);
    }

    /**
     * These types are treated as ordinary Set, will convert to LinkedHashSet
     */
    protected boolean isOrdinarySet(Class<?> toClass) {
        return Set.class.equals(toClass);
    }

}
