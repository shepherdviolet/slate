package sviolet.slate.springboot.modelx.loadbalance.auto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sviolet.slate.common.modelx.loadbalance.classic.GsonDataConverter;
import sviolet.slate.common.modelx.loadbalance.classic.SimpleOkHttpClient;
import sviolet.slate.springboot.auto.SlateProperties;
import sviolet.thistle.util.judge.CheckUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * slate.httpclients
 * 自动配置SimpleOkHttpClient(多个)
 *
 * @author S.Violet
 */
@Configuration
public class HttpClientsConfig {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientsConfig.class);

    /**
     * 自动配置SimpleOkHttpClient(多个)
     *
     * 只配置一个客户端, 且上下文中也没有手动创建的SimpleOkHttpClient时, 可以用@Autowired SimpleOkHttpClient直接获得客户端实例,
     * 否则要通过@Autowired HttpClients获得客户端集合
     */
    @Bean("slate.springboot.HttpClients")
    public HttpClients httpClients(SlateProperties slateProperties){
        Map<String, SimpleOkHttpClient> clients = new HashMap<>(1);

        if (slateProperties.getHttpclients() != null){

            for (Map.Entry<String, HttpClientProperties> entry : slateProperties.getHttpclients().entrySet()) {

                logger.info("Slate HttpClients: creating http client with tag:" + entry.getKey());

                if (entry.getValue() == null) {
                    logger.warn("Slate HttpClients: tag " + entry.getKey() + " has no properties, skip creating");
                }

                SimpleOkHttpClient client;

                if (!CheckUtils.isEmptyOrBlank(entry.getValue().getHosts())) {
                    logger.info("Slate HttpClients: hosts:" + entry.getValue().getHosts());
                    client = new SimpleOkHttpClient()
                            .setHosts(entry.getValue().getHosts());
                } else {
                    for (String host : entry.getValue().getHostList()) {
                        logger.info("Slate HttpClients: host:" + host);
                    }
                    client = new SimpleOkHttpClient()
                            .setHostArray(entry.getValue().getHostList());
                }

                clients.put(entry.getKey(), (SimpleOkHttpClient) client
                        .setInitiativeInspectInterval(entry.getValue().getInitiativeInspectInterval())
                        .setMaxThreads(entry.getValue().getMaxThreads())
                        .setMaxThreadsPerHost(entry.getValue().getMaxThreadsPerHost())
                        .setPassiveBlockDuration(entry.getValue().getPassiveBlockDuration())
                        .setConnectTimeout(entry.getValue().getConnectTimeout())
                        .setWriteTimeout(entry.getValue().getWriteTimeout())
                        .setReadTimeout(entry.getValue().getReadTimeout())
                        .setVerboseLog(entry.getValue().isVerboseLog())
                        .setDataConverter(new GsonDataConverter())
                        .setTag(entry.getKey())
                );

            }

        }

        return new HttpClientsImpl(clients);
    }

    /**
     * 只配置一个客户端, 且上下文中也没有手动创建的SimpleOkHttpClient时, 可以用@Autowired SimpleOkHttpClient直接获得客户端实例,
     * 否则要通过@Autowired HttpClients获得客户端集合
     */
    @Bean("slate.springboot.SimpleOkHttpClient")
    @ConditionalOnMissingBean
    public SimpleOkHttpClient httpClient(HttpClients httpClients){
        if (httpClients.size() == 1) {
            for (String tag : httpClients.tags()) {
                logger.debug("Slate HttpClients: only one instance, you can get instance by @Autowired SimpleOkHttpClient");
                return httpClients.get(tag);
            }
        }
        return null;
    }

}
