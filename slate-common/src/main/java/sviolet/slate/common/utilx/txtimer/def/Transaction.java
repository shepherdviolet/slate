package sviolet.slate.common.utilx.txtimer.def;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static sviolet.slate.common.utilx.txtimer.def.DefaultTxTimerProvider.*;

class Transaction {

    private DefaultTxTimerProvider provider;

    AtomicInteger finishCount = new AtomicInteger(0);
    AtomicInteger runningCount = new AtomicInteger(0);
    AtomicInteger duplicateCount = new AtomicInteger(0);

    private Unit[] units;

    Transaction(DefaultTxTimerProvider provider) {
        this.provider = provider;

        units = new Unit[REPORT_INTERVAL * 2];
        for (int i = 0 ; i < units.length ; i++) {
            units[i] = new Unit();
        }
    }

    void duplicate(){
        duplicateCount.incrementAndGet();

        provider.reporter.notifyReport();
    }

    void running(){
        runningCount.incrementAndGet();

        provider.reporter.notifyReport();
    }

    void finish(long currentTime, long elapse) {
        runningCount.decrementAndGet();
        finishCount.incrementAndGet();
        getUnit(currentTime).record(elapse);

        provider.reporter.notifyReport();
    }

    Unit getUnit(long currentTime){
        long currentMinute = currentTime / MINUTE_MILLIS;
        long quotient = currentMinute / units.length;
        int remainder = (int) (currentMinute % units.length);
        Unit unit = units[remainder];
        //turn over
        unit.turnOver(currentTime, quotient);
        return unit;
    }

    List<Unit> getUnits(long startTime, long endTime){
        long startMinute = startTime / MINUTE_MILLIS;
        int startRemainder = (int) (startMinute % units.length);
        long endMinute = endTime / MINUTE_MILLIS;
        int endRemainder = (int) (endMinute % units.length);

        List<Unit> unitList = new ArrayList<>(units.length);
        if (startRemainder < endRemainder) {
            for (int i = startRemainder ; i < endRemainder ; i++) {
                unitList.add(units[i]);
            }
        } else if (startRemainder > endRemainder){
            for (int i = startRemainder ; i < units.length ; i++) {
                unitList.add(units[i]);
            }
            for (int i = 0 ; i < endRemainder ; i++) {
                unitList.add(units[i]);
            }
        }
        return unitList;
    }

}
