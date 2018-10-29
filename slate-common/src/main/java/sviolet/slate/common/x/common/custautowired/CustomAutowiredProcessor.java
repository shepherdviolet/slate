package sviolet.slate.common.x.common.custautowired;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * <p>自定义Autowired, 处理器</p>
 *
 * @author S.Violet
 */
public interface CustomAutowiredProcessor {

    void visitField(Object bean, String beanName, Field field, Annotation annotation);

    void visitMethod(Object bean, String beanName, Method method, Annotation annotation);

}
