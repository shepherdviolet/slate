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