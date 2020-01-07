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

package sviolet.slate.common.x.conversion.beanutil;

/**
 * SlateBeanUtils拷贝/转换异常
 *
 * @author S.Violet
 */
public class MappingRuntimeException extends RuntimeException {

    private static final long serialVersionUID = -1014117760954793998L;

    private String fromType;
    private String toType;
    private String fieldName;

    /**
     * @param message 错误信息
     * @param cause 原异常, 可为null
     * @param fromType 原数据类型
     * @param toType 期望转成的数据类型
     * @param fieldName 转换失败的参数名, PropMapper中可以留空, 底层逻辑会自动赋值
     */
    public MappingRuntimeException(String message, Throwable cause, String fromType, String toType, String fieldName) {
        super(message, cause);
        this.fromType = fromType;
        this.toType = toType;
        this.fieldName = fieldName != null ? fieldName : "?";
    }
    /**
     * @return 源类型(类名)
     */
    public String getFromType() {
        return fromType;
    }

    /**
     * @return 目的类型(类名)
     */
    public String getToType() {
        return toType;
    }

    /**
     * @return 转换异常的字段名(可能为?)
     */
    public String getFieldName() {
        return fieldName;
    }

    void setFieldName(String fieldName){
        this.fieldName = fieldName != null ? fieldName : "?";
    }

}