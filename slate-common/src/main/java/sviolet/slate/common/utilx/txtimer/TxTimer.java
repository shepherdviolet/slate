package sviolet.slate.common.utilx.txtimer;

import sviolet.thistle.util.spi.ThistleSpi;

/**
 * <p>简易版交易耗时统计</p>
 *
 * <p>默认实现了交易耗时的统计, 并通过日志定时输出报告. 可以使用ThistleSpi替换实现.</p>
 *
 * <p>公共参数:<br>
 *      -Dslate.txtimer.enabled=true 启用统计, 默认true, 默认开启<br>
 * </p>
 *
 * <p>默认实现的参数(在没有替换实现时):<br>
 *      -Dslate.txtimer.reportinterval=30 日志报告输出间隔, 单位分钟, [3-60], 默认30<br>
 *      -Dslate.txtime.reportlines=20 日志每次输出的最大行数, 大于该行数会分页, 默认20<br>
 * </p>
 *
 * </p>
 *
 * @author S.Violet
 */
public class TxTimer {

    private static final TxTimerProvider provider;

    static {
        //统计开关, 默认关闭
        if ("true".equals(System.getProperty("slate.txtimer.enabled", "true"))) {
            TxTimerProvider service = ThistleSpi.newLoader().loadService(TxTimerProvider.class);
            //再根据provider判断是否要启用
            if (service.enabled()) {
                provider = service;
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
        return null;
    }

}
