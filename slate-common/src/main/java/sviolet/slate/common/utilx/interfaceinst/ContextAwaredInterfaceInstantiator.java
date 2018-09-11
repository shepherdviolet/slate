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

import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.cglib.proxy.Proxy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Method;

/**
 * <p>接口实例化器:可实现代理逻辑, 可获取ApplicationContext</p>
 *
 * @author S.Violet
 */
public abstract class ContextAwaredInterfaceInstantiator implements InterfaceInstantiator, InvocationHandler, ApplicationContextAware {

    @Override
    public final Object newInstance(Class<?> clazz) {
        return Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[]{clazz, ApplicationContextAware.class},
                this);
    }

    @Override
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("setApplicationContext".equals(method.getName()) && args.length == 1 && args[0] instanceof ApplicationContext) {
            setApplicationContext((ApplicationContext) args[0]);
            return null;
        }
        return onMethodCall(proxy, method, args);
    }

    /**
     * 实现接口方法调用逻辑
     */
    protected abstract Object onMethodCall(Object proxy, Method method, Object[] args) throws Throwable;

}
