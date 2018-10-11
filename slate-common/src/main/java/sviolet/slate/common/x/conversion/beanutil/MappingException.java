package sviolet.slate.common.x.conversion.beanutil;

/**
 * SlateBeanUtils拷贝/转换异常
 *
 * @author S.Violet
 */
public class MappingException extends RuntimeException {
    private String from;
    private String to;
    private String field;

    public MappingException(String message, Throwable cause, String from, String to, String field) {
        super(message, cause);
        this.from = from;
        this.to = to;
        this.field = field != null ? field : "?";
    }
    /**
     * @return 源类型(类名)
     */
    public String getFrom() {
        return from;
    }

    /**
     * @return 目的类型(类名)
     */
    public String getTo() {
        return to;
    }

    /**
     * @return 转换异常的字段名(可能为?)
     */
    public String getField() {
        return field;
    }

    void setField(String field){
        this.field = field != null ? field : "?";
    }

}