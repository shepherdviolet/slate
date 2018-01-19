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

package sviolet.slate.common.modelx.loadbalance.classic;

/**
 * Http请求拒绝异常
 *
 * @author S.Violet
 */
public class HttpRejectException extends Exception {

    private int code;
    private String message;

    public HttpRejectException(int code, String message) {
        super("Http rejected, code:" + code + ", message:" + message);
        this.code = code;
        this.message = message;
    }

    public int getResponseCode() {
        return code;
    }

    public String getResponseMessage(){
        return message;
    }

}
