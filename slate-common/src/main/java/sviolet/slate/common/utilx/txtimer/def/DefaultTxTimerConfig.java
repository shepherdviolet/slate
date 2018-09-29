package sviolet.slate.common.utilx.txtimer.def;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>默认交易耗时统计的配置</p>
 *
 * @author S.Violet
 */
class DefaultTxTimerConfig {

    /**
     * 启动后固定
     * [基本设置]日志报告输出间隔(周期), 单位:分钟, [2-60], 默认5
     */
    static int reportInterval;
    static int reportIntervalMillis;
    /**
     * 可动态调整, 启动参数优先级大于动态配置<br>
     * [基本设置]打印周期内平均耗时超过该值的交易, 单位:毫秒<br>
     * slate.txtimer.threshold系列参数均未配置, 则输出全部交易的报告. 若设置了任意一个, 则只有满足条件的交易才输出:
     * avg >= thresholdAvg || max >= thresholdMax || min >= thresholdMin<br>
     */
    static int thresholdAvg;
    static boolean lockThresholdAvg = false;
    /**
     * 可动态调整, 启动参数优先级大于动态配置<br>
     * [基本设置]打印周期内最大耗时超过该值的交易, 单位:毫秒<br>
     * slate.txtimer.threshold系列参数均未配置, 则输出全部交易的报告. 若设置了任意一个, 则只有满足条件的交易才输出:
     * avg >= thresholdAvg || max >= thresholdMax || min >= thresholdMin<br>
     */
    static int thresholdMax;
    static boolean lockThresholdMax = false;
    /**
     * 可动态调整, 启动参数优先级大于动态配置<br>
     * [基本设置]打印周期内最小耗时超过该值的交易, 单位:毫秒<br>
     * slate.txtimer.threshold系列参数均未配置, 则输出全部交易的报告. 若设置了任意一个, 则只有满足条件的交易才输出:
     * avg >= thresholdAvg || max >= thresholdMax || min >= thresholdMin<br>
     */
    static int thresholdMin;
    static boolean lockThresholdMin = false;
    static boolean thresholdEnabled = false;

    /**
     * 启动后固定
     * [调优设置]日志每次输出的最大行数, 大于该行数会分页, 默认20
     */
    static int pageLines;
    /**
     * 启动后固定
     * [调优设置]内部Map的初始大小, 大于观测点数量为宜
     */
    static int mapInitCap;
    /**
     * 启动后固定
     * [调优设置]StringHashLocks的锁数量
     */
    static int hashLockNum;
    /**
     * 启动后固定
     * [调优设置]内部一些非锁更新操作的最大尝试次数
     */
    static int updateAttempts;

    /* ******************************************************************************************************************* */

    /**
     * 可动态调整, 启动参数优先级大于动态配置<br>
     * [基本设置]打印周期内平均耗时超过该值的记录, 单位:毫秒<br>
     * slate.txtimer.threshold系列参数均未配置, 则输出全部交易的报告. 若设置了任意一个, 则只有满足条件的交易才输出:
     * avg >= thresholdAvg || max >= thresholdMax || min >= thresholdMin<br>
     */
    public static void setThresholdAvg(int thresholdAvg) {
        if (lockThresholdAvg) {
            logger.warn("TxTimer | Config: thresholdAvg has been locked by -Dslate.txtimer.threshold.avg, can not change");
            return;
        }
        DefaultTxTimerConfig.thresholdAvg = thresholdAvg;
        thresholdEnabled = true;
        logger.info("TxTimer | Config: thresholdAvg change to " + thresholdAvg);
        logger.info("TxTimer | Config: Now report " + reportCondition());
    }

    /**
     * 可动态调整, 启动参数优先级大于动态配置<br>
     * [基本设置]打印周期内最大耗时超过该值的记录, 单位:毫秒<br>
     * slate.txtimer.threshold系列参数均未配置, 则输出全部交易的报告. 若设置了任意一个, 则只有满足条件的交易才输出:
     * avg >= thresholdAvg || max >= thresholdMax || min >= thresholdMin<br>
     */
    public static void setThresholdMax(int thresholdMax) {
        if (lockThresholdMax) {
            logger.warn("TxTimer | Config: thresholdMax has been locked by -Dslate.txtimer.threshold.max, can not change");
            return;
        }
        DefaultTxTimerConfig.thresholdMax = thresholdMax;
        thresholdEnabled = true;
        logger.info("TxTimer | Config: thresholdMax change to " + thresholdMax);
        logger.info("TxTimer | Config: Now report " + reportCondition());
    }

