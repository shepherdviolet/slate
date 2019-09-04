/*
 * Copyright (C) 2015-2019 S.Violet
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

package sviolet.slate.common.helper.sentinel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>注解方式启用JSON格式的Sentinel规则设置器. 说明文档见: https://github.com/shepherdviolet/slate/blob/master/docs/ezsentinel/guide.md</p>
 *
 * 依赖: compile "com.google.code.gson:gson:$version_gson"
 *
 * @author S.Violet
 */
@Configuration
public class EzSentinelConfiguration {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Bean("slate.common.ezSentinelRuleConfigurer")
    public AbstractEzSentinelRuleConfigurer<String> ezSentinelRuleConfigurer(){
        return new JsonEzSentinelRuleConfigurer();
    }

    @Bean("slate.common.ezSentinelRuleConfigurerPropertySetter")
    public Object ezSentinelRuleConfigurerPropertySetter(AbstractEzSentinelRuleConfigurer<String> ezSentinelRuleConfigurer){
        return new Object() {

            /**
             * 监听本地配置和Apollo配置, 更新规则
             */
            @Value("${slate.common.ez-sentinel.rule-data:}")
            public void setRuleData(String ruleData) {
                ezSentinelRuleConfigurer.update(ruleData);
            }

            /**
             * Sentinel总开关
             */
            @Value("${spring.cloud.sentinel.enabled:true}")
            public void setEnabled(boolean enabled) {
                //这个只对核心有效, 即对本地统计限流熔断有效
                //但是, 这个参数和SpringBoot版的一样, 所以对核心和外围(连Dashboard等)都有效, 但是改了配置需要重启应用
                com.alibaba.csp.sentinel.Constants.ON = enabled;
                logger.info("EzSentinel | Sentinel " + (enabled ? "Enabled" : "Disabled"));
            }

        };
    }

}