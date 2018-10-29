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

package sviolet.slate.common.x.common.custautowired;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

/**
 * <p>配置类</p>
 *
 * @author S.Violet
 */
@Configuration
public class CustomAutowiredConfig {

    @Bean("slate.common.customAutowiredBeanPostProcessor")
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static BeanPostProcessor customAutowiredBeanPostProcessor(){
        return new CustomAutowiredBeanPostProcessor(CustomAutowiredSelector.annotationAttributesList);
    }

}
