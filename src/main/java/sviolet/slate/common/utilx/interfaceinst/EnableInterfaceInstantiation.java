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
 * Project GitHub: https://github.com/shepherdviolet/slate-common
 * Email: shepherdviolet@163.com
 */

package sviolet.slate.common.utilx.interfaceinst;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * [JDK8 + Spring 5.0]
 * @since 1.8
 * @author S.Violet
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({InterfaceInstantiationSelector.class})
public @interface EnableInterfaceInstantiation {

    /**
     * 自定义参数:
     * 配置需要实例化的接口类包路径
     */
    String[] basePackages();

    /**
     * 自定义参数:
     * 配置接口类实例化工具
     */
    Class<? extends InterfaceInstantiator> interfaceInstantiator() default DefaultInterfaceInstantiator.class;

}