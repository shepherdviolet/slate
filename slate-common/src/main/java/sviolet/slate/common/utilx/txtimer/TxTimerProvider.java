package sviolet.slate.common.utilx.txtimer;

public interface TxTimerProvider {

    void start(String groupName, String transactionName);

    void stop();

    boolean enabled();

    boolean canBeGet();

}
