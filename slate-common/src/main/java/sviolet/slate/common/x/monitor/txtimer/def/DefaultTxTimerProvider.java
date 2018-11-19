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
import sviolet.thistle.x.common.thistlespi.ThistleSpiUtils;
import sviolet.slate.common.x.monitor.txtimer.TxTimerProvider;
import sviolet.thistle.model.common.SysPropFirstProperties;
import sviolet.thistle.model.concurrent.lock.UnsafeHashSpinLocks;
import sviolet.thistle.model.concurrent.lock.UnsafeSpinLock;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>默认实现了交易耗时的统计, 并通过日志定时输出报告. 可以使用ThistleSpi替换实现.</p>
 *
 * @author S.Violet
 */
public class DefaultTxTimerProvider implements TxTimerProvider {

    /**
     * 启动后固定
     * [基本设置]日志报告输出间隔(周期), 单位:分钟, [2-60], 默认5
     */
    int reportInterval;
    int reportIntervalMillis;
    /**
     * 启动后固定
     * [调优设置]日志每次输出的最大行数, 大于该行数会分页, 默认20
     */
    int pageLines;
    /**
     * 启动后固定
     * [调优设置]内部Map的初始大小, 大于观测点数量为宜
     */
    int mapInitCap;
    /**
     * 启动后固定
     * [调优设置]StringHashLocks的锁数量
     */
    int hashLockNum;
    /**
     * 启动后固定
     * [调优设置]内部一些非锁更新操作的最大尝试次数
     */
    int updateAttempts;

    private void intiProperties(Properties parameters) {
        SysPropFirstProperties enhancedParameters = ThistleSpiUtils.wrapPropertiesBySysProp(parameters);

        reportInterval = enhancedParameters.getInt("slate.txtimer.report.interval", 5);
        if (reportInterval < 2 || reportInterval > 60) {
            throw new IllegalArgumentException("slate.txtimer.report.interval must >= 2 and <= 60 (minute)");
        }
        logger.info("TxTimer | Config: Ordinary Report every " + reportInterval + " minutes");
        reportIntervalMillis = reportInterval * 60 * 1000;

        pageLines = enhancedParameters.getInt("slate.txtimer.pagelines", 20);
        mapInitCap = enhancedParameters.getInt("slate.txtimer.mapinitcap", 128);
        hashLockNum = enhancedParameters.getInt("slate.txtimer.hashlocknum", 16);
        updateAttempts = enhancedParameters.getInt("slate.txtimer.updateattemps", 10);
    }

    /* ******************************************************************************************************** */

    private static final Logger logger = LoggerFactory.getLogger(DefaultTxTimerProvider.class);

    //每分钟的毫秒数
    static final long MINUTE_MILLIS = 60L * 1000L;

    //用于start和stop之间的上下文传递
    private ThreadLocal<Record> record = new ThreadLocal<>();

    //组Map
    Map<String, Group> groups = new ConcurrentHashMap<>();
    //stop时记录找不到的计数器
    AtomicInteger missingCount = new AtomicInteger(0);

    //锁
    UnsafeHashSpinLocks locks;
    //日志输出器
    Reporter reporter = new Reporter(this);

    public DefaultTxTimerProvider(Properties parameters) {
        intiProperties(parameters);
        locks = new UnsafeHashSpinLocks(hashLockNum);
    }

    @Override
    public void start(String groupName, String transactionName) {
        if (groupName == null) {
            groupName = "<null>";
        }
        if (transactionName == null) {
            transactionName = "<null>";
        }
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
