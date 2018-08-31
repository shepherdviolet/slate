package sviolet.slate.springboot.auto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import sviolet.slate.springboot.modelx.loadbalance.auto.HttpClientProperties;

import java.util.Map;

/**
 * Slate自动配置参数
 *
 * @author S.Violet
 */
@ConfigurationProperties(prefix = "slate")
public class SlateProperties {

    /**
     * 自动配置SimpleOkHttpClient, 使用HttpClients示例获得
     */
    private Map<String, HttpClientProperties> httpclients;

    public Map<String, HttpClientProperties> getHttpclients() {
        return httpclients;
    }

    public void setHttpclients(Map<String, HttpClientProperties> httpclients) {
        this.httpclients = httpclients;
    }

}
