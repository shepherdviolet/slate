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

import org.springframework.cglib.core.Converter;
import sviolet.thistle.util.conversion.BeanMethodNameUtils;
import sviolet.thistle.util.conversion.PrimitiveUtils;

/**
 * <p>SlateBeanUtils Bean工具的扩展点</p>
 *
 * <p>Bean参数类型转换器</p>
 *
 * <p>实现:将参数的类型转换为指定类型, 用于mapToMap / mapToBean / mapBeanization</p>
 *
 * <p>使用扩展点之前, 请先仔细阅读文档: https://github.com/shepherdviolet/thistle/blob/master/docs/thistlespi/guide.md</p>
 *
 * @see SlateBeanUtils
 * @author S.Violet
 */
public abstract class BeanConverter implements Converter {

    @Override
    public final Object convert(Object from, Class toType, Object setMethodName) {
        try {
            return onConvert(Cause.BEAN_TO_BEAN, from, new Class[]{PrimitiveUtils.toWrapperType(toType)});
        } catch (MappingRuntimeException e) {
            //补上field名
            String fieldName = BeanMethodNameUtils.methodToField(String.valueOf(setMethodName));
            e.setFieldName(fieldName);
            throw e;
        }
    }

    /**
     * 实现类型转换
     * @param cause 转换类型
     * @param from 待转换参数
     * @param toTypes 目的类型(符合其中的一个类型即可)
     * @return 转换后的参数, 转换失败返回null
     */
    protected abstract Object onConvert(Cause cause, Object from, Class[] toTypes);

    public enum Cause {
        /**
         * 由SlateBeanUtils.beanToBean触发
         */
        BEAN_TO_BEAN,

        /**
         * 由SlateBeanUtils.fromMap和mapBeanization触发
         */
        BEANIZATION
    }

}
