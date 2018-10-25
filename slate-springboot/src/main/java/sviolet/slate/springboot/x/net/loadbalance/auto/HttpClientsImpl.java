package sviolet.slate.springboot.x.net.loadbalance.auto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import sviolet.slate.common.x.net.loadbalance.classic.SimpleOkHttpClient;
import sviolet.thistle.util.concurrent.ThreadPoolExecutorUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SimpleOkHttpClient集合
 *
 * @author S.Violet
 */
class HttpClientsImpl implements HttpClients, Closeable, InitializingBean, DisposableBean {

    public static final String OVERRIDE_PREFIX = "slate.httpclients.";

    private static final Logger logger = LoggerFactory.getLogger(HttpClientsImpl.class);

    private Map<String, SimpleOkHttpClient> clients = new HashMap<>(16);

    private OverrideSettings previousOverrideSettings = new MapBasedOverrideSettings(new HashMap<String, String>(0));
    private AtomicReference<OverrideSettings> newOverrideSettings = new AtomicReference<>(null);
    private ExecutorService overrideThreadPool = ThreadPoolExecutorUtils.createLazy(60, "Slate-HttpClients-override-%d");

    HttpClientsImpl(Map<String, HttpClientProperties> initProperties) {

        //init properties
        if (initProperties == null) {
            initProperties = new HashMap<>(0);
        }

        //create client
        for (Map.Entry<String, HttpClientProperties> entry : initProperties.entrySet()) {

            String tag = entry.getKey();

            HttpClientProperties properties = entry.getValue();
            if (properties == null) {
                logger.warn("HttpClients | " + tag + "> Has no properties, skip creation");
                continue;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("HttpClients | " + tag + "> Creating with properties: " + properties);
            }

            SimpleOkHttpClient client = HttpClientCreator.create(tag, properties);
            clients.put(tag, client);

            if (logger.isInfoEnabled()) {
                logger.info("HttpClients | " + tag + "> Created HttpClient: " + client);
            }

        }
    }

    @Override
    public void settingsOverride(OverrideSettings overrideSettings) {
        if (overrideSettings == null) {
            logger.warn("HttpClients SettingsOverride | overrideSettings is null, skip override");
            return;
        }
        newOverrideSettings.set(overrideSettings);
        overrideThreadPool.execute(overrideTask);
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

    private Runnable overrideTask = new Runnable() {
        @Override
        public void run() {
            OverrideSettings settings;
            while ((settings = newOverrideSettings.getAndSet(null)) != null) {
                Set<String> keys = settings.getKeys();
                if (keys == null || keys.isEmpty()) {
                    logger.warn("HttpClients SettingsOverride | overrideSettings.getKeys() return null or empty, skip override");
                    continue;
                }
                Set<SimpleOkHttpClient> changedClients = new HashSet<>();
                Map<String, String> relatedSettings = new HashMap<>();
                for (String key : keys) {
                    //Check if relevant
                    if (key == null || !key.startsWith(OVERRIDE_PREFIX) || key.length() <= OVERRIDE_PREFIX.length()) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("HttpClients SettingsOverride | Skip key '" + key + "', not start with " + OVERRIDE_PREFIX);
                        }
                        continue;
                    }

                    //Get value
                    String value = settings.getProperty(key);
                    String previousValue = previousOverrideSettings.getProperty(key);

                    //Record setting, we should take a copy of OverrideSettings, prevent data changes
                    relatedSettings.put(key, value);

                    //Get tag and property key
                    int tagEnd = key.indexOf('.', OVERRIDE_PREFIX.length());
                    if (tagEnd <= 0 || tagEnd == key.length() - 1) {
                        logger.error("HttpClients SettingsOverride | Illegal key '" + key + "', The correct format is '" + OVERRIDE_PREFIX + "tag.property=value', skip key");
                        continue;
                    }
                    String tag = key.substring(OVERRIDE_PREFIX.length(), tagEnd);
                    String property = key.substring(tagEnd + 1, key.length());

                    //Get client
                    SimpleOkHttpClient client = clients.get(tag);

                    //Check if changed
                    if (client == null) {
                        logger.error("HttpClients SettingsOverride | No HttpClient named " + tag + ", skip key '" + key + "'");
                        continue;
                    }
                    if (value == null) {
                        logger.warn("HttpClients SettingsOverride | The new value of '" + key + "' is null, " + property + " of " + tag + " stay the same");
                        continue;
                    }
                    if (value.equals(previousValue)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("HttpClients SettingsOverride | The new value of '" + key + "' is the same as the old value, " + property + " of " + tag + " stay the same");
                        }
                        continue;
                    }

                    //Change setting
                    HttpClientCreator.settingsOverride(client, tag, property, value);

                    //Record changed
                    changedClients.add(client);
                }

                //Print changed
                if (logger.isInfoEnabled()) {
                    for (SimpleOkHttpClient client : changedClients) {
                        logger.info("HttpClients SettingsOverride | " + client.getTag() +  "> Adjusted HttpClient: " + client);
                    }
                }

                //Set previous, we should take a copy of OverrideSettings, prevent data changes
                previousOverrideSettings = new MapBasedOverrideSettings(relatedSettings);
            }
        }
    };

}
