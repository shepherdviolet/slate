package sviolet.slate.springboot.modelx.loadbalance.auto;

/**
 * slate.httpclients
 * 自动配置SimpleOkHttpClient
 *
 * @author S.Violet
 */
public class HttpClientProperties {

    /**
     * 设置远端列表: 逗号分隔格式. 若hosts和hostList同时设置, 则只有hosts配置生效.
     * 例如:
     * hosts: http://localhost:8080,http://localhost:8081
     */
    private String hosts = "";

    /**
     * 设置远端列表: 列表格式. 若hosts和hostList同时设置, 则只有hosts配置生效.
     * 例如:
     * hostList:
     *  - http://localhost:8080
     *  - http://localhost:8081
     */
    private String[] hostList = new String[0];

    /**
     * 设置主动探测间隔 (主动探测器)
     */
    private long initiativeInspectInterval = 5000L;

    /**
     * 最大请求线程数(仅异步请求时有效)
     */
    private int maxThreads = 200;

    /**
     * 对应每个后端的最大请求线程数(仅异步请求时有效)
     */
    private int maxThreadsPerHost = 200;

    /**
     * <p>设置被动检测到网络故障时阻断后端的时间</p>
     *
     * <p>当请求服务端时, 发生特定的异常或返回特定的响应码(MultiHostOkHttpClient.needBlock方法决定), 客户端会将该
     * 后端服务器的IP/PORT标记为暂不可用状态, 阻断时长就是不可用的时长, 建议比主动探测器的探测间隔大.</p>
     */
    private long passiveBlockDuration = 6000L;

    /**
     * 设置连接超时ms
     */
    private long connectTimeout = 3000L;

    /**
     * 设置写数据超时ms
     */
    private long writeTimeout = 10000L;

    /**
     * 设置读数据超时ms
     */
    private long readTimeout = 10000L;

    /**
     * 打印更多的日志, 默认关闭
     */
    private boolean verboseLog = true;

    /**
     * 如果设置为false(默认), 当所有远端都被阻断时, nextHost方法返回一个后端.
     * 如果设置为true, 当所有远端都被阻断时, nextHost方法返回null.
     */
    private boolean returnNullIfAllBlocked = false;

    /**
     * 设置阻断后的恢复期系数, 修复期时长 = blockDuration * recoveryCoefficient, 设置1则无恢复期
     */
    private int recoveryCoefficient = 10;

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
}
