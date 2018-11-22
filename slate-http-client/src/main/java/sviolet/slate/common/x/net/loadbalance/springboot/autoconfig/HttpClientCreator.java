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
import sviolet.slate.common.x.net.loadbalance.classic.GsonDataConverter;
import sviolet.slate.common.x.net.loadbalance.classic.SimpleOkHttpClient;
import sviolet.thistle.util.conversion.SimpleKeyValueEncoder;
import sviolet.thistle.util.judge.CheckUtils;

import java.util.Map;

/**
 * HttpClientCreator
 *
 * @author S.Violet
 */
class HttpClientCreator {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientCreator.class);

    /**
     * 根据tag和配置创建一个HttpClient
     * @param tag tag
     * @param settings 配置
     * @return SimpleOkHttpClient
     */
    static SimpleOkHttpClient create(String tag, HttpClientSettings settings) {
        //tag
        SimpleOkHttpClient client = (SimpleOkHttpClient) new SimpleOkHttpClient().setTag(tag);

        //hosts
        if (!CheckUtils.isEmptyOrBlank(settings.getHosts())) {
            client.setHosts(settings.getHosts());
        } else {
            client.setHostArray(settings.getHostList());
        }

        Map<String, String> headers = null;
        if (!CheckUtils.isEmptyOrBlank(settings.getHeaders())) {
            try {
                headers = SimpleKeyValueEncoder.decode(settings.getHeaders());
            } catch (SimpleKeyValueEncoder.DecodeException e) {
                throw new RuntimeException("HttpClients | Error while parsing headers '" + settings.getHeaders() + "' to Map, illegal key-value format, see github.com/shepherdviolet/thistle/blob/master/docs/kvencoder/guide.md", e);
            }
        }

        //properties
        return (SimpleOkHttpClient) client
                .setInitiativeInspectInterval(settings.getInitiativeInspectInterval())
                .setReturnNullIfAllBlocked(settings.isReturnNullIfAllBlocked())
                .setHttpGetInspector(settings.getHttpGetInspectorUrlSuffix())
                .setInspectorVerboseLog(settings.isInspectorVerboseLog())
                .setPassiveBlockDuration(settings.getPassiveBlockDuration())
                .setMediaType(settings.getMediaType())
                .setEncode(settings.getEncode())
                .setHeaders(headers)
                .setRecoveryCoefficient(settings.getRecoveryCoefficient())
                .setMaxIdleConnections(settings.getMaxIdleConnections())
                .setMaxThreads(settings.getMaxThreads())
                .setMaxThreadsPerHost(settings.getMaxThreadsPerHost())
                .setConnectTimeout(settings.getConnectTimeout())
                .setWriteTimeout(settings.getWriteTimeout())
                .setReadTimeout(settings.getReadTimeout())
                .setMaxReadLength(settings.getMaxReadLength())
                .setHttpCodeNeedBlock(settings.getHttpCodeNeedBlock())
                .setVerboseLog(settings.isVerboseLog())
                .setTxTimerEnabled(settings.isTxTimerEnabled())
                .setDataConverter(new GsonDataConverter());
    }

    /**
     * 覆盖客户端的配置
     * @param client 客户端
     * @param tag 客户端tag
     * @param property 客户端参数名
     * @param value 新参数值
     */
    static void settingsOverride(SimpleOkHttpClient client, String tag, String property, String value){
        if (logger.isInfoEnabled()) {
            logger.info("HttpClients SettingsOverride | Trying to change " + property + " of " + tag + " to '" + value + "'");
        }
        try {
            switch (property) {
                case "hosts":
                    client.setHosts(value);
                    break;
                case "httpGetInspectorUrlSuffix":
                    client.setHttpGetInspector(value);
                    break;
                case "mediaType":
                    client.setMediaType(value);
                    break;
                case "encode":
                    client.setEncode(value);
                    break;
                case "httpCodeNeedBlock":
                    client.setEncode(value);
                    break;
                case "initiativeInspectInterval":
                    client.setInitiativeInspectInterval(Long.parseLong(value));
                    break;
                case "returnNullIfAllBlocked":
                    client.setReturnNullIfAllBlocked(Boolean.parseBoolean(value));
                    break;
                case "inspectorVerboseLog":
                    client.setInspectorVerboseLog(Boolean.parseBoolean(value));
                    break;
                case "passiveBlockDuration":
                    client.setPassiveBlockDuration(Long.parseLong(value));
                    break;
                case "headers":
                    client.setHeaders(SimpleKeyValueEncoder.decode(value));
                    break;
                case "recoveryCoefficient":
                    client.setRecoveryCoefficient(Integer.parseInt(value));
                    break;
                case "maxIdleConnections":
                    client.setMaxIdleConnections(Integer.parseInt(value));
                    break;
                case "maxThreads":
                    client.setMaxThreads(Integer.parseInt(value));
                    break;
                case "maxThreadsPerHost":
                    client.setMaxThreadsPerHost(Integer.parseInt(value));
                    break;
                case "connectTimeout":
                    client.setConnectTimeout(Long.parseLong(value));
                    break;
                case "writeTimeout":
                    client.setWriteTimeout(Long.parseLong(value));
                    break;
                case "readTimeout":
                    client.setReadTimeout(Long.parseLong(value));
                    break;
                case "maxReadLength":
                    client.setMaxReadLength(Long.parseLong(value));
                    break;
                case "verboseLog":
                    client.setVerboseLog(Boolean.parseBoolean(value));
                    break;
                case "txTimerEnabled":
                    client.setTxTimerEnabled(Boolean.parseBoolean(value));
                    break;
                default:
                    logger.error("HttpClients SettingsOverride | Error while changing " + property + " of " + tag + " to '" + value + "', No overridable setting named '" + property + "'");
                    break;
            }
        } catch (NumberFormatException e) {
            logger.error("HttpClients SettingsOverride | Error while changing " + property + " of " + tag + " to '" + value + "', number format failed", e);
        } catch (SimpleKeyValueEncoder.DecodeException e) {
            logger.error("HttpClients SettingsOverride | Error while changing " + property + " of " + tag + " to '" + value + "', illegal key-value format, see github.com/shepherdviolet/thistle/blob/master/docs/kvencoder/guide.md", e);
        } catch (Exception e) {
            logger.error("HttpClients SettingsOverride | Error while changing " + property + " of " + tag + " to '" + value + "'", e);
        }
    }

}
