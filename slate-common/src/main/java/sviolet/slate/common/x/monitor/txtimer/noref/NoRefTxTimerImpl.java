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

package sviolet.slate.common.x.monitor.txtimer.noref;

import sviolet.slate.common.x.monitor.txtimer.TxTimer;
import sviolet.slate.common.x.monitor.txtimer.TxTimerProvider;

/**
 * 与TxTimer关联点
 *
 * @author S.Violet
 */
class NoRefTxTimerImpl implements NoRefTxTimer {

    @Override
    public void start(String groupName, String transactionName) {
        TxTimer.start(groupName, transactionName);
    }

    @Override
    public void stop() {
        TxTimer.stop();
    }

    @Override
    public TxTimerProvider getProvider() {
        return TxTimer.getProvider();
    }

}
