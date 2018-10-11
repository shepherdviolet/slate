package sviolet.slate.common.x.conversion.beanutil;

import sviolet.thistle.util.conversion.BeanMethodNameUtils;
import sviolet.thistle.util.judge.CheckUtils;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Bean矫正器
 *
 * @author S.Violet
 */
class BeanizationFactory {

    private Class<?> templateType;
    private BeanConverter converter;

    private Map<String, Class[]> templateProperties;

    BeanizationFactory(Class<?> templateType, BeanConverter converter) {
        this.templateType = templateType;
        this.converter = converter;
        init();
    }

    private void init(){
        if (templateType == null) {
            throw new NullPointerException("templateType is null");
        }
        if (converter == null) {
            throw new NullPointerException("converter is null");
        }
        Map<String, Set<Class>> properties = new HashMap<>();
        Map<String, Class> definiteProperties = new HashMap<>();
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
                    if (paramTypes.length != 1) {
                        continue;
                    }
                    Set<Class> classSet = properties.get(fieldName);
                    if (classSet == null) {
                        classSet = new HashSet<>();
                        properties.put(fieldName, classSet);
                    }
                    classSet.add(paramTypes[0]);
                } else if (methodName.startsWith("get") || methodName.startsWith("is")) {
                    if (paramTypes.length != 0) {
                        continue;
                    }
                    if (void.class.isAssignableFrom(method.getReturnType())) {
                        continue;
                    }
                    definiteProperties.put(fieldName, method.getReturnType());
                }
            }
        }
        for (Map.Entry<String, Class> entry : definiteProperties.entrySet()) {
            Set<Class> classSet = new HashSet<>(1);
            classSet.add(entry.getValue());
            properties.put(entry.getKey(), classSet);
        }
        this.templateProperties = new HashMap<>(properties.size());
        for (Map.Entry<String, Set<Class>> entry : properties.entrySet()) {
            Class[] classArray = new Class[entry.getValue().size()];
            this.templateProperties.put(entry.getKey(), entry.getValue().toArray(classArray));
        }
    }

    Map<String, Object> beanization(Map<String, Object> map, boolean convert){
        Map<String, Object> result = new HashMap<>(templateProperties.size());
        for (Map.Entry<String, Class[]> entry : templateProperties.entrySet()) {
            String entryKey = entry.getKey();
            Object value = map.get(entryKey);
            if (value == null) {
                continue;
            }
            Class<?> valueType = value.getClass();
            try {
                boolean found = false;
                for (Class<?> type : entry.getValue()) {
                    if (type.isAssignableFrom(valueType)) {
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
                //fallback
                if (convert) {
                    value = converter.onConvert(BeanConverter.Cause.BEANIZATION, value, entry.getValue());
                    if (value == null) {
                        throw new MappingRuntimeException("SlateBeanUtils: Error while pre-mapping (check and conversion) Map to " + templateType.getName() + ", field \"" + entryKey + "\" convert failed (No PropMapper for " + valueType.getName() + " to" + getClassNames(entry.getValue()) + "), map data:" + map, null, "java.util.Map", templateType.getName(), entryKey);
                    }
                    result.put(entryKey, value);
                } else {
                    throw new MappingRuntimeException("SlateBeanUtils: Error while pre-mapping (check and conversion) Map to " + templateType.getName() + ", field \"" + entryKey + "\" convert failed (No PropMapper for " + valueType.getName() + " to" + getClassNames(entry.getValue()) + "), map data:" + map, null, "java.util.Map", templateType.getName(), entryKey);
                }
            } catch (MappingRuntimeException e) {
                //补上field名
                String fieldName = BeanMethodNameUtils.methodToField(entryKey);
                e.setFieldName(fieldName);
                throw e;
            } catch (Exception e) {
                throw new MappingRuntimeException("SlateBeanUtils: Error while pre-mapping (check and conversion) Map to " + templateType.getName() + ", problem property \"" + entryKey + "\" (No PropMapper for " + valueType.getName() + " to" + getClassNames(entry.getValue()) + "), map data:" + map, e, "java.util.Map", templateType.getName(), entryKey);
            }
        }
        return result;
    }

    private String getClassNames(Class[] classes) {
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

}
