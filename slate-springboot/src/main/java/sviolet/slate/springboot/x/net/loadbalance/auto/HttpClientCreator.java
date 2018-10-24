package sviolet.slate.springboot.x.net.loadbalance.auto;

import sviolet.slate.common.x.net.loadbalance.classic.GsonDataConverter;
import sviolet.slate.common.x.net.loadbalance.classic.SimpleOkHttpClient;
import sviolet.thistle.util.judge.CheckUtils;

class HttpClientCreator {

    /**
     * 根据tag和配置创建一个HttpClient
     * @param tag tag
     * @param properties 配置
     * @return SimpleOkHttpClient
     */
    static SimpleOkHttpClient create(String tag, HttpClientProperties properties) {
        //tag
        SimpleOkHttpClient client = (SimpleOkHttpClient) new SimpleOkHttpClient().setTag(tag);

        //hosts
        if (!CheckUtils.isEmptyOrBlank(properties.getHosts())) {
            client.setHosts(properties.getHosts());
        } else {
            client.setHostArray(properties.getHostList());
        }

        //properties
        return (SimpleOkHttpClient) client
                .setInitiativeInspectInterval(properties.getInitiativeInspectInterval())
                .setReturnNullIfAllBlocked(properties.isReturnNullIfAllBlocked())
                .setHttpGetInspector(properties.getHttpGetInspectorUrlSuffix())
                .setPassiveBlockDuration(properties.getPassiveBlockDuration())
                .setMediaType(properties.getMediaType())
                .setEncode(properties.getEncode())
                .setHeaders(properties.getHeaders())
                .setRecoveryCoefficient(properties.getRecoveryCoefficient())
                .setMaxIdleConnections(properties.getMaxIdleConnections())
                .setMaxThreads(properties.getMaxThreads())
                .setMaxThreadsPerHost(properties.getMaxThreadsPerHost())
                .setConnectTimeout(properties.getConnectTimeout())
                .setWriteTimeout(properties.getWriteTimeout())
                .setReadTimeout(properties.getReadTimeout())
                .setMaxReadLength(properties.getMaxReadLength())
                .setHttpCodeNeedBlock(properties.getHttpCodeNeedBlock())
                .setVerboseLog(properties.isVerboseLog())
                .setTxTimerEnabled(properties.isTxTimerEnabled())
                .setDataConverter(new GsonDataConverter());
    }

    static SimpleOkHttpClient recreate(SimpleOkHttpClient client, HttpClientProperties oldProperties, HttpClientProperties newProperties){
        return null;
    }

}
