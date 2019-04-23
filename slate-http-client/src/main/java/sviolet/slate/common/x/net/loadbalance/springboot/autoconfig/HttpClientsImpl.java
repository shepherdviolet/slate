/*
 * Copyright (C) 2015-2018 S.Violet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project GitHub: https://github.com/shepherdviolet/slate
 * Email: shepherdviolet@163.com
 */

package sviolet.slate.common.x.net.loadbalance.springboot.autoconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import sviolet.slate.common.x.net.loadbalance.classic.DataConverter;
import sviolet.slate.common.x.net.loadbalance.classic.SimpleOkHttpClient;
import sviolet.slate.common.x.net.loadbalance.springboot.HttpClients;
import sviolet.thistle.util.concurrent.ThreadPoolExecutorUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * SimpleOkHttpClient集合
 *
 * @author S.Violet
 */
class HttpClientsImpl implements HttpClients, Closeable, InitializingBean, DisposableBean {

    public static final String OVERRIDE_PREFIX = "slate.httpclients.";

    private static final Logger logger = LoggerFactory.getLogger(HttpClientsImpl.class);

    private DataConverter dataConverter;
    private boolean noticeLogEnabled = true;

    private Map<String, SimpleOkHttpClient> clients = new ConcurrentHashMap<>(16);

    private Map<String, String> previousSettingMap = new HashMap<>(16);
    private LinkedBlockingQueue<OverrideSettings> overrideSettingQueue = new LinkedBlockingQueue<>();
    private ExecutorService overrideThreadPool = ThreadPoolExecutorUtils.createLazy(60, "Slate-HttpClients-override-%d");

    HttpClientsImpl(SlatePropertiesForHttpClient slatePropertiesForHttpClient, DataConverter dataConverter) {

        logger.info("HttpClients | Enabled");

        this.dataConverter = dataConverter;

        if (slatePropertiesForHttpClient.getHttpclient() != null) {
            noticeLogEnabled = slatePropertiesForHttpClient.getHttpclient().isNoticeLogEnabled();
        }

        //init properties
        if (slatePropertiesForHttpClient.getHttpclients() == null) {
            return;
        }

        //create client
        for (Map.Entry<String, HttpClientSettings> entry : slatePropertiesForHttpClient.getHttpclients().entrySet()) {

            String tag = entry.getKey();

            HttpClientSettings settings = entry.getValue();
            if (settings == null) {
                logger.warn("HttpClients | " + tag + "> Has no properties, skip creation");
                continue;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("HttpClients | " + tag + "> Creating with settings: " + settings);
            }

            SimpleOkHttpClient client = HttpClientCreator.create(tag, settings, dataConverter);
            clients.put(tag, client);

            if (logger.isInfoEnabled()) {
                logger.info("HttpClients | " + tag + "> Created HttpClient: " + client);
            }

        }
    }

    @Override
    public void settingsOverride(OverrideSettings overrideSettings) {
        if (overrideSettings == null) {
            if (logger.isDebugEnabled() && noticeLogEnabled) {
                logger.warn("HttpClients SettingsOverride | overrideSettings is null, skip override");
            }
            return;
        }
        overrideSettingQueue.offer(overrideSettings);
        overrideThreadPool.execute(overrideTask);
    }

    @Override
    public SimpleOkHttpClient get(String key) {
        SimpleOkHttpClient client = clients.get(key);
        if (client == null && logger.isInfoEnabled() && noticeLogEnabled) {
            logger.warn("HttpClients | No HttpClient named " + key + ", return null");
        }
        return client;
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
            while ((settings = overrideSettingQueue.poll()) != null) {
                Set<String> keys = settings.getKeys();
                if (keys == null || keys.isEmpty()) {
                    if (logger.isDebugEnabled() && noticeLogEnabled) {
                        logger.warn("HttpClients SettingsOverride | overrideSettings.getKeys() return null or empty, skip override");
                    }
                    continue;
                }
                Set<SimpleOkHttpClient> changedClients = new HashSet<>();
                for (String key : keys) {
                    //Check if relevant
                    if (key == null || !key.startsWith(OVERRIDE_PREFIX) || key.length() <= OVERRIDE_PREFIX.length()) {
                        if (logger.isTraceEnabled() && noticeLogEnabled) {
                            logger.trace("HttpClients SettingsOverride | Skip key '" + key + "', not start with " + OVERRIDE_PREFIX);
                        }
                        continue;
                    }

                    //Get value
                    String value = settings.getValue(key);
                    String previousValue = previousSettingMap.get(key);

                    //Record setting
                    previousSettingMap.put(key, value);

                    //Get tag and property key
                    int tagEnd = key.indexOf('.', OVERRIDE_PREFIX.length());
                    if (tagEnd <= 0 || tagEnd == key.length() - 1) {
                        logger.error("HttpClients SettingsOverride | Illegal key '" + key + "', The correct format is '" + OVERRIDE_PREFIX + "tag.property=value', skip key");
                        continue;
                    }
                    String tag = key.substring(OVERRIDE_PREFIX.length(), tagEnd);
                    String property = key.substring(tagEnd + 1, key.length());

                    //Check if changed
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

                    //Get client
                    SimpleOkHttpClient client = clients.get(tag);

                    //Check if new
                    if (client == null) {
                        try {
                            client = HttpClientCreator.create(tag, new HttpClientSettings(), dataConverter);
                            client.start();
                            clients.put(tag, client);
                            logger.info("HttpClients SettingsOverride | " + tag + "> Create new HttpClient with default properties, because no HttpClient named " + tag + " before");
                        } catch (Exception e) {
                            logger.warn("HttpClients SettingsOverride | Error while creating new HttpClient named " + tag + ", skip key " + key, e);
                            continue;
                        }
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

            }
        }
    };

}
