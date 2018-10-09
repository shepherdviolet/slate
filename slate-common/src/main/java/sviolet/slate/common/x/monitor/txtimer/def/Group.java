package sviolet.slate.common.x.monitor.txtimer.def;

import sviolet.thistle.model.concurrent.lock.UnsafeSpinLock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class Group {

    private DefaultTxTimerProvider provider;

    Map<String, Transaction> transactions = new ConcurrentHashMap<>(DefaultTxTimerConfig.mapInitCap);

    Group(DefaultTxTimerProvider provider) {
        this.provider = provider;
    }

    Transaction getTransaction(String transactionName){
        Transaction transaction = transactions.get(transactionName);
        if (transaction == null) {
            //用StringHashLocks分散碰撞的可能性
            @SuppressWarnings("deprecation")
            UnsafeSpinLock lock = provider.locks.getLock(transactionName);
            try {
                lock.lock();
                transaction = transactions.get(transactionName);
                if (transaction == null) {
                    transaction = new Transaction(provider);
                    transactions.put(transactionName, transaction);
                }
            } finally {
                lock.unlock();
            }
        }
        return transaction;
    }

}
