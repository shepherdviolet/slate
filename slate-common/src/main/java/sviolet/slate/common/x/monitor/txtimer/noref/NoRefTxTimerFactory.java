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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sviolet.slate.common.x.monitor.txtimer.TimerContext;
import sviolet.slate.common.x.monitor.txtimer.TxTimerProvider2;

/**
 * <p>对TxTimer类无引用的NoRefTxTimer代理工厂</p>
 *
 * <p>本工厂类和NoRefTxTimer类对TxTimer无直接类引用, 用于类库和框架层, 可以由用户选择是否开启TxTimer, 若不开启, 则不会初始化
 * TxTimer类, 减少无用的对象创建.</p>
 *
 * @author S.Violet
 */
public class NoRefTxTimerFactory {

    private static final Logger logger = LoggerFactory.getLogger(NoRefTxTimerFactory.class);

    public static NoRefTxTimer newInstance() {
        try {
            return (NoRefTxTimer) Class.forName("sviolet.slate.common.x.monitor.txtimer.noref.NoRefTxTimerImpl").newInstance();
        } catch (Exception e) {
            logger.error("TxTimer | NoRefTxTimerFactory create NoRefTxTimer instance failed, TxTimer disabled !!!", e);
            return new DummyNoRefTxTimer();
        }
    }

    private static class DummyNoRefTxTimer implements NoRefTxTimer {
        @Override
        public TimerContext entry(String groupName, String transactionName) {
            return DUMMY_CONTEXT;
        }
        @Override
        public void exit(TimerContext timerContext) {
        }
        @Override
        public void exit(TimerContext timerContext, int resultCode) {
        }
        @Override
        public TxTimerProvider2 getProvider() {
            return null;
        }
    }

    private static final TimerContext DUMMY_CONTEXT = new TimerContext(){
        @Override
        public void exit(int resultCode) {
            //do nothing
        }
    };

}
