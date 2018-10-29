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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import sviolet.slate.common.spring.processor.MemberVisitBeanPostProcessor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <p>自定义Autowired</p>
 *
 * @author S.Violet
 */
class CustomAutowiredBeanPostProcessor extends MemberVisitBeanPostProcessor implements ApplicationContextAware {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Map<Class<? extends Annotation>, List<CustomAutowiredProcessor>> processors;
    private ApplicationContext applicationContext;

    CustomAutowiredBeanPostProcessor(List<AnnotationAttributes> annotationAttributesList) {
        processors = new HashMap<>(annotationAttributesList.size());
        for (AnnotationAttributes annotationAttributes : annotationAttributesList) {
            Class<? extends Annotation> annotationClass = annotationAttributes.getClass("annotation");
            Class<? extends CustomAutowiredProcessor> processorClass = annotationAttributes.getClass("processor");
            CustomAutowiredProcessor processor;
            try {
                processor = processorClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("CustAutowired | Illegal processor " + processorClass.getName() + ", annotation: " + annotationClass.getName(), e);
            }
            List<CustomAutowiredProcessor> processorList = processors.get(annotationClass);
            if (processorList == null) {
                processorList = new LinkedList<>();
                processors.put(annotationClass, processorList);
            }
            processorList.add(processor);
        }
    }

    @Override
    protected void visitField(Object bean, String beanName, Field field) {
        Annotation[] annotations = AnnotationUtils.getAnnotations(field);
        for (Annotation annotation : annotations) {
            List<CustomAutowiredProcessor> processorList = processors.get(annotation.annotationType());
            if (processorList != null) {
                for (CustomAutowiredProcessor processor : processorList) {
                    processor.visitField(bean, beanName, field, annotation);
                }
            }
        }
    }

    @Override
    protected void visitMethod(Object bean, String beanName, Method method) {
        Annotation[] annotations = AnnotationUtils.getAnnotations(method);
        for (Annotation annotation : annotations) {
            List<CustomAutowiredProcessor> processorList = processors.get(annotation.annotationType());
            if (processorList != null) {
                for (CustomAutowiredProcessor processor : processorList) {
                    processor.visitMethod(bean, beanName, method, annotation);
                }
            }
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
