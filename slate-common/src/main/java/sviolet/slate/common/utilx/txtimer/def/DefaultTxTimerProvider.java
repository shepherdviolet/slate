package sviolet.slate.common.utilx.txtimer.def;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sviolet.slate.common.utilx.txtimer.TxTimerProvider;
import sviolet.thistle.model.concurrent.StringHashLocks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class DefaultTxTimerProvider implements TxTimerProvider {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTxTimerProvider.class);

    static final int MAP_INIT_CAP;
    static final int HASH_LOCK_NUM;
    static final int UPDATE_MAX_ATTEMPTS;

    static {
        MAP_INIT_CAP = getIntFromProperty("slate.txtimer.mapinitcap", 1024);
        HASH_LOCK_NUM = getIntFromProperty("slate.txtimer.hashlocknum", 64);
        UPDATE_MAX_ATTEMPTS = getIntFromProperty("slate.txtime.updateattemps", 10);
    }

    private static int getIntFromProperty(String key, int def) {
        try {
            return Integer.parseInt(System.getProperty(key, String.valueOf(def)));
        } catch (Exception e) {
            logger.error("Error while parsing -D" + key + " to int, using " + def + " by default", e);
            return def;
        }
    }

    /* *************************************************************************************************************** */

    private Map<String, Group> map = new ConcurrentHashMap<>(MAP_INIT_CAP);
    private ThreadLocal<Record> record = new ThreadLocal<>();
    private AtomicInteger missingCount = new AtomicInteger(0);

    StringHashLocks locks = new StringHashLocks(HASH_LOCK_NUM);

    @Override
    public void start(String groupName, String transactionName) {
        Record record = this.record.get();
        if (record != null) {
            Transaction transaction = getGroup(record.getGroupName()).getTransaction(record.getTransactionName());
            transaction.lost();
        }
        Transaction transaction = getGroup(groupName).getTransaction(transactionName);
        transaction.running();
        record = new Record(groupName, transactionName);
        this.record.set(record);
    }

    @Override
    public void stop() {
        Record record = this.record.get();
        if (record == null) {
            missingCount.incrementAndGet();
            return;
        }
        long elapse = System.currentTimeMillis() - record.getStartTime();
        Transaction transaction = getGroup(record.getGroupName()).getTransaction(record.getTransactionName());
        transaction.finish(elapse);
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public boolean canBeGet() {
        return true;
    }

    private Group getGroup(String groupName) {
        Group group = map.get(groupName);
        if (group == null) {
            ReentrantLock lock = locks.getLock(groupName);
            try {
                lock.lock();
                group = map.get(groupName);
                if (group == null) {
                    group = new Group(this, groupName);
                    map.put(groupName, group);
                }
            } finally {
                lock.unlock();
            }
        }
        return group;
    }

}
