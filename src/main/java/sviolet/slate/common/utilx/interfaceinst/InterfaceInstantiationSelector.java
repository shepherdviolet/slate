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

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

public class InterfaceInstantiationSelector implements ImportSelector {

    static AnnotationAttributes annotationAttributes;

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        /*
         * 不知道为什么InterfaceInstantiationConfiguration实现了ImportAware还是获取不到AnnotationMetadata, 只能静态变量了
         */
        annotationAttributes = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(EnableInterfaceInstantiation.class.getName(), false));
        return new String[]{InterfaceInstantiationConfiguration.class.getName()};
    }
}
