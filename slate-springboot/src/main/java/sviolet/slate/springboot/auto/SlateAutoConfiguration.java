package sviolet.slate.springboot.auto;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import sviolet.slate.springboot.x.net.loadbalance.auto.HttpClientsConfig;

/**
 * Slate自动配置(Spring Boot)
 *
 * @author S.Violet
 */
@Configuration
@EnableConfigurationProperties(SlateProperties.class)
@Import({
        ContextListenerConfig.class,
        HttpClientsConfig.class,
})
public class SlateAutoConfiguration {

}
