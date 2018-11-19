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

import org.springframework.boot.context.properties.ConfigurationProperties;

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
    private Map<String, HttpClientsProperties> httpclients;

    public Map<String, HttpClientsProperties> getHttpclients() {
        return httpclients;
    }

    public void setHttpclients(Map<String, HttpClientsProperties> httpclients) {
        this.httpclients = httpclients;
    }

}
