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

package sviolet.slate.common.util.proxy;

import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.cglib.proxy.Proxy;

import java.lang.reflect.Method;

/**
 * springframework CGLib 代理工具
 * 依赖org.springframework:spring-core
 *
 * @author S.Violet
 */
public class CGLibProxyUtils {

    /**
     * 给定类/接口创建一个代理对象, 所有的方法实现为空,
     * 通常用于将接口实例化
     */
    public static <T> T newEmptyInstance(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(EmptyInvocationHandler.class.getClassLoader(), new Class<?>[] {clazz}, new EmptyInvocationHandler());
    }

    static class EmptyInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // do something
            return null;
        }
    }

}
