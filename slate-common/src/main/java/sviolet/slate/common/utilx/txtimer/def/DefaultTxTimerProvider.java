package sviolet.slate.common.utilx.txtimer.def;

import sviolet.slate.common.utilx.txtimer.TxTimerProvider;
import sviolet.thistle.model.concurrent.lock.UnsafeHashSpinLocks;
import sviolet.thistle.model.concurrent.lock.UnsafeSpinLock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>默认实现了交易耗时的统计, 并通过日志定时输出报告. 可以使用ThistleSpi替换实现.</p>
 *
 * @author S.Violet
 */
public class DefaultTxTimerProvider implements TxTimerProvider {

    //每分钟的毫秒数
    static final long MINUTE_MILLIS = 60L * 1000L;

    //用于start和stop之间的上下文传递
    private ThreadLocal<Record> record = new ThreadLocal<>();

    //组Map
    Map<String, Group> groups = new ConcurrentHashMap<>();
    //stop时记录找不到的计数器
    AtomicInteger missingCount = new AtomicInteger(0);

    //锁
    @SuppressWarnings("deprecation")
    UnsafeHashSpinLocks locks = new UnsafeHashSpinLocks(DefaultTxTimerConfig.hashLockNum);
    //日志输出器
    Reporter reporter = new Reporter(this);

    @Override
    public void start(String groupName, String transactionName) {
        //从ThreadLocal获取上下文, 若存在则不正常
        Record record = this.record.get();
        if (record != null) {
            //重复调用start没有做stop, 忘记做stop, 有可能会导致这个问题
            Transaction transaction = getGroup(record.getGroupName()).getTransaction(record.getTransactionName());
            transaction.duplicate();
        }
        //获得交易记录实例
        Transaction transaction = getGroup(groupName).getTransaction(transactionName);
        //标记为正在执行
        transaction.running();
        //在ThreadLocal记录上下文
        record = new Record(groupName, transactionName);
        this.record.set(record);
    }

    @Override
    public void stop() {
        //从ThreadLocal获取上下文, 若不存在则不正常
        Record record = this.record.get();
        if (record == null) {
            //重复调用stop, 没做start直接做stop, 有可能会导致这个问题
            missingCount.incrementAndGet();
            return;
        }
        //置空
        this.record.set(null);
        //计算时长
        long elapse = System.currentTimeMillis() - record.getStartTime();
        //获得交易记录实例
        Transaction transaction = getGroup(record.getGroupName()).getTransaction(record.getTransactionName());
        //标记为完成交易, 并记录时间
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
            //用UnsafeHashSpinLocks分散碰撞的可能性
            @SuppressWarnings("deprecation")
            UnsafeSpinLock lock = locks.getLock(groupName);
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
