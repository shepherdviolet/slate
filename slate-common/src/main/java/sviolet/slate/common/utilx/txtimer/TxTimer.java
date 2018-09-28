package sviolet.slate.common.utilx.txtimer;

import sviolet.thistle.util.spi.ThistleSpi;

/**
 * 简易版交易耗时统计
 *
 * @author S.Violet
 */
public class TxTimer {

    private static final TxTimerProvider provider;

    static {
        //check args
        if ("true".equals(System.getProperty("slate.txtimer.enabled", "false"))) {
            TxTimerProvider service = ThistleSpi.newLoader().loadService(TxTimerProvider.class);
            //check enabled
            if (service.enabled()) {
                provider = service;
            } else {
                provider = null;
            }
        } else {
            provider = null;
        }
    }

    public static void start(String groupName, String transactionName){
        if (provider != null) {
            provider.start(groupName, transactionName);
        }
    }

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
