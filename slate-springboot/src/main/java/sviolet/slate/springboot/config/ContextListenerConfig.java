package sviolet.slate.springboot.config;


import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sviolet.slate.common.util.common.SlateServletContextListener;

import javax.servlet.ServletContextListener;

/**
 * 通用上下文监听器
 *
 * @author S.Violet
 */
@Configuration
@ConditionalOnClass(javax.servlet.ServletContextListener.class)
public class ContextListenerConfig {

    @Bean("slate.springboot.slateServletContextListener")
    public ServletContextListener slateServletContextListener() {
        return new SlateServletContextListener();
    }

}
