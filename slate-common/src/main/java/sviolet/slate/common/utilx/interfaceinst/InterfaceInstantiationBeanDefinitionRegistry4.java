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

package sviolet.slate.common.utilx.interfaceinst;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>[JDK8- Spring 5-]</p>
 *
 * <p>核心逻辑, 适配低版本</p>
 *
 * @author S.Violet
 */
class InterfaceInstantiationBeanDefinitionRegistry4 implements BeanDefinitionRegistryPostProcessor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private List<AnnotationAttributes> annotationAttributesList;

    InterfaceInstantiationBeanDefinitionRegistry4(List<AnnotationAttributes> annotationAttributesList) {
        this.annotationAttributesList = annotationAttributesList;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        logger.info("InterfaceInstantiation: -------------------------------------------------");
        logger.info("InterfaceInstantiation: start (spring 5- or jdk 8-)");

        Set<String> processedClasses = new HashSet<>(128);

        for (AnnotationAttributes annotationAttributes : annotationAttributesList) {

            logger.info("InterfaceInstantiation: -------------------------------------------------");

            //接口类实例化器
            InterfaceInstantiator interfaceInstantiator;
            try {
                Class<? extends InterfaceInstantiator> interfaceInstantiatorClass = annotationAttributes.getClass("interfaceInstantiator");
                interfaceInstantiator = interfaceInstantiatorClass.newInstance();
                logger.info("InterfaceInstantiation: interfaceInstantiator:" + interfaceInstantiatorClass.getName());
            } catch (Exception e) {
                throw new FatalBeanException("InterfaceInstantiation: interfaceInstantiator create failed", e);
            }
            final InterfaceInstantiator interfaceInstantiatorFinal = interfaceInstantiator;

            //指定的注解类型
            Class<? extends Annotation> annotationClass = annotationAttributes.getClass("annotationClass");

            //接口搜索器
            ClassPathScanningCandidateComponentProvider beanScanner = new ClassPathScanningCandidateComponentProvider(false) {
                @Override
                protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                    // access interface
                    return beanDefinition.getMetadata().isInterface();
                }
            };

            //根据注解过滤
            if (annotationAttributes.getBoolean("annotationRequired")) {
                TypeFilter includeFilter = new AnnotationTypeFilter(annotationClass);
                beanScanner.addIncludeFilter(includeFilter);
            } else {
                beanScanner.addIncludeFilter(new TypeFilter() {
                    @Override
                    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
                        return true;
                    }
                });
            }

            //包路径
            String[] basePackages = annotationAttributes.getStringArray("basePackages");

            if (basePackages == null || basePackages.length <= 0) {
                logger.info("InterfaceInstantiation: skip, no basePackages");
                return;
            }

            //遍历包路径
            for (String basePackage : basePackages) {

                //搜索包路径下的接口类定义
                logger.info("InterfaceInstantiation: scan package:" + basePackage);
                Set<BeanDefinition> beanDefinitions = beanScanner.findCandidateComponents(basePackage);

                if (beanDefinitions == null || beanDefinitions.size() <= 0) {
                    continue;
                }

                //遍历接口类定义
                for (BeanDefinition beanDefinition : beanDefinitions) {

                    //类名
                    String className = beanDefinition.getBeanClassName();
                    //Bean名
                    String beanName = interfaceInstantiator.resolveBeanName(className);

                    //跳过已实例化的接口
                    if (processedClasses.contains(className)) {
                        logger.warn("InterfaceInstantiation: duplicate class(skipped):" + className);
                        continue;
                    }

                    try {

                        //类
                        final Class clazz = Class.forName(className);

                        //FactoryBean的构造参数
                        ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
                        constructorArgumentValues.addIndexedArgumentValue(0, clazz);
                        constructorArgumentValues.addIndexedArgumentValue(1, interfaceInstantiator);

                        //Bean定义
                        RootBeanDefinition rootBeanDefinition = new RootBeanDefinition(InterfaceInstantiationFactoryBean4.class, constructorArgumentValues, null);

                        //注册Bean定义
                        registry.registerBeanDefinition(beanName, rootBeanDefinition);

                        //记录类名
                        processedClasses.add(className);

                        logger.info("InterfaceInstantiation: created:" + className);

                    } catch (ClassNotFoundException e) {
                        throw new FatalBeanException("InterfaceInstantiation: interface class not found:" + className, e);
                    }

                }

            }

        }

        logger.info("InterfaceInstantiation: -------------------------------------------------");
        logger.info("InterfaceInstantiation: finish");
        logger.info("InterfaceInstantiation: -------------------------------------------------");

    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        //do nothing
    }

}
