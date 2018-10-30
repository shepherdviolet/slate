package sviolet.slate.springboot.x.net.loadbalance.auto;

import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;
import sviolet.slate.common.x.bean.mbrproc.MemberProcessor;
import sviolet.slate.common.x.net.loadbalance.classic.SimpleOkHttpClient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * <p>实现用@HttpClient注解注入客户端</p>
 *
 * @author S.Violet
 */
public class HttpClientMemberProcessor implements MemberProcessor<HttpClient> {

    private volatile HttpClients httpClients;

    @Override
    public Class<HttpClient> acceptAnnotationType() {
        return HttpClient.class;
    }

    @Override
    public void visitField(Object bean, String beanName, Field field, HttpClient annotation, ApplicationContext applicationContext) {
        if (!SimpleOkHttpClient.class.isAssignableFrom(field.getType())) {
            throw new IllegalHttpClientAnnotationException("Illegal usage of @HttpClient in " + bean.getClass().getName() + " (field " + field.getName() + ")" +
                    ", this annotation can only be used on field of SimpleOkHttpClient, e.g. @HttpClient(\"tagname\") SimpleOkHttpClient client;");
        }
        SimpleOkHttpClient client = getHttpClient(applicationContext, annotation, bean);
        if (client != null) {
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, bean, client);
        }
    }

    @Override
    public void visitMethod(Object bean, String beanName, Method method, HttpClient annotation, ApplicationContext applicationContext) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 1) {
            throw new IllegalHttpClientAnnotationException("Illegal usage of @HttpClient in " + bean.getClass().getName() + " (method " + method.getName() + ")" +
                    ", this annotation can only be used on method with one parameter, but you have " + parameterTypes.length +
                    " parameters, e.g. @HttpClient(\"tagname\") public void setClient(SimpleOkHttpClient client){...}");
        }
        if (!SimpleOkHttpClient.class.isAssignableFrom(parameterTypes[0])) {
            throw new IllegalHttpClientAnnotationException("Illegal usage of @HttpClient in " + bean.getClass().getName() + " (method " + method.getName() + ")" +
                    ", this annotation can only be used on method with one SimpleOkHttpClient parameter, but your parameter type is " + parameterTypes[0].getName() +
                    ", e.g. @HttpClient(\"tagname\") public void setClient(SimpleOkHttpClient client){...}");
        }
        SimpleOkHttpClient client = getHttpClient(applicationContext, annotation, bean);
        if (client != null) {
            ReflectionUtils.makeAccessible(method);
            ReflectionUtils.invokeMethod(method, bean, client);
        }
    }

    private SimpleOkHttpClient getHttpClient(ApplicationContext applicationContext, HttpClient annotation, Object bean) {
        if (httpClients == null) {
            synchronized (this) {
                if (httpClients == null) {
                    httpClients = applicationContext.getBean(HttpClientsConfig.HTTP_CLIENTS_NAME, HttpClients.class);
                }
            }
        }
        SimpleOkHttpClient client = httpClients.get(annotation.value());
        if (client == null && annotation.required()) {
            throw new NoSuchHttpClientDefinitionException("No HttpClient named '" + annotation.value() + "', required by " + bean.getClass().getName());
        }
        return client;
    }

}
