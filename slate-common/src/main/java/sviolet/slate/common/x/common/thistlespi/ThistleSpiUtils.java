package sviolet.slate.common.x.common.thistlespi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sviolet.thistle.model.common.SysPropFirstProperties;
import sviolet.thistle.x.common.thistlespi.ThistleSpi;

import java.util.Properties;

/**
 * ThistleSpi 工具
 *
 * @author S.Violet
 */
public class ThistleSpiUtils {

    private static final Logger logger = LoggerFactory.getLogger(ThistleSpiUtils.class);

    /**
     * 将Service/Plugin实现类构造参数的Properties对象包装成强化版, 先从系统参数(启动参数)中获取, 若获取失败, 则从构造参数
     * 引用的配置文件中获取, 还是获取失败则返回默认值. 提供数字类型的安全获取(解析失败会打印日志并返回默认值).
     * @param properties Service/Plugin实现类构造参数的Properties对象
     * @return 强化版
     */
    public static SysPropFirstProperties wrapPropertiesBySysProp(Properties properties){
        return new SysPropFirstProperties(properties, SYS_PROP_FIRST_PROPERTIES_EXCEPTION_HANDLER);
    }

    private static SysPropFirstProperties.ExceptionHandler SYS_PROP_FIRST_PROPERTIES_EXCEPTION_HANDLER = new SysPropFirstProperties.ExceptionHandler() {
        /**
         * 当value从String转为指定类型时发生异常.
         * 你可以在这里打印日志, get方法会返回默认值, 或抛出RuntimeException, get方法就会抛出该异常.
         * @param parsingSysProp true:解析系统参数(启动参数)发生错误 false:解析内置的Properties参数发生错误
         * @param key 发生错误的key
         * @param value 发生错误的value
         * @param defValue 尝试使用的默认值
         * @param properties 内置的Properties(不是系统Properties)
         */
        @Override
        public void onParseException(boolean parsingSysProp, String key, String value, Class<?> toType, String defValue, Properties properties, Exception e) {
            if (parsingSysProp) {
                logger.warn("?" + ThistleSpi.LOG_PREFIX_LOADER + "WARNING: Error while parsing system property '-D" + key + "=" + value + "' to " + toType.getName() + ", try using '" + defValue + "'", e);
            } else {
                logger.warn("?" + ThistleSpi.LOG_PREFIX_LOADER + "WARNING: Error while parsing constructor parameter '" + value + "' to " + toType.getName() + ", using default '" + defValue + "'" +
                        ", parameter key:" + key + ", definite in " + properties.get(ThistleSpi.PROPERTIES_URL), e);
            }
        }
    };

}