    /**
     * 可动态调整, 启动参数优先级大于动态配置<br>
     * [基本设置]打印周期内最小耗时超过该值的记录, 单位:毫秒<br>
     * slate.txtimer.threshold系列参数均未配置, 则输出全部交易的报告. 若设置了任意一个, 则只有满足条件的交易才输出:
     * avg >= thresholdAvg || max >= thresholdMax || min >= thresholdMin<br>
     */
    public static void setThresholdMin(int thresholdMin) {
        if (lockThresholdMin) {
            logger.warn("TxTimer | Config: thresholdMin has been locked by -Dslate.txtimer.threshold.min, can not change");
            return;
        }
        DefaultTxTimerConfig.thresholdMin = thresholdMin;
        thresholdEnabled = true;
        logger.info("TxTimer | Config: thresholdMin change to " + thresholdMin);
        logger.info("TxTimer | Config: Now report " + reportCondition());
    }

    /* ******************************************************************************************************************* */

    private static final Logger logger = LoggerFactory.getLogger(DefaultTxTimerConfig.class);

    static {
        reportInterval = getIntFromProperty("slate.txtimer.report.interval", 5);
        if (reportInterval < 2 || reportInterval > 60) { throw new IllegalArgumentException("slate.txtimer.report.interval must >= 2 and <= 60 (minus)"); }
        logger.info("TxTimer | Config: Report every " + reportInterval + " minutes");
        reportIntervalMillis = reportInterval * 60 * 1000;

        thresholdAvg = getIntFromProperty("slate.txtimer.threshold.avg", Integer.MAX_VALUE);
        if (thresholdAvg < Integer.MAX_VALUE) { lockThresholdAvg = true; thresholdEnabled = true; logger.debug("TxTimer | Config: thresholdAvg is locked by -Dslate.txtimer.threshold.avg=" + thresholdAvg); }
        thresholdMax = getIntFromProperty("slate.txtimer.threshold.max", Integer.MAX_VALUE);
        if (thresholdMax < Integer.MAX_VALUE) { lockThresholdMax = true; thresholdEnabled = true; logger.debug("TxTimer | Config: thresholdMax is locked by -Dslate.txtimer.threshold.max=" + thresholdMax); }
        thresholdMin = getIntFromProperty("slate.txtimer.threshold.min", Integer.MAX_VALUE);
        if (thresholdMin < Integer.MAX_VALUE) { lockThresholdMin = true; thresholdEnabled = true; logger.debug("TxTimer | Config: thresholdMin is locked by -Dslate.txtimer.threshold.min=" + thresholdMin); }
        logger.info("TxTimer | Config: Report " + reportCondition());

        pageLines = getIntFromProperty("slate.txtimer.pagelines", 20);
        mapInitCap = getIntFromProperty("slate.txtimer.mapinitcap", 128);
        hashLockNum = getIntFromProperty("slate.txtimer.hashlocknum", 16);
        updateAttempts = getIntFromProperty("slate.txtimer.updateattemps", 10);
    }

    private static int getIntFromProperty(String key, int def) {
        try {
            return Integer.parseInt(System.getProperty(key, String.valueOf(def)));
        } catch (Exception e) {
            logger.error("TxTimer | Config: Error while parsing -D" + key + " to int, using " + def + " by default", e);
            return def;
        }
    }

    static String reportCondition(){
        if (thresholdEnabled) {
            return "if avg >= " + (thresholdAvg < Integer.MAX_VALUE ? thresholdAvg : "∞") +
                    " || max >= " + (thresholdMax < Integer.MAX_VALUE ? thresholdMax : "∞") +
                    " || min >= " + (thresholdMin < Integer.MAX_VALUE ? thresholdMin : "∞");
        } else {
            return "all";
        }
    }

}
