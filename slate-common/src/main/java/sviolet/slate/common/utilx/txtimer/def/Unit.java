package sviolet.slate.common.utilx.txtimer.def;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

class Unit {

    private String group;
    private String key;
    private long startTime;
    private AtomicInteger finishCount = new AtomicInteger(0);
    private AtomicLong elapse = new AtomicLong(0);
    private AtomicLong maxElapse = new AtomicLong(Long.MIN_VALUE);
    private AtomicLong minElapse = new AtomicLong(Long.MAX_VALUE);

    void add(long elapse){

    }

}
