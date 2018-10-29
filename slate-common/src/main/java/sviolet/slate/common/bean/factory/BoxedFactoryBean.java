package sviolet.slate.common.bean.factory;

import org.springframework.beans.factory.FactoryBean;

/**
 * 用于将已经实例化好的Bean对象注册到spring context中
 *
 * @author S.Violet
 */
public class BoxedFactoryBean implements FactoryBean {

    private Class<?> type;
    private Object obj;

    public BoxedFactoryBean(Class<?> type, Object obj) {
        this.type = type;
        this.obj = obj;
    }

    @Override
    public Object getObject() throws Exception {
        return obj;
    }

    @Override
    public Class<?> getObjectType() {
        return type;
    }

}
