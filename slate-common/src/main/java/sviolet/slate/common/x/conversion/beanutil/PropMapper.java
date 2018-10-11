package sviolet.slate.common.x.conversion.beanutil;

public interface PropMapper {

    Class<?> fromType();

    Class<?> toType();

    Object map(Object from);

}
