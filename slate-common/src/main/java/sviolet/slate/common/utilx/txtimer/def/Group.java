package sviolet.slate.common.utilx.txtimer.def;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static sviolet.slate.common.utilx.txtimer.def.DefaultTxTimerProvider.*;

class Group {

    private DefaultTxTimerProvider provider;

    Map<String, Transaction> transactions = new ConcurrentHashMap<>(MAP_INIT_CAP);

    Group(DefaultTxTimerProvider provider) {
        this.provider = provider;
    }

    Transaction getTransaction(String transactionName){
        Transaction transaction = transactions.get(transactionName);
        if (transaction == null) {
            //用StringHashLocks分散碰撞的可能性
            ReentrantLock lock = provider.locks.getLock(transactionName);
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
