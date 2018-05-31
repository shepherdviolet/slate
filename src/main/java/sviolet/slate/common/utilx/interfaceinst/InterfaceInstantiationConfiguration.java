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
import org.springframework.context.annotation.ImportAware;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * [JDK8 + Spring 5.0]
 * @since 1.8
 * @author S.Violet
 */
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class InterfaceInstantiationConfiguration implements ImportAware {

    private AnnotationAttributes annotationAttributes;

    @Bean(name = "slate.common.InterfaceInstantiationBeanDefinitionRegistry")
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public BeanDefinitionRegistryPostProcessor interfaceInstantiationBeanDefinitionRegistry(){
        return new InterfaceInstantiationBeanDefinitionRegistry(annotationAttributes);
    }

    /**
     * 获得EnableInterfaceInstantiation注解的参数
     */
    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        this.annotationAttributes = AnnotationAttributes.fromMap(
                importMetadata.getAnnotationAttributes(EnableInterfaceInstantiation.class.getName(), false));
        if (this.annotationAttributes == null) {
            throw new IllegalArgumentException(
                    "@EnableInterfaceInstantiation is not present on importing class " + importMetadata.getClassName());
        }
    }

}
