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

import com.github.shepherdviolet.glaciion.api.annotation.PropertyInject;
import sviolet.thistle.util.conversion.PrimitiveUtils;
import sviolet.thistle.util.conversion.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认的不可分割类型判断器
 *
 * @author S.Violet
 */
public class DefaultIndivisibleJudge implements IndivisibleJudge {

    private Map<Class<?>, JudgeType> indivisibleTypes = new HashMap<>();

    @PropertyInject
    public void setTypes(String types) {
        List<String> typeList = StringUtils.splitAndTrim(types, ",");
        for (String type : typeList) {
            try {
                indivisibleTypes.put(Class.forName(type), JudgeType.EQUALS);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Invalid property 'types': " + type, e);
            }
        }
    }

    @PropertyInject
    public void setTypeGroups(String types) {
        List<String> typeList = StringUtils.splitAndTrim(types, ",");
        for (String type : typeList) {
            try {
                indivisibleTypes.put(Class.forName(type), JudgeType.IS_ASSIGNABLE_FROM);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Invalid property 'typeGroups': " + type, e);
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
