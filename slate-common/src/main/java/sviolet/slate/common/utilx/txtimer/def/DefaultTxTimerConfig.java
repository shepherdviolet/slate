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
     * 可动态调整, 启动参数优先级大于动态配置
     * [基本设置]打印周期内交易次数大于该值的记录, 默认0
     * 满足cnt >= thresholdCnt && (avg >= thresholdAvg || max >= thresholdMax || min >= thresholdMin)条件时打印日志
     */
    static int thresholdCnt;
    static boolean lockThresholdCnt = false;
    /**
     * 可动态调整, 启动参数优先级大于动态配置
     * [基本设置]打印周期内平均耗时超过该值的记录, 单位:毫秒, 默认0
     * 满足cnt >= thresholdCnt && (avg >= thresholdAvg || max >= thresholdMax || min >= thresholdMin)条件时打印日志
     */
    static int thresholdAvg;
    static boolean lockThresholdAvg = false;
    /**
     * 可动态调整, 启动参数优先级大于动态配置
     * [基本设置]打印周期内最大耗时超过该值的记录, 单位:毫秒, 默认0
     * 满足cnt >= thresholdCnt && (avg >= thresholdAvg || max >= thresholdMax || min >= thresholdMin)条件时打印日志
     */
    static int thresholdMax;
    static boolean lockThresholdMax = false;
    /**
     * 可动态调整, 启动参数优先级大于动态配置
     * [基本设置]打印周期内最小耗时超过该值的记录, 单位:毫秒, 默认0
     * 满足cnt >= thresholdCnt && (avg >= thresholdAvg || max >= thresholdMax || min >= thresholdMin)条件时打印日志
     */
    static int thresholdMin;
    static boolean lockThresholdMin = false;

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
     * 可动态调整, 启动参数优先级大于动态配置
     * [基本设置]打印周期内交易次数大于该值的记录, 默认0
     * 满足cnt >= thresholdCnt && (avg >= thresholdAvg || max >= thresholdMax || min >= thresholdMin)条件时打印日志
     */
    public static void setThresholdCnt(int thresholdCnt) {
        if (lockThresholdCnt) {
            logger.warn("TxTimer | Config: thresholdCnt has been locked by -Dslate.txtimer.threshold.cnt, can not change");
            return;
        }
        DefaultTxTimerConfig.thresholdCnt = thresholdCnt;
        logger.info("TxTimer | Config: thresholdCnt change to " + thresholdCnt);
        logger.info("TxTimer | Config: Now report if " + reportCondition());
    }

    /**
     * 可动态调整, 启动参数优先级大于动态配置
     * [基本设置]打印周期内平均耗时超过该值的记录, 单位:毫秒, 默认0
     * 满足cnt >= thresholdCnt && (avg >= thresholdAvg || max >= thresholdMax || min >= thresholdMin)条件时打印日志
     */
    public static void setThresholdAvg(int thresholdAvg) {
        if (lockThresholdAvg) {
            logger.warn("TxTimer | Config: thresholdAvg has been locked by -Dslate.txtimer.threshold.avg, can not change");
            return;
        }
        DefaultTxTimerConfig.thresholdAvg = thresholdAvg;
        logger.info("TxTimer | Config: thresholdAvg change to " + thresholdAvg);
        logger.info("TxTimer | Config: Now report if " + reportCondition());
    }

    /**
     * 可动态调整, 启动参数优先级大于动态配置
     * [基本设置]打印周期内最大耗时超过该值的记录, 单位:毫秒, 默认0
     * 满足cnt >= thresholdCnt && (avg >= thresholdAvg || max >= thresholdMax || min >= thresholdMin)条件时打印日志
     */
    public static void setThresholdMax(int thresholdMax) {
        if (lockThresholdMax) {
            logger.warn("TxTimer | Config: thresholdMax has been locked by -Dslate.txtimer.threshold.max, can not change");
            return;
        }
        DefaultTxTimerConfig.thresholdMax = thresholdMax;
        logger.info("TxTimer | Config: thresholdMax change to " + thresholdMax);
        logger.info("TxTimer | Config: Now report if " + reportCondition());
    }

    /**
     * 可动态调整, 启动参数优先级大于动态配置
     * [基本设置]打印周期内最小耗时超过该值的记录, 单位:毫秒, 默认0
     * 满足cnt >= thresholdCnt && (avg >= thresholdAvg || max >= thresholdMax || min >= thresholdMin)条件时打印日志
     */
    public static void setThresholdMin(int thresholdMin) {
        if (lockThresholdMin) {
            logger.warn("TxTimer | Config: thresholdMin has been locked by -Dslate.txtimer.threshold.min, can not change");
            return;
        }
        DefaultTxTimerConfig.thresholdMin = thresholdMin;
        logger.info("TxTimer | Config: thresholdMin change to " + thresholdMin);
        logger.info("TxTimer | Config: Now report if " + reportCondition());

    }

    /* ******************************************************************************************************************* */

    private static final Logger logger = LoggerFactory.getLogger(DefaultTxTimerConfig.class);

    static {
        reportInterval = getIntFromProperty("slate.txtimer.report.interval", 5);
        if (reportInterval < 2 || reportInterval > 60) { throw new IllegalArgumentException("slate.txtimer.report.interval must >= 2 and <= 60 (minus)"); }
        logger.info("TxTimer | Config: Report every " + reportInterval + " minutes");
        reportIntervalMillis = reportInterval * 60 * 1000;

        thresholdCnt = getIntFromProperty("slate.txtimer.threshold.cnt", -2);
        if (thresholdCnt > -2) { lockThresholdCnt = true; logger.debug("TxTimer | Config: thresholdCnt is locked by -Dslate.txtimer.threshold.cnt"); }
        thresholdAvg = getIntFromProperty("slate.txtimer.threshold.avg", -1);
        if (thresholdAvg > -2) { lockThresholdAvg = true; logger.debug("TxTimer | Config: thresholdAvg is locked by -Dslate.txtimer.threshold.avg"); }
        thresholdMax = getIntFromProperty("slate.txtimer.threshold.max", -1);
        if (thresholdMax > -2) { lockThresholdMax = true; logger.debug("TxTimer | Config: thresholdMax is locked by -Dslate.txtimer.threshold.max"); }
        thresholdMin = getIntFromProperty("slate.txtimer.threshold.min", -1);
        if (thresholdMin > -2) { lockThresholdMin = true; logger.debug("TxTimer | Config: thresholdMin is locked by -Dslate.txtimer.threshold.min"); }
        logger.info("TxTimer | Config: Report if " + reportCondition());

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
        return "cnt >= " + thresholdCnt + " && ( avg >= " + thresholdAvg + " || max >= " + thresholdMax + " || min >= " + thresholdMin + " )";
    }

}
