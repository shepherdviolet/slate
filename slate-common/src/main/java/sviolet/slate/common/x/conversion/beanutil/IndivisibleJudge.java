package sviolet.slate.common.x.conversion.beanutil;

import java.util.Map;

/**
 * <p>SlateBeanUtils Bean工具的扩展点</p>
 *
 * <p>不可分割类型判断器</p>
 *
 * <p>实现:判断一个元素是否不可分割, 用于beanOrMapToMapRecursively</p>
 *
 * <p>使用扩展点之前, 请先仔细阅读文档: https://github.com/shepherdviolet/thistle/blob/master/docs/thistlespi/guide.md</p>
 *
 * @see SlateBeanUtils
 * @author S.Violet
 */
public interface IndivisibleJudge {

    /**
     * 实现: 判断元素是否不可分割
     * @param obj 待判断的对象
     * @param extraIndivisibleTypes 额外的不可分割类型
     */
    boolean isIndivisible(Object obj, Map<Class<?>, JudgeType> extraIndivisibleTypes);

    /**
     * 判断方式
     */
    public enum JudgeType {

        /**
         * 使用isAssignableFrom判断
         */
        IS_ASSIGNABLE_FROM,

        /**
         * 使用equals判断
         */
        EQUALS;

        public static JudgeType parse(String str) {
            if ("isAssignableFrom".equalsIgnoreCase(str)) {
                return IS_ASSIGNABLE_FROM;
            } else if ("equals".equalsIgnoreCase(str)) {
                return EQUALS;
            }
            return null;
        }

    }

}
