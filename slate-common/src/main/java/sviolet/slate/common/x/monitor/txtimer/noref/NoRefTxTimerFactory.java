package sviolet.slate.common.x.monitor.txtimer.noref;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sviolet.slate.common.x.monitor.txtimer.TxTimerProvider;

/**
 * <p>对TxTimer类无引用的NoRefTxTimer代理工厂</p>
 *
 * <p>本工厂类和NoRefTxTimer类对TxTimer无直接类引用, 用于类库和框架层, 可以由用户选择是否开启TxTimer, 若不开启, 则不会初始化
 * TxTimer类, 减少无用的对象创建.</p>
 *
 * @author S.Violet
 */
public class NoRefTxTimerFactory {

    private static final Logger logger = LoggerFactory.getLogger(NoRefTxTimerFactory.class);

    public static NoRefTxTimer newInstance() {
        try {
            return (NoRefTxTimer) Class.forName("sviolet.slate.common.x.monitor.txtimer.noref.NoRefTxTimerImpl").newInstance();
        } catch (Exception e) {
            logger.error("TxTimer | NoRefTxTimerFactory create NoRefTxTimer instance failed, TxTimer disabled !!!", e);
            return new DummyNoRefTxTimer();
        }
    }

    private static class DummyNoRefTxTimer implements NoRefTxTimer {
        @Override
        public void start(String groupName, String transactionName) {
        }
        @Override
        public void stop() {
        }
        @Override
        public TxTimerProvider getProvider() {
            return null;
        }
    }

}
