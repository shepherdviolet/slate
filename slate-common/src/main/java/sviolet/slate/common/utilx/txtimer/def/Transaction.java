package sviolet.slate.common.utilx.txtimer.def;

import java.util.concurrent.atomic.AtomicInteger;

class Transaction {

    private DefaultTxTimerProvider provider;
    private String transactionName;

    private AtomicInteger runningCount = new AtomicInteger(0);
    private AtomicInteger lostCount = new AtomicInteger(0);

    Transaction(DefaultTxTimerProvider provider, String transactionName) {
        this.provider = provider;
        this.transactionName = transactionName;
    }

    void lost(){
        lostCount.incrementAndGet();
    }

    void running(){
        runningCount.incrementAndGet();
    }

    void finish(long elapse) {
        runningCount.decrementAndGet();


    }

}
