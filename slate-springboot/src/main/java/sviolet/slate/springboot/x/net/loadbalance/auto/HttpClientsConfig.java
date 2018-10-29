package sviolet.slate.springboot.x.net.loadbalance.auto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sviolet.slate.common.x.net.loadbalance.classic.SimpleOkHttpClient;
import sviolet.slate.springboot.auto.SlateProperties;

/**
 * <p>HttpClients配置: 自动配置SimpleOkHttpClient</p>
 * <p>配置前缀: slate.httpclients</p>
 *
 * @author S.Violet
 */
@Configuration
public class HttpClientsConfig {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientsConfig.class);

    /**
     * 自动配置HttpClients
     *
     * 只配置一个客户端, 且上下文中也没有手动创建的SimpleOkHttpClient时, 可以用@Autowired SimpleOkHttpClient直接获得客户端实例,
     * 否则要通过@Autowired HttpClients获得客户端集合
     */
    @Bean("slate.springboot.httpClients")
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public HttpClients httpClients(SlateProperties slateProperties){
        return new HttpClientsImpl(slateProperties.getHttpclients());
    }

    /**
     * 只配置一个客户端, 且上下文中也没有手动创建的SimpleOkHttpClient时, 可以用@Autowired SimpleOkHttpClient直接获得客户端实例,
     * 否则要通过@Autowired HttpClients获得客户端集合
     */
    @Bean("slate.springboot.simpleOkHttpClient")
    @ConditionalOnMissingBean
    public SimpleOkHttpClient httpClient(HttpClients httpClients){
        if (httpClients.size() == 1) {
            for (String tag : httpClients.tags()) {
                logger.info("HttpClients | Only one instance, you can get instance by @Autowired SimpleOkHttpClient");
                return httpClients.get(tag);
            }
        }
        return null;
    }

}
