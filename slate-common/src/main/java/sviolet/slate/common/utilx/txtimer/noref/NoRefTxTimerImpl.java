package sviolet.slate.common.utilx.txtimer.noref;

import sviolet.slate.common.utilx.txtimer.TxTimer;
import sviolet.slate.common.utilx.txtimer.TxTimerProvider;

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
