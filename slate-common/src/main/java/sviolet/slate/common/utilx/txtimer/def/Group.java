package sviolet.slate.common.utilx.txtimer.def;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static sviolet.slate.common.utilx.txtimer.def.DefaultTxTimerProvider.*;

class Group {

    private Map<String, Transaction> map = new ConcurrentHashMap<>(MAP_INIT_CAP);

    private DefaultTxTimerProvider provider;
    private String groupName;

    Group(DefaultTxTimerProvider provider, String groupName) {
        this.provider = provider;
        this.groupName = groupName;
    }

    Transaction getTransaction(String transactionName){
        Transaction transaction = map.get(transactionName);
        if (transaction == null) {
            ReentrantLock lock = provider.locks.getLock(transactionName);
            try {
                lock.lock();
                transaction = map.get(transactionName);
                if (transaction == null) {
                    transaction = new Transaction(provider, transactionName);
                    map.put(transactionName, transaction);
                }
            } finally {
                lock.unlock();
            }
        }
        return transaction;
    }

}
