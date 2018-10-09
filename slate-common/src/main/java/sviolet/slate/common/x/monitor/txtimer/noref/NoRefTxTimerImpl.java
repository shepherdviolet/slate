package sviolet.slate.common.x.monitor.txtimer.noref;

import sviolet.slate.common.x.monitor.txtimer.TxTimer;
import sviolet.slate.common.x.monitor.txtimer.TxTimerProvider;

/**
 * 与TxTimer关联点
 *
 * @author S.Violet
 */
class NoRefTxTimerImpl implements NoRefTxTimer {

    @Override
    public void start(String groupName, String transactionName) {
        TxTimer.start(groupName, transactionName);
    }

    @Override
    public void stop() {
        TxTimer.stop();
    }

    @Override
    public TxTimerProvider getProvider() {
        return TxTimer.getProvider();
    }

}
