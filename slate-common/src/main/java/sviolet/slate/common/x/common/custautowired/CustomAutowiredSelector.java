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

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import sviolet.slate.common.x.proxy.interfaceinst.InterfaceInstConfig;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>ImportSelector</p>
 *
 * @author S.Violet
 */
public abstract class CustomAutowiredSelector implements ImportSelector {

    static List<AnnotationAttributes> annotationAttributesList = new LinkedList<>();

    @Override
    public final String[] selectImports(AnnotationMetadata importingClassMetadata) {
        /*
         * 此处用静态变量持有注解参数, 原因同InterfaceInstConfig
         */
        AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(EnableCustomAutowired.class.getName(), false));
        if (annotationAttributes != null) {
            annotationAttributesList.add(annotationAttributes);
        }
        //指定配置类
        return new String[]{CustomAutowiredConfig.class.getName()};
    }

}
