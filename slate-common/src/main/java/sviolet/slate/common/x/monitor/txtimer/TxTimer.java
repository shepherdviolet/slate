package sviolet.slate.common.x.monitor.txtimer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sviolet.thistle.x.common.thistlespi.ThistleSpi;

/**
 * <p>简单的交易耗时统计, 提供默认实现, 也可以用ThistleSpi扩展</p>
 *
 * <p>日志前缀:TxTimer</p>
 *
 * <p>启动参数:<br>
 *      -Dslate.txtimer.enabled=true 启用统计, true开启, false关闭, 默认开启<br>
 * </p>
 *
 * <p>默认实现 ********************************************************************************************</p>
 *
 * <p>
 *     1.默认实现了交易耗时的统计, 并通过日志定时输出报告. <br>
 *     2.可以使用ThistleSpi替换实现, 替换实现后下面的参数无效. <br>
 * </p>
 *
 * <p>默认实现的启动参数(不可动态修改):<br>
 *      -Dslate.txtimer.report.interval=5 日志报告输出间隔, 单位分钟, [2-60], 默认5 <br>
 *      -Dslate.txtimer.pagelines=20 日志每次输出的最大行数, 大于该行数会分页, 默认20 <br>
 * </p>
 *
 * <p>默认实现的启动参数(可动态修改):<br>
 *      -Dslate.txtimer.reportall.interval=60 全量日志报告输出间隔(周期), 单位:分钟, [2-∞], 默认∞(不输出全量日志)<br>
 *      -Dslate.txtimer.threshold.avg=2000 打印周期内平均耗时超过该值的交易, 单位:毫秒<br>
 *      -Dslate.txtimer.threshold.max=10000 打印周期内最大耗时超过该值的交易, 单位:毫秒<br>
 *      -Dslate.txtimer.threshold.min=1000 打印周期内最小耗时超过该值的交易, 单位:毫秒<br>
 * </p>
 *
 * <p>slate.txtimer.threshold系列参数均未配置, 则输出全部交易的报告. 若设置了任意一个, 则只有满足条件的交易才输出:
 * avg >= thresholdAvg || max >= thresholdMax || min >= thresholdMin</p>
 *
 * @author S.Violet
 */
public class TxTimer {

    private static final Logger logger = LoggerFactory.getLogger(TxTimer.class);

    private static final TxTimerProvider provider;

    static {
        //统计开关, 默认关闭
        if ("true".equals(System.getProperty("slate.txtimer.enabled", "true"))) {
            TxTimerProvider service = ThistleSpi.getLoader().loadService(TxTimerProvider.class);
            //再根据provider判断是否要启用
            if (service.enabled()) {
                provider = service;
                logger.info("TxTimer | TxTimer Enabled !!! implementation " + provider.getClass().getName());
            } else {
                provider = null;
            }
        } else {
            provider = null;
        }
    }

    /**
     * <p>交易开始时调用</p>
     *
     * <code>
     *  try {
     *      TxTimer.start("Entrance", "TestService");
     *      // 交易逻辑 ......
     *  } finally {
     *      TxTimer.stop();
     *  }
     * </code>
     *
     * @param groupName 组别
     * @param transactionName 交易名
     */
    public static void start(String groupName, String transactionName){
        if (provider != null) {
            provider.start(groupName, transactionName);
        }
    }

    /**
     * 交易结束时调用
     *
     * <code>
     *  try {
     *      TxTimer.start("Entrance", "TestService");
     *      // 交易逻辑 ......
     *  } finally {
     *      TxTimer.stop();
     *  }
     * </code>
     *
     */
    public static void stop(){
        if (provider != null) {
            provider.stop();
        }
    }

    public static TxTimerProvider getProvider(){
        if (provider != null && provider.canBeGet()) {
            return provider;
        }
        logger.error("TxTimer | Prohibit access to get TxTimerProvider, Null or Banned by TxTimerProvider.canBeGet()");
        return null;
    }

}
