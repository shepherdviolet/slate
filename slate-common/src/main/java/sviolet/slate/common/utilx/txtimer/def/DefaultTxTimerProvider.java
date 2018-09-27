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

    static {
        //map initial capacity
        int mapInitCap;
        try {
            mapInitCap = Integer.parseInt(System.getProperty("slate.txtimer.mapinitcap", "1024"));
        } catch (Exception e) {
            logger.error("Error while parsing -Dslate.txtimer.mapinitcap to int, using 1024 by default", e);
            mapInitCap = 1024;
        }
        MAP_INIT_CAP = mapInitCap;
        //hash lockers number
        int hashLockNum;
        try {
            hashLockNum = Integer.parseInt(System.getProperty("slate.txtimer.hashlocknum", "64"));
        } catch (Exception e) {
            logger.error("Error while parsing -Dslate.txtimer.hashlocknum to int, using 64 by default", e);
            hashLockNum = 64;
        }
        HASH_LOCK_NUM = hashLockNum;
    }

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
