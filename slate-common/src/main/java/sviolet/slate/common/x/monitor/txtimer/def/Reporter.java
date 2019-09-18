/*
 * Copyright (C) 2015-2018 S.Violet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project GitHub: https://github.com/shepherdviolet/slate
 * Email: shepherdviolet@163.com
 */

package sviolet.slate.common.x.monitor.txtimer.def;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sviolet.thistle.util.concurrent.ConcurrentUtils;
import sviolet.thistle.util.concurrent.ThreadPoolExecutorUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static sviolet.slate.common.x.monitor.txtimer.def.DefaultTxTimerProvider2.*;

class Reporter {

    private static final Logger logger = LoggerFactory.getLogger(Reporter.class);

    private DefaultTxTimerProvider2 provider;

    private ExecutorService reportThreadPool = ThreadPoolExecutorUtils.createLazy(60, "Slate-TxTimer-Report-%d");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private volatile boolean shutdown = false;
    private long lastReportAllTime = System.currentTimeMillis();

    Reporter(DefaultTxTimerProvider2 provider) {
        this.provider = provider;
        initMessagePool();

        //监听进程结束事件
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                shutdown = true;
                try {
                    reportThreadPool.shutdownNow();
                } catch (Exception ignore) {
                }
            }
        }));
    }

    /**
     * 提醒日志输出线程工作, 使用了Lazy线程池, 反复调用execute至多执行2次, 这样在有交易时, 会进行日志输出, 若无交易, 则线程会结束
     */
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
            //等待报告输出间隔时间到
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < provider.reportIntervalMillis) {
                try {
                    Thread.sleep(MINUTE_MILLIS);
                } catch (InterruptedException ignored) {
                }
                if (shutdown) {
                    return;
                }
            }
            //输出报告
            report(System.currentTimeMillis());
        }
    };

    private void report(long currentTime){

        //判断是否打印全量日志
        boolean reportAll = false;
        if (System.currentTimeMillis() - lastReportAllTime > DefaultTxTimerConfig.reportAllIntervalMillis){
            lastReportAllTime = System.currentTimeMillis();
            reportAll = true;
        }

        //报告结束时间(抹去秒数)
        long reportEndTime = currentTime / MINUTE_MILLIS * MINUTE_MILLIS;
        //报告起始时间
        long reportStartTime = reportEndTime - provider.reportIntervalMillis;

        if (DefaultTxTimerConfig.reportPrintsPerMinute) {
            //一分钟一分钟打印
            int elapseMinutes = (int) (provider.reportIntervalMillis / MINUTE_MILLIS);
            for (int i = 0 ; i < elapseMinutes ; i++) {
                long startTime = reportStartTime + i * MINUTE_MILLIS;
                report(startTime, startTime + MINUTE_MILLIS, reportAll);
            }
        }else {
            //合计打印
            report(reportStartTime, reportEndTime, reportAll);
        }
    }

    private void report(long reportStartTime, long reportEndTime, boolean reportAll) {
        //遍历groups
        Map<String, Group> groupsSnap = ConcurrentUtils.getSnapShot(provider.groups);
        for (Map.Entry<String, Group> groupEntry : groupsSnap.entrySet()) {

            Map<String, Transaction> transactionsSnap = ConcurrentUtils.getSnapShot(groupEntry.getValue().transactions);
            List<Info> infos = new ArrayList<>(transactionsSnap.size());

            //遍历transactions
            for (Map.Entry<String, Transaction> transactionEntry : transactionsSnap.entrySet()) {

                //将时间段内的多个统计单元做合并计算
                int finishCountSum = 0;
                long totalElapseSum = 0;
                long maxElapse = Long.MIN_VALUE;
                long minElapse = Long.MAX_VALUE;
                int unitNum = 0;

                //遍历时间段内的统计单元
                List<Unit> unitList = transactionEntry.getValue().getUnits(reportStartTime, reportEndTime);
                for (Unit unit : unitList) {
                    //记录时间商数, 若最后该值变化, 说明单元被翻篇, 数据无效
                    long unitQuotient = unit.timeQuotient.get();
                    long unitStartTime = unit.startTime.get();
                    //排除非报告期间的单元
                    if (unitStartTime < reportStartTime || unitStartTime >= reportEndTime) {
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
                    unitNum++;
                }

                //交易统计结果
                Info info = new Info();
                info.transactionName = transactionEntry.getKey();
                info.finishTotal = transactionEntry.getValue().finishCount.get();
                info.runningTotal = transactionEntry.getValue().runningCount.get();
                info.finish = finishCountSum;
                info.averageElapse = finishCountSum > 0 ? totalElapseSum / finishCountSum : 0;
                info.maxElapse = maxElapse != Long.MIN_VALUE ? maxElapse : 0;
                info.minElapse = minElapse != Long.MAX_VALUE ? minElapse : 0;
                info.unitNum = unitNum;

                //粗略地估算总平均耗时
                if (transactionEntry.getValue().averageElapseTotal == 0){
                    transactionEntry.getValue().averageElapseTotal = info.averageElapse;
                } else if (info.finish > 0) {
                    float changeRate;
                    if (info.finishTotal > 10000) {
                        changeRate = 0.03f;
                    } else {
                        changeRate = (float)info.finish / (float)info.finishTotal;
                        if (changeRate < 0.03f) {
                            changeRate = 0.03f;
                        }
                    }
                    transactionEntry.getValue().averageElapseTotal =
                            (long) ((float)transactionEntry.getValue().averageElapseTotal * (1f - changeRate) +
                                   (float)info.averageElapse * changeRate);
                }

                info.averageElapseTotal = transactionEntry.getValue().averageElapseTotal;
                infos.add(info);

            }

            //排序
            Collections.sort(infos, comparator);

            //输出日志
            String title = "Group (" + groupEntry.getKey() + ") Time (" + dateFormat.format(new Date(reportStartTime)) + " - " + dateFormat.format(new Date(reportEndTime)) + ")" + (reportAll ? " ReportAll" : "");
            for (Info info : infos) {
                if (reportAll ||
                        !DefaultTxTimerConfig.thresholdEnabled ||
                        info.averageElapse >= DefaultTxTimerConfig.thresholdAvg ||
                        info.maxElapse >= DefaultTxTimerConfig.thresholdMax ||
                        info.minElapse >= DefaultTxTimerConfig.thresholdMin) {
                    print(title, String.valueOf(info));
                }
            }

        }

        //保证日志都写完
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
        private long averageElapseTotal;
        private int finish;
        private long maxElapse;
        private long minElapse;
        private long averageElapse;
        private int unitNum;

        @Override
        public String toString() {
            return transactionName +
                    " > last " + unitNum +
                    " min ( cnt:" + finish +
                    ", avg:" + averageElapse +
                    "ms, max:" + maxElapse +
                    "ms, min:" + minElapse +
                    "ms ) total ( cnt:" + finishTotal +
                    ", ing:" + runningTotal +
                    ", est-avg:" + averageElapseTotal + "ms )";
        }
    }

    /* *********************************************************************************************************** */

    private String title;
    private int page = 1;
    private List<String> messagePool;

    private void initMessagePool(){
        messagePool = new ArrayList<>(provider.pageLines);
    }

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
        if (messagePool.size() >= provider.pageLines) {
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
