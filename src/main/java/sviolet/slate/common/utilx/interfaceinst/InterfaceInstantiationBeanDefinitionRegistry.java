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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

import java.util.Set;
import java.util.function.Supplier;

/**
 * [JDK8 + Spring 5.0]
 * @since 1.8
 * @author S.Violet
 */
class InterfaceInstantiationBeanDefinitionRegistry implements BeanDefinitionRegistryPostProcessor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private AnnotationAttributes annotationAttributes;

    InterfaceInstantiationBeanDefinitionRegistry(AnnotationAttributes annotationAttributes) {
        this.annotationAttributes = annotationAttributes;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        logger.info("InterfaceInstantiation: start");

        //接口类实例化工具
        InterfaceInstantiator interfaceInstantiator;
        try {
            Class<? extends InterfaceInstantiator> interfaceInstantiatorClass = annotationAttributes.getClass("interfaceInstantiator");
            interfaceInstantiator = interfaceInstantiatorClass.newInstance();
            logger.info("InterfaceInstantiation: interfaceInstantiator:" + interfaceInstantiatorClass.getName());
        } catch (Exception e) {
            throw new FatalBeanException("InterfaceInstantiation: interfaceInstantiator create failed", e);
        }
        final InterfaceInstantiator interfaceInstantiatorFinal = interfaceInstantiator;

        //接口搜索器
        ClassPathScanningCandidateComponentProvider beanScanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                // access interface
                return beanDefinition.getMetadata().isInterface();
            }
        };
        //过滤注解
        TypeFilter includeFilter = new AnnotationTypeFilter(InterfaceInstance.class);
        beanScanner.addIncludeFilter(includeFilter);

        //遍历包路径
        String[] basePackages = annotationAttributes.getStringArray("basePackages");

        if (basePackages == null || basePackages.length <= 0) {
            logger.info("InterfaceInstantiation: skip, no basePackages");
            return;
        }

        for (String basePackage : basePackages) {

            logger.debug("InterfaceInstantiation: scan package:" + basePackage);
            Set<BeanDefinition> beanDefinitions = beanScanner.findCandidateComponents(basePackage);

            if (beanDefinitions == null || beanDefinitions.size() <= 0) {
                continue;
            }

            for (BeanDefinition beanDefinition : beanDefinitions) {

                //类名
                String className = beanDefinition.getBeanClassName();

                try {

                    //类
                    final Class clazz = Class.forName(className);

                    //定义Bean
                    BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(clazz, new Supplier<Object>() {
                        @Override
                        public Object get() {
                            return interfaceInstantiatorFinal.newInstance(clazz);
                        }
                    });

                    //注册
                    registry.registerBeanDefinition(className, beanDefinitionBuilder.getBeanDefinition());

                    logger.debug("InterfaceInstantiation: interface instantiated:" + className);

                } catch (ClassNotFoundException e) {
                    throw new FatalBeanException("InterfaceInstantiation: interface class not found:" + className, e);
                }

            }

        }

        logger.info("InterfaceInstantiation finish");

    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

}
