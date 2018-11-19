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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import sviolet.slate.common.x.net.loadbalance.springboot.HttpClients;

import java.util.Set;

/**
 * <p>HttpClients阿波罗动态配置: 支持在Apollo配置中心上动态调整客户端配置</p>
 * <p>配置前缀: slate.httpclients</p>
 *
 * @author S.Violet
 */
@Configuration
@ConditionalOnClass(com.ctrip.framework.apollo.Config.class)
public class HttpClientsApolloConfig {

    private HttpClients httpClients;

    //构造注入确保第一时间获得实例
    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public HttpClientsApolloConfig(HttpClients httpClients) {
        this.httpClients = httpClients;
    }

    //获得Apollo配置实例, 注意配置正确的namespace
    @ApolloConfig("application")
    private Config config;

    //监听Apollo配置变化
    @ApolloConfigChangeListener("application")
    private void onApolloConfigChanged(ConfigChangeEvent configChangeEvent){
        //实时调整HttpClient配置
        httpClients.settingsOverride(new ApolloOverrideSettings(config));
    }

    //将Apollo配置包装为OverrideSettings
    private static class ApolloOverrideSettings implements HttpClients.OverrideSettings {

        private Config config;

        private ApolloOverrideSettings(Config config) {
            //持有Apollo配置
            this.config = config;
        }

        @Override
        public Set<String> getKeys() {
            //获取所有配置key
            return config.getPropertyNames();
        }

        @Override
        public String getValue(String key) {
            //根据key返回配置value, 不存在返回null
            return config.getProperty(key, null);
        }

    }

}
