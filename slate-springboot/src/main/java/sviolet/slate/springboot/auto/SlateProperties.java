package sviolet.slate.springboot.auto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import sviolet.slate.springboot.x.net.loadbalance.auto.HttpClientProperties;

import java.util.Map;

/**
 * Slate自动配置参数
 *
 * @author S.Violet
 */
@ConfigurationProperties(prefix = "slate")
public class SlateProperties {

    /**
     * slate.httpclients
     * 自动配置SimpleOkHttpClient(多个)
     */
    private Map<String, HttpClientProperties> httpclients;

    public Map<String, HttpClientProperties> getHttpclients() {
        return httpclients;
    }

    public void setHttpclients(Map<String, HttpClientProperties> httpclients) {
        this.httpclients = httpclients;
    }

}
