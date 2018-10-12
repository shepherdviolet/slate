package sviolet.slate.common.x.conversion.beanutil;

/**
 * SlateBeanUtils拷贝/转换异常
 *
 * @author S.Violet
 */
public class MappingRuntimeException extends RuntimeException {
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