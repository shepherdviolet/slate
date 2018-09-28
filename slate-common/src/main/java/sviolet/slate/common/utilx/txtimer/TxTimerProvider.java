package sviolet.slate.common.utilx.txtimer;

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
     * <p>需要添加启动参数-Dslate.txtimer.enabled=true启用功能</p>
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
     * <p>需要添加启动参数-Dslate.txtimer.enabled=true启用功能</p>
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
