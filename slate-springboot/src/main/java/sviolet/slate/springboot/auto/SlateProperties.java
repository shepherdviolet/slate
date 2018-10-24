package sviolet.slate.springboot.auto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import sviolet.slate.springboot.x.net.loadbalance.auto.HttpClientProperties;

import java.util.Map;

/**
 * <p>Slate配置</p>
 * <p>配置前缀: slate</p>
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
