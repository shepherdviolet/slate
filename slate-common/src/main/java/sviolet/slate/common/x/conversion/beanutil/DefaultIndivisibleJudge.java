package sviolet.slate.common.x.conversion.beanutil;

import sviolet.thistle.util.conversion.PrimitiveUtils;
import sviolet.thistle.x.common.thistlespi.ThistleSpi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * 默认的不可分割元素判断器
 *
 * @author S.Violet
 */
public class DefaultIndivisibleJudge implements IndivisibleJudge {

    private Map<Class<?>, JudgeType> indivisibleTypes;

    public DefaultIndivisibleJudge(Properties parameter) {
        if (parameter == null) {
            return;
        }
        String propertiesUrl = (String) parameter.remove(ThistleSpi.PROPERTIES_URL);
        if (parameter.size() <= 0) {
            return;
        }
        indivisibleTypes = new LinkedHashMap<>(parameter.size());
        ClassLoader classLoader = getClass().getClassLoader();
        for (Object key : parameter.keySet()) {
            JudgeType judgeType = JudgeType.parse((String)parameter.get(key));
            if (judgeType == null) {
                throw new RuntimeException("Invalid constructor parameter '" + key + "=" + parameter.get(key) + "', illegal JudgeType (value) should be isAssignableFrom or equals, properties url:" + propertiesUrl);
            }
            try {
                indivisibleTypes.put(classLoader.loadClass((String)key), judgeType);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Invalid constructor parameter '" + key + "=" + parameter.get(key) + "', class not found, properties url:" + propertiesUrl, e);
            }
        }
    }

    @Override
    public boolean isIndivisible(Object obj, Map<Class<?>, JudgeType> extraIndivisibleTypes) {
        if (obj == null) {
            return true;
        }
        Class type = obj.getClass();

        if (PrimitiveUtils.isPrimitiveOrWrapper(type) ||
                type.isEnum() ||
                type.isArray() ||
                Object.class.equals(type) ||
                String.class.isAssignableFrom(type) ||
                BigDecimal.class.isAssignableFrom(type) ||
                BigInteger.class.isAssignableFrom(type) ||
                Date.class.isAssignableFrom(type)) {
            return true;
        }

        return handleWithProperties(type, indivisibleTypes) ||
                handleWithProperties(type, extraIndivisibleTypes);
    }

    private boolean handleWithProperties(Class type, Map<Class<?>, JudgeType> indivisibleTypes) {
        if (indivisibleTypes != null) {
            for (Map.Entry<Class<?>, JudgeType> entry : indivisibleTypes.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                if (JudgeType.IS_ASSIGNABLE_FROM.equals(entry.getValue())) {
                    if (entry.getKey().isAssignableFrom(type)) {
                        return true;
                    }
                } else {
                    if (entry.getKey().equals(type)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
