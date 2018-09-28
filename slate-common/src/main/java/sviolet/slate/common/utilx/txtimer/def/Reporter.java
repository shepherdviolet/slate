package sviolet.slate.common.utilx.txtimer.def;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sviolet.thistle.util.concurrent.ConcurrentUtils;
import sviolet.thistle.util.concurrent.ThreadPoolExecutorUtils;
import sviolet.thistle.util.conversion.DateTimeUtils;

import java.util.*;
import java.util.concurrent.ExecutorService;

import static sviolet.slate.common.utilx.txtimer.def.DefaultTxTimerProvider.*;

class Reporter {

    private static final Logger logger = LoggerFactory.getLogger(Reporter.class);

    private DefaultTxTimerProvider provider;

    private ExecutorService reportThreadPool = ThreadPoolExecutorUtils.createLazy(60, "Slate-TxTimer-Report-%d");
    private volatile boolean shutdown = false;
    private long lastReportTime = 0;

    Reporter(DefaultTxTimerProvider provider) {
        this.provider = provider;

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                shutdown = true;
                reportThreadPool.shutdown();
            }
        }));
    }

    void notifyReport(){
        if (!shutdown) {
            reportThreadPool.execute(reportTask);
        }
    }

    private Runnable reportTask = new Runnable() {
        @Override
        public void run() {
            if (shutdown) {
                return;
            }
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < REPORT_INTERVAL_MILLIS) {
                try {
                    Thread.sleep(10000L);
                } catch (InterruptedException ignored) {
                }
                if (shutdown) {
                    return;
                }
            }
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastReportTime < REPORT_INTERVAL_MILLIS) {
                return;
            }
            report(currentTime);
            lastReportTime = currentTime;
        }
    };

    private void report(long currentTime){

        if (provider.missingCount.get() > 0) {
            print("Error Report", "ERROR!!! Times:" + provider.missingCount.get() + ", No record found in ThreadLocal when invoking TxTimer.stop()");
            print("Error Report", "Suggest 1: If you invoke TxTimer.stop() without or before TxTimer.start()?");
            print("Error Report", "Suggest 2: If you invoke TxTimer.stop() twice?");
            print("Error Report", "Suggest 2: If you invoke TxTimer.stop() and TxTimer.start() in different threads?");
        }

        long reportStartTime = currentTime - REPORT_INTERVAL_MILLIS;
        long reportEndTime = currentTime;

        Map<String, Group> groupsSnap = ConcurrentUtils.getSnapShot(provider.groups);
        for (Map.Entry<String, Group> groupEntry : groupsSnap.entrySet()) {

            Map<String, Transaction> transactionsSnap = ConcurrentUtils.getSnapShot(groupEntry.getValue().transactions);
            List<Info> infos = new ArrayList<>(transactionsSnap.size());

            for (Map.Entry<String, Transaction> transactionEntry : transactionsSnap.entrySet()) {

                int finishCountSum = 0;
                long totalElapseSum = 0;
                long maxElapse = Long.MIN_VALUE;
                long minElapse = Long.MAX_VALUE;

                List<Unit> unitList = transactionEntry.getValue().getUnits(reportStartTime, reportEndTime);
                for (Unit unit : unitList) {
                    //记录时间商数, 若最后该值变化, 说明单元被翻篇, 数据无效
                    long unitQuotient = unit.timeQuotient.get();
                    long unitStartTime = unit.startTime.get();
                    //排除非报告期间的单元
                    if (unitStartTime < reportStartTime || unitStartTime > reportEndTime) {
                        continue;
                    }
                    //取值
                    int unitFinishCount = unit.finishCount.get();
                    long unitTotalElapse = unit.totalElapse.get();
                    long unitMaxElapse = unit.maxElapse.get();
                    long unitMinElapse = unit.minElapse.get();
                    //若单元被翻篇, 数据无效
                    if (unit.timeQuotient.get() != unitQuotient) {
                        continue;
                    }
                    //若单元无交易, 则无效
                    if (unitFinishCount <= 0) {
                        continue;
                    }
                    finishCountSum += unitFinishCount;
                    totalElapseSum += unitTotalElapse;
                    maxElapse = Math.max(maxElapse, unitMaxElapse);
                    minElapse = Math.min(minElapse, unitMinElapse);
                }

                Info info = new Info();
                info.transactionName = transactionEntry.getKey();
                info.finishTotal = transactionEntry.getValue().finishCount.get();
                info.runningTotal = transactionEntry.getValue().runningCount.get();
                info.duplicateTotal = transactionEntry.getValue().duplicateCount.get();
                info.finish = finishCountSum;
                info.averageElapse = finishCountSum > 0 ? totalElapseSum / finishCountSum : 0;
                info.maxElapse = maxElapse;
                info.minElapse = minElapse;
                infos.add(info);

            }

            Collections.sort(infos, comparator);

            //输出日志
            String title = "Group (" + groupEntry.getKey() + ") Time ( " + DateTimeUtils.getDateTime(reportStartTime) + "~" + DateTimeUtils.getDateTime(reportEndTime);
            for (Info info : infos) {
                print(title, String.valueOf(info));
                if (info.duplicateTotal > 0) {
                    print(title, "ERROR!!! Times:" + info.duplicateTotal + ", TxTimer.start() been invoked before previous transaction finished (you invoke TxTimer.start() twice before TxTimer.stop(), start > start > stop)");
                }
            }

        }

        finish();

    }

    private Comparator<Info> comparator = new Comparator<Info>() {
        @Override
        public int compare(Info o1, Info o2) {
            return (int) (o2.averageElapse - o1.averageElapse);
        }
    };

    private static class Info {
        private String transactionName;
        private int finishTotal;
        private int runningTotal;
        private int duplicateTotal;
        private int finish;
        private long maxElapse;
        private long minElapse;
        private long averageElapse;

        @Override
        public String toString() {
            return transactionName +
                    " > last " + REPORT_INTERVAL +
                    " min ( cnt:" + finish +
                    ", avg:" + averageElapse +
                    "ms, max:" + maxElapse +
                    "ms, min:" + minElapse +
                    "ms ) total ( cnt:" + finishTotal +
                    ", ing:" + runningTotal +
                    " )";
        }
    }

    /* *********************************************************************************************************** */

    private String title;
    private int page = 1;
    private List<String> messagePool = new ArrayList<>(REPORT_LINES);

    private void print(String title, String msg){
        //如果标题变化
        if (!title.equals(this.title)) {
            //输出之前的日志
            if (this.title != null) {
                flush();
            }
            //重置页码
            page = 1;
            //设置标题
            this.title = title;
        }
        if (messagePool.size() >= 20) {
            flush();
        }
        messagePool.add(msg);
    }

    private void flush(){

        StringBuilder stringBuilder = new StringBuilder("\nTxTimer | ------------------------------------------------------------------------------------------------------------");
        stringBuilder.append("\nTxTimer | ");
        stringBuilder.append(title);
        stringBuilder.append("  Page ");
        stringBuilder.append(page);

        for (String msg : messagePool) {
            stringBuilder.append("\nTxTimer | ");
            stringBuilder.append(msg);
        }

        logger.info(stringBuilder.toString());

        messagePool.clear();
        page++;
    }

    private void finish(){
        flush();
        page = 1;
        title = null;
    }

}
