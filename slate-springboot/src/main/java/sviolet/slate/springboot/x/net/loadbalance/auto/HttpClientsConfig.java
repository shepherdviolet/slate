package sviolet.slate.springboot.x.net.loadbalance.auto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sviolet.slate.common.x.net.loadbalance.classic.GsonDataConverter;
import sviolet.slate.common.x.net.loadbalance.classic.SimpleOkHttpClient;
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

                String tag = entry.getKey();

                logger.info("Slate HttpClients | -------------------------------------------------------------");
                logger.info("Slate HttpClients | Creating " + tag);

                HttpClientProperties properties = entry.getValue();
                if (properties == null) {
                    logger.warn("Slate HttpClients | " + tag + " has no properties, skip");
                    continue;
                }

                SimpleOkHttpClient client = HttpClientCreator.create(tag, properties);
                clients.put(tag, client);

                if (logger.isInfoEnabled()) {
                    logger.info("Slate HttpClients | Created " + client);
                }

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
                logger.info("Slate HttpClients | Only one instance, you can get instance by @Autowired SimpleOkHttpClient");
                return httpClients.get(tag);
            }
        }
        return null;
    }

}
