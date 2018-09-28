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

    static final int REPORT_INTERVAL;
    static final int REPORT_INTERVAL_MILLIS;

    static final int MAP_INIT_CAP;
    static final int HASH_LOCK_NUM;
    static final int UPDATE_MAX_ATTEMPTS;
    static final int REPORT_LINES;

    static {
        REPORT_INTERVAL = getIntFromProperty("slate.txtimer.reportinterval", 5);
        REPORT_INTERVAL_MILLIS = REPORT_INTERVAL * 60 * 1000;
        if (REPORT_INTERVAL < 3 || REPORT_INTERVAL > 60) {
            throw new IllegalArgumentException("slate.txtimer.reportinterval must >= 3 and <= 60 (minus)");
        }

        MAP_INIT_CAP = getIntFromProperty("slate.txtimer.mapinitcap", 1024);
        HASH_LOCK_NUM = getIntFromProperty("slate.txtimer.hashlocknum", 32);
        UPDATE_MAX_ATTEMPTS = getIntFromProperty("slate.txtime.updateattemps", 10);
        REPORT_LINES = getIntFromProperty("slate.txtime.reportlines", 20);
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

    static final long MINUTE_MILLIS = 60L * 1000L;

    private ThreadLocal<Record> record = new ThreadLocal<>();

    Map<String, Group> groups = new ConcurrentHashMap<>(MAP_INIT_CAP);
    AtomicInteger missingCount = new AtomicInteger(0);

    StringHashLocks locks = new StringHashLocks(HASH_LOCK_NUM);
    Reporter reporter = new Reporter(this);

    @Override
    public void start(String groupName, String transactionName) {
        Record record = this.record.get();
        if (record != null) {
            Transaction transaction = getGroup(record.getGroupName()).getTransaction(record.getTransactionName());
            transaction.duplicate();
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
        this.record.set(null);
        long elapse = System.currentTimeMillis() - record.getStartTime();
        Transaction transaction = getGroup(record.getGroupName()).getTransaction(record.getTransactionName());
        transaction.finish(System.currentTimeMillis(), elapse);
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
        Group group = groups.get(groupName);
        if (group == null) {
            ReentrantLock lock = locks.getLock(groupName);
            try {
                lock.lock();
                group = groups.get(groupName);
                if (group == null) {
                    group = new Group(this);
                    groups.put(groupName, group);
                }
            } finally {
                lock.unlock();
            }
        }
        return group;
    }

}
