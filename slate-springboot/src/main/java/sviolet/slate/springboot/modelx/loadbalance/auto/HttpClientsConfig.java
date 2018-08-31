package sviolet.slate.springboot.modelx.loadbalance.auto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sviolet.slate.common.modelx.loadbalance.classic.GsonDataConverter;
import sviolet.slate.common.modelx.loadbalance.classic.SimpleOkHttpClient;
import sviolet.slate.springboot.auto.SlateProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 自动配置SimpleOkHttpClient
 *
 * @author S.Violet
 */
@Configuration
public class HttpClientsConfig {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientsConfig.class);

    @Bean("slate.springboot.HttpClients")
    public HttpClients httpClients(SlateProperties slateProperties){
        Map<String, SimpleOkHttpClient> clients = new HashMap<>(1);

        if (slateProperties.getHttpclients() != null){

            for (Map.Entry<String, HttpClientProperties> entry : slateProperties.getHttpclients().entrySet()) {

                logger.info("Slate HttpClients: creating http client with tag:" + entry.getKey());

                if (entry.getValue() == null) {
                    logger.warn("Slate HttpClients: tag " + entry.getKey() + " has no properties, skip creating");
                }

                for (String host : entry.getValue().getHosts()) {
                    logger.info("Slate HttpClients: host:" + host);
                }

                clients.put(entry.getKey(), (SimpleOkHttpClient) new SimpleOkHttpClient()
                        .setHostArray(entry.getValue().getHosts())
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
     * slate.httpclients只配置了一个, 且上下文中没有手动创建过任何SimpleOkHttpClient时,
     * 可以用@Autoaware SimpleOkHttpClient获得客户端实例
     */
    @Bean("slate.springboot.SimpleOkHttpClient")
    @ConditionalOnMissingBean
    public SimpleOkHttpClient httpClient(HttpClients httpClients){
        if (httpClients.size() == 1) {
            for (String tag : httpClients.tags()) {
                logger.debug("Slate HttpClients: only one instance, you can get instance by @Autoaware SimpleOkHttpClient");
                return httpClients.get(tag);
            }
        }
        return null;
    }

}
