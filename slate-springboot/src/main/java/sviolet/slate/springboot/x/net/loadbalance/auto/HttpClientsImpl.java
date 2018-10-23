package sviolet.slate.springboot.x.net.loadbalance.auto;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import sviolet.slate.common.x.net.loadbalance.classic.SimpleOkHttpClient;
import sviolet.thistle.entity.common.Destroyable;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

class HttpClientsImpl implements HttpClients, Closeable, Destroyable, InitializingBean, DisposableBean {

    private Map<String, SimpleOkHttpClient>  clients;

    HttpClientsImpl(Map<String, SimpleOkHttpClient> clients) {
        if (clients == null) {
            throw new IllegalArgumentException("clients is null");
        }
        this.clients = clients;
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
        for (Map.Entry<String, SimpleOkHttpClient> entry : clients.entrySet()) {
            entry.getValue().close();
        }
    }

    @Override
    public void destroy() throws Exception {
        for (Map.Entry<String, SimpleOkHttpClient> entry : clients.entrySet()) {
            entry.getValue().destroy();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (Map.Entry<String, SimpleOkHttpClient> entry : clients.entrySet()) {
            entry.getValue().afterPropertiesSet();
        }
    }

    @Override
    public void onDestroy() {
        for (Map.Entry<String, SimpleOkHttpClient> entry : clients.entrySet()) {
            entry.getValue().onDestroy();
        }
    }

}
