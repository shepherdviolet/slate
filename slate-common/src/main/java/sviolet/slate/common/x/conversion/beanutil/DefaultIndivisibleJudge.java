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

import sviolet.thistle.util.conversion.PrimitiveUtils;
import sviolet.thistle.x.common.thistlespi.ThistleSpi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * 默认的不可分割类型判断器
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
