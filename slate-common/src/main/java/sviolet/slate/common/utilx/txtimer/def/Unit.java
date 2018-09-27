package sviolet.slate.common.utilx.txtimer.def;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static sviolet.slate.common.utilx.txtimer.def.DefaultTxTimerProvider.UPDATE_MAX_ATTEMPTS;

class Unit {

    private String group;
    private String key;
    private long startTime;
    private AtomicInteger finishCount = new AtomicInteger(0);
    private AtomicLong totalElapse = new AtomicLong(0);
    private AtomicLong maxElapse = new AtomicLong(Long.MIN_VALUE);
    private AtomicLong minElapse = new AtomicLong(Long.MAX_VALUE);

    void add(long elapse){
        finishCount.incrementAndGet();
        totalElapse.addAndGet(elapse);
        //max elapse
        for (int i = 0 ; i < UPDATE_MAX_ATTEMPTS ; i++) {
            long previous = maxElapse.get();
            if (elapse > previous) {
                if (maxElapse.compareAndSet(previous, elapse)) {
                    break;
                }
            }
        }
        //min elapse
        for (int i = 0 ; i < UPDATE_MAX_ATTEMPTS ; i++) {
            long previous = minElapse.get();
            if (elapse < previous) {
                if (minElapse.compareAndSet(previous, elapse)) {
                    break;
                }
            }
        }
    }

}
