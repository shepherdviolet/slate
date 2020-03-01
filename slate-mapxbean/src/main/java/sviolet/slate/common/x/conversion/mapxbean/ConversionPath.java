/*
 * Copyright (C) 2015-2020 S.Violet
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

package sviolet.slate.common.x.conversion.mapxbean;

import java.lang.reflect.Type;

/**
 * <p>Keep track of what node is currently converting, and you can get the parent nodes, up to the root node.
 * It represents the conversion path of current process. </p>
 *
 * <p>WARNING! Please NO NOT hold this object for a long time, otherwise it will cause a memory leak.</p>
 *
 * @author S.Violet
 * @see MapXBean
 */
public class ConversionPath {

    private final String propertyName;
    private final Class<?> fromClass;
    private final Class<?> toClass;
    private final Type toType;
    private final ConversionPath parentNode;
    private final int depth;
    private boolean inflated = false;

    public ConversionPath(String propertyName, Class<?> fromClass, Class<?> toClass, Type toType, ConversionPath parentNode) {
        this.propertyName = propertyName;
        this.fromClass = fromClass;
        this.toClass = toClass;
        this.toType = toType;
        this.parentNode = parentNode;
        this.depth = parentNode != null ? parentNode.depth + 1 : 0;
    }

    /**
     * Nullable. The property name of the current node in its parent.
     * Root node has no property name.
     */
    public String propertyName() {
        return propertyName;
    }

    /**
     * Current node is convert from this class
     */
    public Class<?> fromClass() {
        return fromClass;
    }

    /**
     * Current node is convert to this class
     */
    public Class<?> toClass() {
        return toClass;
    }

    /**
     * Current node is convert to this generic type
     */
    public Type toType() {
        return toType;
    }

    /**
     * Nullable. Get parent node.
     * Root node has no parent node.
     */
    public ConversionPath parentNode() {
        return parentNode;
    }

    /**
     * Current conversion depth, starting from 0.
     * Depth of root node is 0, and it's properties is 1 ...
     */
    public int depth() {
        return depth;
    }

    /**
     * Is it an inflated node (The property is inflated to Map)
     *
     * <p>References:</p><br>
     *
     * <p>In Bean -> Map scene. While a Bean is converting to a Map, all the properties of Bean will keep the original type
     * by default, unless {@link BeanToMapInflateStrategy} tells the program that it needs to be inflated
     * (i.e. the method {@link BeanToMapInflateStrategy#needToBeInflated} returns true).
     * 'Inflate' means convert a property of Java Bean (or element of Collection) to a Map. Notice that the property must
     * be a Java Bean with read methods (Judged by {@link MxbTypeJudger#isBean}).</p><br>
     */
    public boolean isInflated() {
        return inflated;
    }

    /**
     * Mark as inflated node
     */
    void setInflated(boolean inflated) {
        this.inflated = inflated;
    }

    @Override
    public String toString() {
        return (parentNode != null ? parentNode.toString() + "->" : "") + (propertyName != null ? propertyName : "root");
    }
}
