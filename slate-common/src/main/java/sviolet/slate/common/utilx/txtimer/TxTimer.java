package sviolet.slate.common.utilx.txtimer;

import sviolet.thistle.util.spi.ThistleSpi;

public class TxTimer {

    private static final TxTimerProvider provider;

    static {
        if ("true".equals(System.getProperty("slate.txtimer.enabled", "false"))) {
            provider = ThistleSpi.newLoader().loadService(TxTimerProvider.class);
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
