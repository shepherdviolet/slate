package sviolet.slate.springboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 自动配置(Spring Boot)
 *
 * @author S.Violet
 */
@Configuration
@Import({
        SimpleOkHttpClientConfig.class,
        ContextListenerConfig.class,
})
public class SlateAutoConfiguration {

}
