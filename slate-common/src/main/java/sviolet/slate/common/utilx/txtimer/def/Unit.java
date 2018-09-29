package sviolet.slate.common.utilx.txtimer.def;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

class Unit {

    //开始时间
    AtomicLong startTime = new AtomicLong(0);
    //时间商数(重要), 1.确定统计单元的版本, 2.判断是否需要翻篇, 3.判断是否更新, 4.竞争该值获得重置权
    AtomicLong timeQuotient = new AtomicLong(0);
    //完成计数
    AtomicInteger finishCount = new AtomicInteger(0);
    //总耗时
    AtomicLong totalElapse = new AtomicLong(0);
    //最大耗时
    AtomicLong maxElapse = new AtomicLong(Long.MIN_VALUE);
    //最小耗时
    AtomicLong minElapse = new AtomicLong(Long.MAX_VALUE);

    /**
     * 尝试翻篇
     */
    boolean turnOver(long startTime, long quotient){
        long previousQuotient;
        while (true) {
            //获取之前的商数
            previousQuotient = timeQuotient.get();
            //如果当前商数等于或小于之前商数, 则不翻篇
            if (quotient <= previousQuotient) {
                return false;
            }
            //若当前商数大于之前商数, 尝试更新商数
            if (timeQuotient.compareAndSet(previousQuotient, quotient)) {
                //更新成功则重置统计单元
                this.startTime.set(startTime);
                this.minElapse.set(Long.MAX_VALUE);
                this.maxElapse.set(Long.MIN_VALUE);
                this.totalElapse.set(0);
                this.finishCount.set(0);
                return true;
            }
        }
    }

    /**
     * 记录耗时
     */
    void record(long elapse){
        //完成+1
        finishCount.incrementAndGet();
        //耗时累计
        totalElapse.addAndGet(elapse);
        //max elapse
        for (int i = 0 ; i < DefaultTxTimerConfig.updateAttempts; i++) {
            long previous = maxElapse.get();
            if (elapse > previous) {
                if (maxElapse.compareAndSet(previous, elapse)) {
                    break;
                }
            }
        }
        //min elapse
        for (int i = 0 ; i < DefaultTxTimerConfig.updateAttempts; i++) {
            long previous = minElapse.get();
            if (elapse < previous) {
                if (minElapse.compareAndSet(previous, elapse)) {
                    break;
                }
            }
        }
    }

}
