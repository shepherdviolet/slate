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

import com.github.shepherdviolet.glaciion.api.annotation.MultipleServiceInterface;
import org.slf4j.Logger;

/**
 * <p>SlateBeanUtils 默认参数类型转换器DefaultBeanConverter的扩展点, 在BeanConverter未被替换实现时有效</p>
 *
 * <p>实现:声明自身能处理什么类型的数据, 实现对应的类型转换逻辑</p>
 *
 * <p>使用扩展点之前, 请先仔细阅读文档: https://github.com/shepherdviolet/glaciion/blob/master/docs/index.md</p>
 *
 * @see DefaultBeanConverter
 * @see SlateBeanUtils
 * @author S.Violet
 */
@MultipleServiceInterface
public interface PropMapper {

    /**
     * 类型转换, 将源类型的数据转换成目的类型
     * @param from 源类型的对象
     * @param toType 目的类型
     * @return 目的类型的对象, 若转换失败, 可以抛出MappingRuntimeException异常或返回null
     */
    Object map(Object from, Class<?> toType, Logger logger, boolean logEnabled);

    /**
     * @return 处理的源类型
     */
    Class<?>[] fromType();

    /**
     * @return 处理的目的类型
     */
    Class<?>[] toType();

}
