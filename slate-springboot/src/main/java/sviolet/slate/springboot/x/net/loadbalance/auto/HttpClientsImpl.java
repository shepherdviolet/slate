package sviolet.slate.springboot.x.net.loadbalance.auto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import sviolet.slate.common.x.net.loadbalance.classic.SimpleOkHttpClient;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * SimpleOkHttpClient集合
 *
 * @author S.Violet
 */
class HttpClientsImpl implements HttpClients, Closeable, InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientsImpl.class);

    private volatile Map<String, SimpleOkHttpClient> clients = new HashMap<>(16);
    private volatile Map<String, HttpClientProperties> propertiesMap;

    HttpClientsImpl(Map<String, HttpClientProperties> propertiesMap) {
        //properties
        if (propertiesMap == null) {
            propertiesMap = new HashMap<>(0);
        }
        this.propertiesMap = propertiesMap;

        //create clients
        for (Map.Entry<String, HttpClientProperties> entry : propertiesMap.entrySet()) {

            String tag = entry.getKey();

            logger.info("Slate HttpClients | -------------------------------------------------------------");
            logger.info("Slate HttpClients | Creating " + tag);

            HttpClientProperties properties = entry.getValue();
            if (properties == null) {
                logger.warn("Slate HttpClients | " + tag + " has no properties, skip");
                continue;
            }

            SimpleOkHttpClient client = HttpClientCreator.create(tag, properties);
            clients.put(tag, client);

            if (logger.isInfoEnabled()) {
                logger.info("Slate HttpClients | Created " + client);
            }

        }
    }

    @Override
    public void update(String config) {

    }

    @Override
    public SimpleOkHttpClient get(String key) {
        return clients.get(key);
    }

    @Override
    public int size() {
        return clients.size();
    }

    @Override
    public Set<String> tags() {
        return clients.keySet();
    }

    @Override
    public void close() throws IOException {
        Map<String, SimpleOkHttpClient>  clients = this.clients;
        for (Map.Entry<String, SimpleOkHttpClient> entry : clients.entrySet()) {
            entry.getValue().close();
        }
    }

    @Override
    public void destroy() throws Exception {
        Map<String, SimpleOkHttpClient>  clients = this.clients;
        for (Map.Entry<String, SimpleOkHttpClient> entry : clients.entrySet()) {
            entry.getValue().destroy();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, SimpleOkHttpClient>  clients = this.clients;
        for (Map.Entry<String, SimpleOkHttpClient> entry : clients.entrySet()) {
            entry.getValue().afterPropertiesSet();
        }
    }

}
