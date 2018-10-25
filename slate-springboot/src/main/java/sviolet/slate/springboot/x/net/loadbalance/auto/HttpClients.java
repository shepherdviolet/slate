package sviolet.slate.springboot.x.net.loadbalance.auto;

import sviolet.slate.common.x.net.loadbalance.classic.SimpleOkHttpClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>HttpClients配置: 自动配置SimpleOkHttpClient</p>
 * <p>配置前缀: slate.httpclients</p>
 */
public interface HttpClients {

    /**
     * 获取Http请求客户端
     * @param tag tag(标识)
     * @return SimpleOkHttpClient
     */
    SimpleOkHttpClient get(String tag);

    /**
     * @return 返回集合中的客户端数量
     */
    int size();

    /**
     * @return 返回集合中所有客户端的tag
     */
    Set<String> tags();

    /**
     * 使用外部配置覆盖客户端的配置
     * @param overrideSettings 外部配置
     */
    void settingsOverride(OverrideSettings overrideSettings);

    /**
     * 外部配置
     */
    interface OverrideSettings {

        Set<String> getKeys();

        String getProperty(String key);

    }

    /**
     * 基于Map实现的外部配置
     */
    class MapBasedOverrideSettings implements OverrideSettings {

        private Map<String, String> map;

        public MapBasedOverrideSettings(Map<String, String> map) {
            if (map == null) {
                map = new HashMap<>(0);
            }
            this.map = map;
        }

        @Override
        public Set<String> getKeys() {
            return map.keySet();
        }

        @Override
        public String getProperty(String key) {
            return map.get(key);
        }

    }

}
