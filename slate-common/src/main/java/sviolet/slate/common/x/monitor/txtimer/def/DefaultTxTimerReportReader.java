/*
 * Copyright (C) 2015-2019 S.Violet
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
import sviolet.thistle.util.file.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import static sviolet.slate.common.x.monitor.txtimer.def.DefaultTxTimerProvider2.MINUTE_MILLIS;

/**
 * <p>[非线程安全 | Not Thread Safe] TxTimer统计报告读取器. </p>
 *
 * <p>建议TxTimer设置slate.txtimer.report.printpermin=true</p>
 *
 * <p>本类是设计用来进行简单地离线地统计数据分析的, 如果写成线程安全的, 会增加代码的复杂度, 同时降低单线程执行时的效率.
 * 所以在使用时, 请在单线程中创建该类, 然后读取, 最后获取结果. 本类提供了一个shutdown方法, 这是唯一一个可以在其他线程
 * 调用的方法, 但是这个方法并不会马上让读取停止, 读取是否停止以读取线程是否执行完或抛异常为准. </p>
 *
 * @author S.Violet
 */
public class DefaultTxTimerReportReader {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SimpleDateFormat dateFormat1 = new SimpleDateFormat("MM/dd HH:mm");//old version
    private final SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    //group -> name -> time
    private Map<String, Map<String, TreeMap<Long, Record>>> data = new HashMap<>(16);

    private String currentGroup;
    private long currentStartTime;
    private int currentElapseMinutes;

    private String currentPath;
    private int currentLineNum;
    private volatile boolean reading = false;
    private boolean shutdown = false;

    private ProgressListener progressListener;

    /**
     * 创建一个读取器
     */
    public static DefaultTxTimerReportReader newReader(){
        return new DefaultTxTimerReportReader();
    }

    /**
     * [非线程安全 | Not Thread Safe] 获取所有组的数据.
     * 注意!!! 这里为了减小开销, 直接返回了内部对象, 如果对返回值进行写操作, 会改变读取器内部的数据.
     * 一般建议在全部文件读取完成后, 再进行get.
     *
     * @return 所有组的数据, 不为空
     */
    public Map<String, Map<String, TreeMap<Long, Record>>> getAllRecords(){
        return data;
    }

    /**
     * [非线程安全 | Not Thread Safe] 获取一个组的所有交易数据.
     * 注意!!! 这里为了减小开销, 直接返回了内部对象, 如果对返回值进行写操作, 会改变读取器内部的数据.
     * 一般建议在全部文件读取完成后, 再进行get.
     *
     * @param group 组名
     * @return 一个组的所有交易数据, 可能为空
     */
    public Map<String, TreeMap<Long, Record>> getRecords(String group) {
        return data.get(group);
    }

    /**
     * [非线程安全 | Not Thread Safe] 获取一个交易的所有数据.
     * 注意!!! 这里为了减小开销, 直接返回了内部对象, 如果对返回值进行写操作, 会改变读取器内部的数据.
     * 一般建议在全部文件读取完成后, 再进行get.
     *
     * @param group 组名
     * @param name 交易名
     * @return 一个交易的所有数据, 可能为空
     */
    public TreeMap<Long, Record> getRecords(String group, String name) {
        Map<String, TreeMap<Long, Record>> groupData = getRecords(group);
        if (groupData == null) {
            return null;
        }
        return groupData.get(name);
    }

    /**
     * [非线程安全 | Not Thread Safe] 从一个文件读取数据
     * @param file 数据源
     */
    public DefaultTxTimerReportReader read(File file, Charset charset) throws IOException {
        //简单的防多线程操作检查
        if (reading) {
            throw new RuntimeException("This class is not thread safe, Do not invoke read in multiple threads at the same time");
        }
        reading = true;
        try {
            read0(file, charset);
        } finally {
            reading = false;
        }
        return this;
    }

