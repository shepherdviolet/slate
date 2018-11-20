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

package sviolet.slate.common.x.net.loadbalance.springboot.apollo;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfig;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import sviolet.slate.common.x.net.loadbalance.springboot.HttpClients;

/**
 * <p>HttpClients阿波罗动态配置: 支持在Apollo配置中心上动态调整客户端配置</p>
 * <p>配置前缀: slate.httpclients</p>
 *
 * @author S.Violet
 */
@Configuration
@ConditionalOnExpression("${slate.httpclient.enabled:false} && ${slate.httpclient.apollo-support:false}")
@ConditionalOnClass(com.ctrip.framework.apollo.Config.class)
public class HttpClientsApolloConfig {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientsApolloConfig.class);

    private HttpClients httpClients;

    //构造注入确保第一时间获得实例
    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public HttpClientsApolloConfig(HttpClients httpClients) {
        this.httpClients = httpClients;
        logger.info("HttpClients Apollo | Listening client configurations changes from apollo, namespace: application");
    }

    //获得Apollo配置实例, 注意配置正确的namespace
    @ApolloConfig
    private Config config;

    //监听Apollo配置变化
    @ApolloConfigChangeListener
    public void onApolloConfigChanged(ConfigChangeEvent configChangeEvent){
        //实时调整HttpClient配置
        httpClients.settingsOverride(new HttpClientsApolloOverrideSettings(config));
    }

}
