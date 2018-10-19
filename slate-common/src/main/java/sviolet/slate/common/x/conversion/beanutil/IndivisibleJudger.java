package sviolet.slate.common.x.conversion.beanutil;

import sviolet.thistle.util.conversion.PrimitiveUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * 不可分割元素判断器
 *
 * @author S.Violet
 */
public abstract class IndivisibleJudger {

    /**
     * 判断元素是否不可分割: 默认(不可修改)
     */
    final boolean isIndivisible(Object obj) {
        if (obj == null) {
            return true;
        }
        Class type = obj.getClass();
        if (PrimitiveUtils.isPrimitiveOrWrapper(type) ||
                Object.class.equals(type) ||
                type.isEnum()) {
            return true;
        }
        return isIndivisibleDefault(type, obj);
    }

    /**
     * 判断元素是否不可分割: 默认(可修改)
     */
    protected boolean isIndivisibleDefault(Class type, Object obj){
        if (type.isArray() ||
                String.class.isAssignableFrom(type) ||
                BigDecimal.class.isAssignableFrom(type) ||
                BigInteger.class.isAssignableFrom(type) ||
                Date.class.isAssignableFrom(type)) {
            return true;
        }
        return isIndivisibleCustom(type, obj);
    }

    /**
     * 判断元素是否不可分割: 自定义
     */
    protected abstract boolean isIndivisibleCustom(Class type, Object obj);

}
