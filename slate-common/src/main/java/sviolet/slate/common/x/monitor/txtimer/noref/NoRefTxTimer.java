package sviolet.slate.common.x.monitor.txtimer.noref;

import sviolet.slate.common.x.monitor.txtimer.TxTimerProvider;

/**
 * <p>对TxTimer类无引用的NoRefTxTimer代理类</p>
 *
 * <p>本类和NoRefTxTimerFactory类对TxTimer无直接类引用, 用于类库和框架层, 可以由用户选择是否开启TxTimer, 若不开启, 则不会初始化
 * TxTimer类, 减少无用的对象创建.</p>
 *
 * @author S.Violet
 */
public interface NoRefTxTimer {

    void start(String groupName, String transactionName);

    void stop();

    TxTimerProvider getProvider();

}
