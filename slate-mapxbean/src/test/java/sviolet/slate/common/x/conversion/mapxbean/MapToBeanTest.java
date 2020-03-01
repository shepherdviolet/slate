/*
 * Copyright (C) 2015-2020 S.Violet
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

package sviolet.slate.common.x.conversion.mapxbean;

import ch.qos.logback.classic.Level;
import org.junit.Assert;
import org.junit.Test;
import sviolet.slate.common.helper.logback.LogbackHelper;
import sviolet.slate.common.util.common.LambdaBuilder;
import sviolet.slate.common.x.conversion.mapxbean.strategy.InflateCollectionElements;
import sviolet.slate.common.x.conversion.mapxbean.strategy.InflateUntilIndivisible;

import java.io.IOException;
import java.util.*;

public class MapToBeanTest {

    @Test
    public void test() {

        //Map -> Bean////////////////////////////////////////////////////////////////////////////////////////////

        LogbackHelper.setLevel("com.github.shepherdviolet.glaciion", Level.INFO);

        Map<String, Object> map = new HashMap<>();
        map.put("person", LambdaBuilder.hashMap(m -> {
            m.put("name", "single man");
            m.put("date", "2020-02-21 08:45:21");
        }));
        map.put("personMap", LambdaBuilder.hashMap(m -> {
            m.put("Mr. Wang", LambdaBuilder.hashMap(mm -> {
                mm.put("name", "Wang da shan");
                mm.put("date", "2020-02-21 08:45:22.002");
            }));
            m.put("Mr. Chen", LambdaBuilder.hashMap(mm -> {
                mm.put("name", "Chen mai zi");
                mm.put("date", "2020-02-21 08:45:23,003");
            }));
        }));
        map.put("personList", LambdaBuilder.arrayList(l -> {
            l.add(LambdaBuilder.hashMap(mm -> {
                mm.put("name", "Rock 0");
                mm.put("date", null);
            }));
            l.add(LambdaBuilder.hashMap(mm -> {
                mm.put("name", "Rock 1");
                mm.put("date", "");
            }));
        }));
        map.put("objectSet", LambdaBuilder.arrayList(l -> {
            l.add(LambdaBuilder.hashMap(mm -> {
                mm.put("name", "MAX 0");
                mm.put("date", "2020-02-21");
            }));
            l.add(LambdaBuilder.hashMap(mm -> {
                mm.put("name", "MAX 1");
                mm.put("date", "20200221");
            }));
        }));
        map.put("onlyReader", LambdaBuilder.hashMap(m -> {
            m.put("name", "Im reader");
            m.put("date", "2020-02-21 08:45:21");
        }));
        map.put("onlyWriter", LambdaBuilder.hashMap(m -> {
            m.put("name", "Im writer");
            m.put("date", "2020-02-21 08:45:21");
        }));

//        System.out.println(map);

        Bean bean = MapXBean.mapToBean()
                .throwExceptionIfFails(true)
//                .throwExceptionIfFails(false)
//                .exceptionCollector(new SimpleConversionExceptionCollector())
                .build()
                .convert(map, Bean.class);

        Assert.assertEquals("Bean{person=Person{name='single man', date=Fri Feb 21 08:45:21 CST 2020}, personMap={Mr. Wang=Person{name='Wang da shan', date=Fri Feb 21 08:45:22 CST 2020}, Mr. Chen=Person{name='Chen mai zi', date=Fri Feb 21 08:45:23 CST 2020}}, personList=[Person{name='Rock 0', date=null}, Person{name='Rock 1', date=null}], objectSet=[{date=2020-02-21, name=MAX 0}, {date=20200221, name=MAX 1}], onlyReader=null, onlyWriter=Person{name='Im writer', date=Fri Feb 21 08:45:21 CST 2020}}",
                String.valueOf(bean));

//        System.out.println(bean);

        //Bean -> Map////////////////////////////////////////////////////////////////////////////////////////////

        Map<String, Object> resultMap = MapXBean.beanToMap()
                .throwExceptionIfFails(true)
//                .throwExceptionIfFails(false)
//                .exceptionCollector(new SimpleConversionExceptionCollector())
                .build()
                .convert(bean);

        Assert.assertEquals("{personList=[Person{name='Rock 0', date=null}, Person{name='Rock 1', date=null}], objectSet=[{date=2020-02-21, name=MAX 0}, {date=20200221, name=MAX 1}], onlyReader=null, person=Person{name='single man', date=Fri Feb 21 08:45:21 CST 2020}, personMap={Mr. Wang=Person{name='Wang da shan', date=Fri Feb 21 08:45:22 CST 2020}, Mr. Chen=Person{name='Chen mai zi', date=Fri Feb 21 08:45:23 CST 2020}}}",
                String.valueOf(resultMap));

//        System.out.println(resultMap);

        resultMap = MapXBean.beanToMap()
                .throwExceptionIfFails(true)
//                .throwExceptionIfFails(false)
//                .exceptionCollector(new SimpleConversionExceptionCollector())
                .inflateStrategy(new InflateUntilIndivisible())
                .build()
                .convert(bean);

        Assert.assertEquals("{personList=[{date=null, name=Rock 0}, {date=null, name=Rock 1}], objectSet=[{date=2020-02-21, name=MAX 0}, {date=20200221, name=MAX 1}], onlyReader=null, person={date=Fri Feb 21 08:45:21 CST 2020, name=single man}, personMap={Mr. Wang={date=Fri Feb 21 08:45:22 CST 2020, name=Wang da shan}, Mr. Chen={date=Fri Feb 21 08:45:23 CST 2020, name=Chen mai zi}}}",
                String.valueOf(resultMap));

//        System.out.println(resultMap);

        resultMap = MapXBean.beanToMap()
                .throwExceptionIfFails(true)
//                .throwExceptionIfFails(false)
//                .exceptionCollector(new SimpleConversionExceptionCollector())
                .inflateStrategy(new InflateUntilIndivisible(LambdaBuilder.object(() -> {
                    Properties properties = new Properties();
                    try {
                        properties.load(getClass().getResourceAsStream("/x/conversion/mapxbean/keep-types.properties"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return properties;
                })))
                .build()
                .convert(bean);

        Assert.assertEquals("{personList=[Person{name='Rock 0', date=null}, Person{name='Rock 1', date=null}], objectSet=[{date=2020-02-21, name=MAX 0}, {date=20200221, name=MAX 1}], onlyReader=null, person=Person{name='single man', date=Fri Feb 21 08:45:21 CST 2020}, personMap={Mr. Wang=Person{name='Wang da shan', date=Fri Feb 21 08:45:22 CST 2020}, Mr. Chen=Person{name='Chen mai zi', date=Fri Feb 21 08:45:23 CST 2020}}}",
                String.valueOf(resultMap));

//        System.out.println(resultMap);

        resultMap = MapXBean.beanToMap()
                .throwExceptionIfFails(true)
//                .throwExceptionIfFails(false)
//                .exceptionCollector(new SimpleConversionExceptionCollector())
                .inflateStrategy(new InflateUntilIndivisible(0))
                .build()
                .convert(bean);

        Assert.assertEquals("{personList=[Person{name='Rock 0', date=null}, Person{name='Rock 1', date=null}], objectSet=[{date=2020-02-21, name=MAX 0}, {date=20200221, name=MAX 1}], onlyReader=null, person=Person{name='single man', date=Fri Feb 21 08:45:21 CST 2020}, personMap={Mr. Wang=Person{name='Wang da shan', date=Fri Feb 21 08:45:22 CST 2020}, Mr. Chen=Person{name='Chen mai zi', date=Fri Feb 21 08:45:23 CST 2020}}}",
                String.valueOf(resultMap));

//        System.out.println(resultMap);

        resultMap = MapXBean.beanToMap()
                .throwExceptionIfFails(true)
//                .throwExceptionIfFails(false)
//                .exceptionCollector(new SimpleConversionExceptionCollector())
                .inflateStrategy(new InflateUntilIndivisible(1))
                .build()
                .convert(bean);

        Assert.assertEquals("{personList=[Person{name='Rock 0', date=null}, Person{name='Rock 1', date=null}], objectSet=[{date=2020-02-21, name=MAX 0}, {date=20200221, name=MAX 1}], onlyReader=null, person={date=Fri Feb 21 08:45:21 CST 2020, name=single man}, personMap={Mr. Wang=Person{name='Wang da shan', date=Fri Feb 21 08:45:22 CST 2020}, Mr. Chen=Person{name='Chen mai zi', date=Fri Feb 21 08:45:23 CST 2020}}}",
                String.valueOf(resultMap));

//        System.out.println(resultMap);

        resultMap = MapXBean.beanToMap()
                .throwExceptionIfFails(true)
//                .throwExceptionIfFails(false)
//                .exceptionCollector(new SimpleConversionExceptionCollector())
                .inflateStrategy(new InflateCollectionElements())
                .build()
                .convert(bean);

        Assert.assertEquals("{personList=[{date=null, name=Rock 0}, {date=null, name=Rock 1}], objectSet=[{date=2020-02-21, name=MAX 0}, {date=20200221, name=MAX 1}], onlyReader=null, person=Person{name='single man', date=Fri Feb 21 08:45:21 CST 2020}, personMap={Mr. Wang={date=Fri Feb 21 08:45:22 CST 2020, name=Wang da shan}, Mr. Chen={date=Fri Feb 21 08:45:23 CST 2020, name=Chen mai zi}}}",
                String.valueOf(resultMap));

//        System.out.println(resultMap);

    }

    public static class Bean {

        private Person person;
        private Map<String, Person> personMap;
        private List<Person> personList;
        private Set<Object> objectSet;
        private Person onlyReader;
        private Person onlyWriter;

        public Person getPerson() {
            return person;
        }

        public void setPerson(Person person) {
            this.person = person;
        }

        public Map<String, Person> getPersonMap() {
            return personMap;
        }

        public void setPersonMap(Map<String, Person> personMap) {
            this.personMap = personMap;
        }

        public List<Person> getPersonList() {
            return personList;
        }

        public void setPersonList(List<Person> personList) {
            this.personList = personList;
        }

        public Set<Object> getObjectSet() {
            return objectSet;
        }

        public void setObjectSet(Set<Object> objectSet) {
            this.objectSet = objectSet;
        }

        public Person getOnlyReader() {
            return onlyReader;
        }

        public void setOnlyWriter(Person onlyWriter) {
            this.onlyWriter = onlyWriter;
        }

        @Override
        public String toString() {
            return "Bean{" +
                    "person=" + person +
                    ", personMap=" + personMap +
                    ", personList=" + personList +
                    ", objectSet=" + objectSet +
                    ", onlyReader=" + onlyReader +
                    ", onlyWriter=" + onlyWriter +
                    '}';
        }
    }

    public static class Person {

        private String name;
        private Date date;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        @Override
        public String toString() {
            return "Person{" +
                    "name='" + name + '\'' +
                    ", date=" + date +
                    '}';
        }
    }

}
