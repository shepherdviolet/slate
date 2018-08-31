package sviolet.slate.springboot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sviolet.slate.common.modelx.loadbalance.classic.GsonDataConverter;
import sviolet.slate.common.modelx.loadbalance.classic.SimpleOkHttpClient;

/**
 * 自动配置SimpleOkHttpClient
 *
 * @author S.Violet
 */
@Configuration
@EnableConfigurationProperties(SimpleOkHttpClientProperties.class)
public class SimpleOkHttpClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(SimpleOkHttpClientConfig.class);

    @Bean("slate.springboot.simpleOkHttpClient")
    @ConditionalOnMissingBean
    public SimpleOkHttpClient simpleOkHttpClient(SimpleOkHttpClientProperties simpleOkHttpClientProperties){
        if (simpleOkHttpClientProperties.getHosts() == null) {
            return null;
        }
        logger.info("SimpleOkHttpClient instance created automatically");
        for (String host : simpleOkHttpClientProperties.getHosts()) {
            logger.info("Host:" + host);
        }
        return (SimpleOkHttpClient) new SimpleOkHttpClient()
                .setHostArray(simpleOkHttpClientProperties.getHosts())
                .setInitiativeInspectInterval(simpleOkHttpClientProperties.getInitiativeInspectInterval())
                .setMaxThreads(simpleOkHttpClientProperties.getMaxThreads())
                .setMaxThreadsPerHost(simpleOkHttpClientProperties.getMaxThreadsPerHost())
                .setPassiveBlockDuration(simpleOkHttpClientProperties.getPassiveBlockDuration())
                .setConnectTimeout(simpleOkHttpClientProperties.getConnectTimeout())
                .setWriteTimeout(simpleOkHttpClientProperties.getWriteTimeout())
                .setReadTimeout(simpleOkHttpClientProperties.getReadTimeout())
                .setDataConverter(new GsonDataConverter())
                .setVerboseLog(true);
    }

}
