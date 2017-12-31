/*
 * Copyright (C) 2015-2017 S.Violet
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
 * Project GitHub: https://github.com/shepherdviolet/slate-common
 * Email: shepherdviolet@163.com
 */

package sviolet.slate.common.modelx.loadbalance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sviolet.thistle.util.common.NetworkUtils;

import java.net.URI;

public class TelnetLoadBalanceInspector implements LoadBalanceInspector {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public boolean inspect(String url, long timeout) {
        try {
            URI uri = URI.create(url);
            return inspectByTelnet(uri.getHost(), uri.getPort(), timeout);
        } catch (Exception e) {
            if (logger.isErrorEnabled()){
                logger.error("Inspect: invalid url " + url, e);
            }
        }
        return false;
    }

    protected boolean inspectByTelnet(String hostname, int ip, long timeout){
        return NetworkUtils.telnet(hostname, ip, timeout > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) timeout);
    }

}
