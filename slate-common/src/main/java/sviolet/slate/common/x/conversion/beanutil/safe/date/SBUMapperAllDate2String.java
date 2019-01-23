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

package sviolet.slate.common.x.conversion.beanutil.safe.date;

import com.github.shepherdviolet.glaciion.api.annotation.ImplementationPriority;
import com.github.shepherdviolet.glaciion.api.annotation.PropertyInject;
import org.slf4j.Logger;
import sviolet.slate.common.x.conversion.beanutil.PropMapper;
import sviolet.thistle.util.judge.CheckUtils;

import java.text.SimpleDateFormat;

@ImplementationPriority(-1)
public class SBUMapperAllDate2String implements PropMapper {

    private static final Class[] FROM = new Class[]{
            java.util.Date.class,
            java.sql.Date.class,
            java.sql.Timestamp.class
    };

    private static final Class[] TO = new Class[]{
            String.class,
    };

    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    private ThreadLocal<SimpleDateFormat> dateFormats = new ThreadLocal<>();
    private String dateFormat = DEFAULT_DATE_FORMAT;

    @PropertyInject
    public void setDateFormat(String dateFormat) {
        //注意这个值可能为空
        if (CheckUtils.isEmptyOrBlank(dateFormat)) {
            dateFormat = DEFAULT_DATE_FORMAT;
        }
        this.dateFormat = dateFormat;
        //pre check format
        dateFormats.set(new SimpleDateFormat(dateFormat));
    }

    @Override
    public Object map(Object from, Class<?> toType, Logger logger, boolean logEnabled) {
        SimpleDateFormat format = dateFormats.get();
        if (format == null) {
            format = new SimpleDateFormat(dateFormat);
            dateFormats.set(format);
        }
        return format.format((java.util.Date)from);
    }

    @Override
    public Class<?>[] fromType() {
        return FROM;
    }

    @Override
    public Class<?>[] toType() {
        return TO;
    }

}
