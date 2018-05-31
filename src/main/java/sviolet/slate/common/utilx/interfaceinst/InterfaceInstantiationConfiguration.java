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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

/**
 * <p>[JDK8 + Spring 5.0]</p>
 * <p>配置类</p>
 * @since 1.8
 * @author S.Violet
 */
@Configuration
public class InterfaceInstantiationConfiguration {

    /**
     * 不实现ImportAware#setImportMetadata方法获取EnableInterfaceInstantiation注解参数的原因:
     * 因为BeanDefinitionRegistryPostProcessor需要在spring启动初期实例化(配置了ROLE_INFRASTRUCTURE), 因此该方法必须为静态(static),
     * 无法通过ImportAware注入注解参数. 如果该方法不为静态(static), 会导致该类过早实例化(即使这样也无法通过ImportAware注入注解参数).
     * 因此, 只能在InterfaceInstantiationSelector中获取并持有注解参数(静态持有).
     */
    @Bean(name = "slate.common.InterfaceInstantiationBeanDefinitionRegistry")
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static BeanDefinitionRegistryPostProcessor interfaceInstantiationBeanDefinitionRegistry(){
        return new InterfaceInstantiationBeanDefinitionRegistry(InterfaceInstantiationSelector.annotationAttributes);
    }

//    /**
//     * 不实现ImportAware#setImportMetadata方法获取EnableInterfaceInstantiation注解参数的原因:
//     * 因为BeanDefinitionRegistryPostProcessor需要在spring启动初期实例化(配置了ROLE_INFRASTRUCTURE), 因此该方法必须为静态(static),
//     * 无法通过ImportAware注入注解参数. 如果该方法不为静态(static), 会导致该类过早实例化(即使这样也无法通过ImportAware注入注解参数).
//     * 因此, 只能在InterfaceInstantiationSelector中获取并持有注解参数(静态持有).
//     */
//    @Override
//    public void setImportMetadata(AnnotationMetadata importMetadata) {
//        this.annotationAttributes = AnnotationAttributes.fromMap(
//                importMetadata.getAnnotationAttributes(EnableInterfaceInstantiation.class.getName(), false));
//        if (this.annotationAttributes == null) {
//            throw new IllegalArgumentException(
//                    "@EnableInterfaceInstantiation is not present on importing class " + importMetadata.getClassName());
//        }
//    }

}
