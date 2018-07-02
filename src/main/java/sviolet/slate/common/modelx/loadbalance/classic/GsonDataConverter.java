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
 * Project GitHub: https://github.com/shepherdviolet/slate-common
 * Email: shepherdviolet@163.com
 */

package sviolet.slate.common.modelx.loadbalance.classic;

import com.google.gson.Gson;

/**
 * GSON数据转换器
 *
 * @author S.Violet
 */
public class GsonDataConverter implements DataConverter {

    private Gson gson = new Gson();

    private String encode = "UTF-8";

    @Override
    public byte[] convert(Object bean) throws Exception{
        if (bean == null) {
            return new byte[0];
        }
        return gson.toJson(bean).getBytes(encode);
    }

    @Override
    public <T> T convert(byte[] data, Class<T> type) throws Exception{
        if (data == null || data.length <= 0) {
            return null;
        }
        return gson.fromJson(new String(data, encode), type);
    }

    public String getEncode() {
        return encode;
    }

    public void setEncode(String encode) {
        this.encode = encode;
    }

}
