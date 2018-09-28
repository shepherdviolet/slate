package sviolet.slate.common.utilx.txtimer.def;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static sviolet.slate.common.utilx.txtimer.def.DefaultTxTimerProvider.UPDATE_MAX_ATTEMPTS;

class Unit {

    AtomicLong startTime = new AtomicLong(0);
    AtomicLong timeQuotient = new AtomicLong(0);
    AtomicInteger finishCount = new AtomicInteger(0);
    AtomicLong totalElapse = new AtomicLong(0);
    AtomicLong maxElapse = new AtomicLong(Long.MIN_VALUE);
    AtomicLong minElapse = new AtomicLong(Long.MAX_VALUE);

    boolean turnOver(long startTime, long quotient){
        long previousQuotient;
        while (true) {
            previousQuotient = timeQuotient.get();
            if (quotient <= previousQuotient) {
                return false;
            }
            if (timeQuotient.compareAndSet(previousQuotient, quotient)) {
                this.startTime.set(startTime);
                this.minElapse.set(0);
                this.maxElapse.set(0);
                this.totalElapse.set(0);
                this.finishCount.set(0);
                return true;
            }
        }
    }

    void record(long elapse){
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
