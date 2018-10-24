package sviolet.slate.springboot.x.net.loadbalance.auto;

import java.util.Map;

/**
 * <p>配置前缀: slate.httpclients</p>
 * <p>自动配置: SimpleOkHttpClient</p>
 *
 * @author S.Violet
 */
public class HttpClientProperties {

    /**
     * [可运行时修改]
     * 设置远端列表: 逗号分隔格式. 若hosts和hostList同时设置, 则只有hosts配置生效.
     * 例如:
     * hosts: http://localhost:8080,http://localhost:8081
     */
    private String hosts = "";

    /**
     * [可运行时修改]
     * 设置远端列表: 列表格式. 若hosts和hostList同时设置, 则只有hosts配置生效.
     * 例如:
     * hostList:
     *  - http://localhost:8080
     *  - http://localhost:8081
     */
    private String[] hostList = new String[0];

    /**
     * [可运行时修改]
     * 设置主动探测间隔 (主动探测器)
     */
    private long initiativeInspectInterval = 5000L;

    /**
     * [可运行时修改]
     * 最大请求线程数(仅异步请求时有效)
     */
    private int maxThreads = 200;

    /**
     * [可运行时修改]
     * 对应每个后端的最大请求线程数(仅异步请求时有效)
     */
    private int maxThreadsPerHost = 200;

    /**
     * [可运行时修改]
     * 最大闲置连接数. 客户端会保持与服务端的连接, 保持数量由此设置决定, 直到闲置超过5分钟. 默认16
     */
    private int maxIdleConnections = 16;

    /**
     * [可运行时修改]
     * <p>设置被动检测到网络故障时阻断后端的时间</p>
     *
     * <p>当请求服务端时, 发生特定的异常或返回特定的响应码(MultiHostOkHttpClient.needBlock方法决定), 客户端会将该
     * 后端服务器的IP/PORT标记为暂不可用状态, 阻断时长就是不可用的时长, 建议比主动探测器的探测间隔大.</p>
     */
    private long passiveBlockDuration = 6000L;

    /**
     * [可运行时修改]
     * 设置连接超时ms
     */
    private long connectTimeout = 3000L;

    /**
     * [可运行时修改]
     * 设置写数据超时ms
     */
    private long writeTimeout = 10000L;

    /**
     * [可运行时修改]
     * 设置读数据超时ms
     */
    private long readTimeout = 10000L;

    /**
     * [可运行时修改]
     * 设置最大读取数据长度(默认:10M), 单位bytes
     */
    private long maxReadLength = 10L * 1024L * 1024L;

    /**
     * [可运行时修改]
     * 设置MediaType
     */
    private String mediaType = "application/json;charset=utf-8";

    /**
     * [可运行时修改]
     * 设置编码
     */
    private String encode = "utf-8";

    /**
     * [可运行时修改]
     * 设置HTTP请求头参数
     */
    private Map<String, String> headers;

    /**
     * [可运行时修改]
     * 打印更多的日志, 默认关闭
     */
    private boolean verboseLog = true;

    /**
     * [可运行时修改]
     * 如果设置为false(默认), 当所有远端都被阻断时, nextHost方法返回一个后端.
     * 如果设置为true, 当所有远端都被阻断时, nextHost方法返回null.
     */
    private boolean returnNullIfAllBlocked = false;

    /**
     * [可运行时修改]
     * 设置阻断后的恢复期系数, 修复期时长 = blockDuration * recoveryCoefficient, 设置1则无恢复期
     */
    private int recoveryCoefficient = 10;

    /**
     * [可运行时修改]
     * 当HTTP返回码为指定返回码时, 阻断后端
     * codes 指定需要阻断的返回码, 例如:403,404
     */
    private String httpCodeNeedBlock;

    /**
     * [可运行时修改]
     * 启用/禁用TxTimer统计请求耗时(暂时只支持同步方式), 默认禁用
     */
    private boolean txTimerEnabled = false;

    /**
     * [可运行时修改]
     * 将主动探测器从默认的TELNET型修改为HTTP-GET型
     * urlSuffix 探测页面URL(例如:http://127.0.0.1:8080/health, 则在此处设置/health), 设置为+telnet+则使用默认的TELNET型
     */
    private String httpGetInspectorUrlSuffix = "+telnet+";

    public String getHosts() {
        return hosts;
    }

    public void setHosts(String hosts) {
        this.hosts = hosts;
    }

    public String[] getHostList() {
        return hostList;
    }

    public void setHostList(String[] hostList) {
        this.hostList = hostList;
    }

    public long getInitiativeInspectInterval() {
        return initiativeInspectInterval;
    }

    public void setInitiativeInspectInterval(long initiativeInspectInterval) {
        this.initiativeInspectInterval = initiativeInspectInterval;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public int getMaxThreadsPerHost() {
        return maxThreadsPerHost;
    }

    public void setMaxThreadsPerHost(int maxThreadsPerHost) {
        this.maxThreadsPerHost = maxThreadsPerHost;
    }

    public long getPassiveBlockDuration() {
        return passiveBlockDuration;
    }

    public void setPassiveBlockDuration(long passiveBlockDuration) {
        this.passiveBlockDuration = passiveBlockDuration;
    }

    public long getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public long getWriteTimeout() {
        return writeTimeout;
    }

    public void setWriteTimeout(long writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    public long getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
    }

    public boolean isVerboseLog() {
        return verboseLog;
    }

    public void setVerboseLog(boolean verboseLog) {
        this.verboseLog = verboseLog;
    }

    public boolean isReturnNullIfAllBlocked() {
        return returnNullIfAllBlocked;
    }

    public void setReturnNullIfAllBlocked(boolean returnNullIfAllBlocked) {
        this.returnNullIfAllBlocked = returnNullIfAllBlocked;
    }

    public int getRecoveryCoefficient() {
        return recoveryCoefficient;
    }

    public void setRecoveryCoefficient(int recoveryCoefficient) {
        this.recoveryCoefficient = recoveryCoefficient;
    }

    public boolean isTxTimerEnabled() {
        return txTimerEnabled;
    }

    public void setTxTimerEnabled(boolean txTimerEnabled) {
        this.txTimerEnabled = txTimerEnabled;
    }

    public int getMaxIdleConnections() {
        return maxIdleConnections;
    }

    public void setMaxIdleConnections(int maxIdleConnections) {
        this.maxIdleConnections = maxIdleConnections;
    }

    public long getMaxReadLength() {
        return maxReadLength;
    }

    public void setMaxReadLength(long maxReadLength) {
        this.maxReadLength = maxReadLength;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getEncode() {
        return encode;
    }

    public void setEncode(String encode) {
        this.encode = encode;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getHttpGetInspectorUrlSuffix() {
        return httpGetInspectorUrlSuffix;
    }

    public void setHttpGetInspectorUrlSuffix(String httpGetInspectorUrlSuffix) {
        this.httpGetInspectorUrlSuffix = httpGetInspectorUrlSuffix;
    }

    public String getHttpCodeNeedBlock() {
        return httpCodeNeedBlock;
    }

    public void setHttpCodeNeedBlock(String httpCodeNeedBlock) {
        this.httpCodeNeedBlock = httpCodeNeedBlock;
    }

}
