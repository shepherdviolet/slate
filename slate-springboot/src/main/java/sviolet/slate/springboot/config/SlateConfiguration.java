package sviolet.slate.springboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import sviolet.slate.common.util.common.SlateServletContextListener;

import javax.servlet.ServletContextListener;

/**
 * 自动配置(Spring Boot)
 *
 * @author S.Violet
 */
@Configuration
@Import({
        SimpleOkHttpClientConfiguration.class
})
public class SlateConfiguration {

    /**
     * 通用上下文监听器
     */
    @Bean("slate.springboot.slateServletContextListener")
    public ServletContextListener slateServletContextListener() {
        return new SlateServletContextListener();
    }

}
