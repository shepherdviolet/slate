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
 * [JDK8 + Spring 5.0]
 * @since 1.8
 * @author S.Violet
 */
@Configuration
public class InterfaceInstantiationConfiguration {

    /**
     * 因为BeanDefinitionRegistryPostProcessor需要在早期实例化, 因此需要将方法标记为static,
     * 可能也是因为这个原因, 所以AnnotationMetadata拿不到, 只能用静态变量
     */
    @Bean(name = "slate.common.InterfaceInstantiationBeanDefinitionRegistry")
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static BeanDefinitionRegistryPostProcessor interfaceInstantiationBeanDefinitionRegistry(){
        return new InterfaceInstantiationBeanDefinitionRegistry(InterfaceInstantiationSelector.annotationAttributes);
    }

//    /**
//     * 不知道为什么InterfaceInstantiationConfiguration实现了ImportAware还是获取不到AnnotationMetadata, 只能静态变量了
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