    /**
     * [非线程安全 | Not Thread Safe] 从一个目录读取数据
     * @param directory 目录
     * @param namePattern 文件名的匹配正则表达式
     * @param recursive 是否递归处理子目录
     * @param charset 字符集
     */
    public DefaultTxTimerReportReader read(String directory, String namePattern, boolean recursive, Charset charset) {
        return read(directory, Pattern.compile(namePattern), recursive, charset);
    }

    /**
     * [非线程安全 | Not Thread Safe] 从一个目录读取数据
     * @param directory 目录
     * @param namePattern 文件名的匹配正则表达式
     * @param recursive 是否递归处理子目录
     * @param charset 字符集
     */
    public DefaultTxTimerReportReader read(String directory, Pattern namePattern, boolean recursive, Charset charset) {
        //简单的防多线程操作检查
        if (reading) {
            throw new RuntimeException("This class is not thread safe, Do not invoke read in multiple threads at the same time");
        }
        reading = true;
        try {
            read0(directory, namePattern, recursive, charset);
        } finally {
            reading = false;
        }
        return this;
    }

    /**
     * 设置进度监听器
     * @param progressListener 监听器
     */
    public DefaultTxTimerReportReader setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
        return this;
    }

    /**
     * 终止读取, 调用该方法后, 正在进行的读取进程会在一定时间后结束, 另外, 当前实例将无法重新读取新的文件(请重新创建实例).
     * 这是唯一一个可以在其他线程调用的方法. 正确的用法是, 在读取线程中, 创建实例, 读取文件, 获取结果, 程序应保证实例不被
     * 其他线程操作(除了这个方法外), 当需要中止读取时, 可以在其他线程中调用该方法, 最终读取是否结束, 要以读取线程那边是否
     * 执行完为准.
     */
    public DefaultTxTimerReportReader shutdown(){
        shutdown = true;
        return this;
    }

    /**
     * 是否被终止, 该状态不能体现读取是否结束, 只能体现shutdown()是否被调用过! 读取是否停止以读取线程是否执行完或抛异常为准!
     */
    public boolean isShutdown(){
        return shutdown;
    }

    private void read0(File file, Charset charset) throws IOException {
        if (shutdown) {
            return;
        }
        if (file == null || !file.exists() || !file.isFile()) {
            logger.warn("file '" + file + "' is null / not exists / not a file");
            return;
        }
        if (charset == null) {
            logger.warn("charset is null");
            return;
        }

        //当前处理的文件路径
        currentPath = file.getAbsolutePath();
        currentLineNum = 0;

        if (logger.isDebugEnabled()) {
            logger.debug("Reading " + currentPath);
        }

        final ProgressListener progressListener = this.progressListener;
        if (progressListener != null) {
            progressListener.enterFile(currentPath);
        }

        FileUtils.readLines(file, 1024 * 4, 1024 * 4, 1024 * 64, (data, outOfLimit) -> {
            currentLineNum++;

            //忽略过长的行
            if (outOfLimit) {
                return true;
            }

            String line = new String(data, charset);

            //忽略非TxTimer行
            if (!line.startsWith("TxTimer | ")) {
                return true;
            }

            if (progressListener != null) {
                progressListener.enterLine(currentLineNum, line);
            }

            //寻找数据行的分割点(交易名和数据的分割点): TxTimer | <name> > last <?> min
            int dataLineSeparatorIndex = line.lastIndexOf(" > last ");

            //确认是数据行: TxTimer | <name> > last <?> min
            if (dataLineSeparatorIndex >= 10) {
                handleDataLine(line.substring(10, dataLineSeparatorIndex), line.substring(dataLineSeparatorIndex));
            }

            //确认是标题行: TxTimer | Group (<?>) Time (<?> - <?>)  Page <?>
            if (line.indexOf("Group (", 10) >= 0) {
                handleTitleLine(line);
            }

            //如果被终止, 就停止读取
            return !shutdown;
        });

        if (progressListener != null) {
            progressListener.exitFile(currentPath);
        }
    }

    private void read0(String directory, Pattern namePattern, boolean recursive, Charset charset) {
        if (shutdown) {
            return;
        }
        if (directory == null) {
            logger.warn("directory is null");
            return;
        }
        if (namePattern == null) {
            logger.warn("namePattern is null");
            return;
        }
        if (charset == null) {
            logger.warn("charset is null");
            return;
        }

        File directoryFile = new File(directory);
        if (!directoryFile.exists() || !directoryFile.isDirectory()) {
            logger.warn("directory '" + directoryFile + "' is not exists / not a directory");
            return;
        }

        File[] files = directoryFile.listFiles();
        if (files == null || files.length == 0) {
            logger.debug("Empty directory " + directory);
            return;
        }

        for (File file : files) {
            if (file.isFile() && namePattern.matcher(file.getName()).matches()) {
                try {
                    read0(file, charset);
                } catch (IOException e) {
                    logger.warn("Error while reading file " + file.getAbsolutePath(), e);
                }
            } else if (file.isDirectory() && recursive) {
                read0(file.getAbsolutePath(), namePattern, recursive, charset);
            }
            if (shutdown) {
                return;
            }
        }
    }

    /**
     * 处理数据行
     */
    private void handleDataLine(String name, String data) {
        if (currentGroup == null) {
            logger.warn("skip line, because the title line is bad, path:" + currentPath + ", lineNum:" + currentLineNum);
            return;
        }
        if (currentStartTime <= 0 || currentElapseMinutes <= 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("skip line, because the elapse is 0, path:" + currentPath + ", lineNum:" + currentLineNum);
            }
            return;
        }

        try {
            // > last <?> min ( cnt:<?>, avg:<?>ms, max:<?>ms, min:<?>ms ) total ( cnt:<?>, ing:<?>, est-avg: <?>ms )
            int dataStartIndex = data.indexOf("min (");
            if (dataStartIndex < 0) {
                throw new Exception("Can not find 'min ('");
            }

            //截取数据, 并换算成每分钟的值
            double count = (double) getInteger(data, "cnt:", ",", dataStartIndex) / (double) currentElapseMinutes;
            double avgElapse = getInteger(data, "avg:", "ms", dataStartIndex);
            int maxElapse = getInteger(data, "max:", "ms", dataStartIndex);
            int minElapse = getInteger(data, "min:", "ms", dataStartIndex);

            //跳过没交易的记录
            if (count <= 0d) {
                return;
            }

            //保存数据
            for (int i = 0 ; i < currentElapseMinutes ; i++) {
                Record record = getRecord(currentGroup, name, currentStartTime + i * MINUTE_MILLIS);
                //计算平均耗时
                double avgElapsePercent = count / (count + record.count);
                record.avgElapse = avgElapse * avgElapsePercent + record.avgElapse * (1d - avgElapsePercent);
                //交易数
                record.count = count + record.count;
                //最大耗时/最小耗时
                record.maxElapse = Math.max(maxElapse, record.maxElapse);
                record.minElapse = Math.min(minElapse, record.minElapse);
            }

        } catch (Exception e) {
            logger.warn("invalid data line, name:" + name + ", data:" + data + ", path:" + currentPath + ", lineNum:" + currentLineNum, e);
        }
    }

    /**
     * 处理标题行
     */
    private void handleTitleLine(String data) {
        try {
            //get group name, TxTimer | Group (<?>) Time (<?> - <?>)  Page <?>
            String group = getString(data, "Group (", ")", 10);

            //get startTime & endTime
            String timeRange = getString(data, "Time (", ")", 10);
            String[] timeRanges = timeRange.split(" - ");
            if (timeRanges.length != 2) {
                throw new Exception("Illegal time range:" + timeRange);
            }
            long startTime = parseTime(timeRanges[0]);
            long endTime = parseTime(timeRanges[1]);

            //set state
            this.currentGroup = group;
            this.currentStartTime = startTime;
            this.currentElapseMinutes = (int) ((endTime - startTime) / MINUTE_MILLIS);

        } catch (Exception e) {
            logger.warn("invalid title line, string:" + data + ", path:" + currentPath + ", lineNum:" + currentLineNum, e);

            //reset state
            this.currentGroup = null;
            this.currentStartTime = 0;
            this.currentElapseMinutes = 0;
        }
    }

    /**
     * 截取字符串
     */
    private String getString(String data, String startString, String endString, int offset) throws Exception {
        try {
            int startIndex = data.indexOf(startString, offset);
            if (startIndex < 0) {
                throw new Exception("Can not find '" + startString + "'");
            }
            startIndex += startString.length();
            int endIndex = data.indexOf(endString, startIndex);
            if (endIndex < 0) {
                throw new Exception("Can not find '" + endString + "'");
            }
            return data.substring(startIndex, endIndex).trim();
        } catch (Exception e) {
            throw new Exception("Error while getting value between '" + startString + "' and '" + endString + "', offset:" + offset, e);
        }
    }

    /**
     * 截取数字
     */
    private int getInteger(String data, String startString, String endString, int offset) throws Exception {
        String value = getString(data, startString, endString, offset);
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            throw new Exception("Error while parsing integer:" + value, e);
        }
    }

    /**
     * 解析日期
     */
    private long parseTime(String timeString) throws ParseException {
//        timeString = timeString.trim();
        return timeString.length() == 19 ?
                dateFormat2.parse(timeString).getTime() :
                dateFormat1.parse(timeString).getTime();
    }

    /**
     * 获取指定组名/名称/时间的记录
     */
    private Record getRecord(String group, String name, long time){
        String group0 = group.intern();
        String name0 = name.intern();
        return data.computeIfAbsent(group0, k -> new HashMap<>(256))
                .computeIfAbsent(name0, k -> new TreeMap<>())
                .computeIfAbsent(time, k -> new Record(group0, name0, time));
    }

    /**
     * 每个交易一秒钟内的统计数据
     */
    public static class Record {

        private String group;
        private String name;
        private long time;

        private double count = 0f;
        private double avgElapse = 0f;
        private int maxElapse = Integer.MIN_VALUE;
        private int minElapse = Integer.MAX_VALUE;

        private Record(String group, String name, long time) {
            this.group = group;
            this.name = name;
            this.time = time;
        }

        /**
         * 组名
         */
        public String getGroup() {
            return group;
        }

        /**
         * 交易名
         */
        public String getName() {
            return name;
        }

        /**
         * 记录时间(起始时间, 时长60s)
         */
        public long getTime() {
            return time;
        }

        /**
         * 交易数量
         */
        public double getCount() {
            return count;
        }

        /**
         * 平均耗时
         */
        public double getAvgElapse() {
            return avgElapse;
        }

        /**
         * 最大耗时
         */
        public int getMaxElapse() {
            return maxElapse;
        }

        /**
         * 最小耗时
         */
        public int getMinElapse() {
            return minElapse;
        }

        @Override
        public String toString() {
            return "Record{" +
                    "group='" + getGroup() + '\'' +
                    ", name='" + getName() + '\'' +
                    ", time=" + getTime()+
                    ", count=" + getCount() +
                    ", avgElapse=" + getAvgElapse() +
                    ", maxElapse=" + getMaxElapse() +
                    ", minElapse=" + getMinElapse() +
                    '}';
        }
    }

    /**
     * 进度监听器
     */
    public interface ProgressListener {

        /**
         * 读取一个文件开始
         */
        void enterFile(String filePath);

        /**
         * 读取一行开始
         */
        void enterLine(int lineNum, String line);

        /**
         * 读取一个文件结束
         */
        void exitFile(String filePath);

    }

}
