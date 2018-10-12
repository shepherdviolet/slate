package sviolet.slate.common.x.monitor.txtimer;

/**
 * <p>TxTimer简单的交易耗时统计 扩展点</p>
 *
 * <p>实现:耗时统计/结果输出</p>
 *
 * <p>使用扩展点之前, 请先仔细阅读文档: https://github.com/shepherdviolet/thistle/blob/master/docs/thistlespi/guide.md</p>
 *
 * @see TxTimer
 * @author S.Violet
 */
public interface TxTimerProvider {

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
    void start(String groupName, String transactionName);

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
    void stop();

    /**
     * 是否启用统计功能
     * @return true 启用
     */
    boolean enabled();

    /**
     * 是否能通过TxTimer.getProvider()获取到当前实例
     * @return true 允许
     */
    boolean canBeGet();

}
