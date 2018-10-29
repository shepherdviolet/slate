package sviolet.slate.common.spring.processor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * [BeanPostProcessor]对每个Bean的成员进行访问
 *
 * @author S.Violet
 */
public abstract class MemberVisitBeanPostProcessor implements BeanPostProcessor, PriorityOrdered {

    @Override
    public final Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
        Class clazz = bean.getClass();
        ReflectionUtils.doWithFields(clazz, new ReflectionUtils.FieldCallback() {
            @Override
            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                visitField(bean, beanName, field);
            }
        });
        ReflectionUtils.doWithMethods(clazz, new ReflectionUtils.MethodCallback() {
            @Override
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                visitMethod(bean, beanName, method);
            }
        });
        return bean;
    }

    @Override
    public final Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * visit field of bean
     * @param bean bean
     * @param beanName bean name
     * @param field field
     */
    protected abstract void visitField(Object bean, String beanName, Field field);

    /**
     * visit method of bean
     * @param bean bean
     * @param beanName bean name
     * @param method method
     */
    protected abstract void visitMethod(Object bean, String beanName, Method method);

}

