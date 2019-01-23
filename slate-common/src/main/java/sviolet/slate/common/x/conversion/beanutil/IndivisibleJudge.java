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

import com.github.shepherdviolet.glaciion.api.annotation.SingleServiceInterface;

import java.util.Map;

/**
 * <p>SlateBeanUtils Bean工具的扩展点</p>
 *
 * <p>不可分割类型判断器</p>
 *
 * <p>实现:判断一个元素是否不可分割, 用于beanOrMapToMapRecursively</p>
 *
 * <p>使用扩展点之前, 请先仔细阅读文档: https://github.com/shepherdviolet/thistle/blob/master/docs/thistlespi/guide.md</p>
 *
 * @see SlateBeanUtils
 * @author S.Violet
 */
@SingleServiceInterface
public interface IndivisibleJudge {

    /**
     * 实现: 判断元素是否不可分割
     * @param obj 待判断的对象
     * @param extraIndivisibleTypes 额外的不可分割类型
     */
    boolean isIndivisible(Object obj, Map<Class<?>, JudgeType> extraIndivisibleTypes);

    /**
     * 判断方式
     */
    enum JudgeType {

        /**
         * 使用isAssignableFrom判断
         */
        IS_ASSIGNABLE_FROM,

        /**
         * 使用equals判断
         */
        EQUALS

    }

}
