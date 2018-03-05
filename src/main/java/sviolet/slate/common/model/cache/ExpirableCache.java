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

package sviolet.slate.common.model.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sviolet.thistle.util.concurrent.ConcurrentUtils;
import sviolet.thistle.util.conversion.DateTimeUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public abstract class ExpirableCache <T> {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Map<String, ElementWrapper<T>> map = new ConcurrentHashMap<>();
    private ReentrantLock elementWrapperLock = new ReentrantLock();

    private long doUpdateTimeout = 5000L;
    private long doCheckInterval = 500L;

    public T get(String key){
        if (key == null){
            throw new RuntimeException("key is null");
        }

        long currentTime = System.currentTimeMillis();

        ElementWrapper<T> elementWrapper = getWrapper(key);

        if (currentTime > elementWrapper.expireTime.get()) {
            return update(key, elementWrapper, currentTime);
        }

        if (logger.isDebugEnabled()){
            logger.debug("ExpirableCache get \"" + key + "\", value:" + String.valueOf(elementWrapper.element));
        }
        //返回token
        return elementWrapper.element;
    }

    private ElementWrapper<T> getWrapper(String key){
        ElementWrapper<T> elementWrapper = map.get(key);
        if (elementWrapper == null) {
            try {
                elementWrapperLock.lock();
                elementWrapper = map.get(key);
                if (elementWrapper == null) {
                    elementWrapper = new ElementWrapper<>();
                    elementWrapper.key = key;
                    map.put(key, elementWrapper);
                }
            } finally {
                elementWrapperLock.unlock();
            }
        }
        return elementWrapper;
    }

    public T forceUpdate(String key){
        if (key == null){
            throw new RuntimeException("key is null");
        }

        if (logger.isInfoEnabled()){
            logger.info("ExpirableCache force update \"" + key + "\"");
        }

        long currentTime = System.currentTimeMillis();

        ElementWrapper<T> elementWrapper = getWrapper(key);

        return update(key, elementWrapper, currentTime);
    }

    public void forceUpdateAll(){
        Map<String, ElementWrapper<T>> snap = ConcurrentUtils.getSnapShot(map);
        for (ElementWrapper<T> wrapper : snap.values()){
            forceUpdate(wrapper.key);
        }
    }

    private T update(String key, ElementWrapper<T> elementWrapper, long currentTime){
        int updateCount = elementWrapper.updateCounter.get();
        int concurrentCount = elementWrapper.updateConcurrentCounter.incrementAndGet();
        if (concurrentCount > 1){
            elementWrapper.updateConcurrentCounter.decrementAndGet();

            if (logger.isInfoEnabled()){
                logger.info("ExpirableCache waiting \"" + key + "\"");
            }

            while (updateCount == elementWrapper.updateCounter.get() &&
                    System.currentTimeMillis() < currentTime + doUpdateTimeout) {
                synchronized (elementWrapper) {
                    try {
                        elementWrapper.wait(doCheckInterval);
                    } catch (InterruptedException e) {
                        if (logger.isErrorEnabled()){
                            logger.error("ExpirableCache waiting \"" + key + "\" interrupted", e);
                        }
                        return null;
                    }
                }
            }
            if (updateCount != elementWrapper.updateCounter.get()) {
                if (logger.isDebugEnabled()){
                    logger.debug("ExpirableCache wait \"" + key + "\" succeed, value:" + String.valueOf(elementWrapper.element));
                } else if (logger.isInfoEnabled()){
                    logger.info("ExpirableCache wait \"" + key + "\" succeed");
                }

                return elementWrapper.element;
            }

            if (logger.isInfoEnabled()){
                logger.info("ExpirableCache wait \"" + key + "\" failed");
            }
            return null;
        }

        try {
            UpdateResult<T> result = onUpdate(key);
            elementWrapper.element = result.element;
            elementWrapper.expireTime.set(currentTime + result.expireThreshold);

            if (logger.isDebugEnabled()){
                logger.debug("ExpirableCache update \"" + key + "\" succeed, expire time " + DateTimeUtils.getDateTime(elementWrapper.expireTime.get()) + ", value:" + String.valueOf(elementWrapper.element));
            } else if (logger.isInfoEnabled()){
                logger.info("ExpirableCache update \"" + key + "\" succeed, expire time " + DateTimeUtils.getDateTime(elementWrapper.expireTime.get()));
            }
        } catch (Throwable t) {
            long interval = onError(key, t);
            if (interval < 0) {
                interval = 0;
            }
            elementWrapper.element = null;
            elementWrapper.expireTime.set(currentTime + interval);

            if (logger.isDebugEnabled()){
                logger.debug("ExpirableCache update \"" + key + "\" failed, update interval " + interval, t);
            } else if (logger.isInfoEnabled()){
                logger.info("ExpirableCache update \"" + key + "\" failed, update interval " + interval);
            }
        } finally {
            elementWrapper.updateConcurrentCounter.decrementAndGet();
            elementWrapper.updateCounter.incrementAndGet();
            synchronized (elementWrapper){
                elementWrapper.notifyAll();
            }
        }

        return elementWrapper.element;
    }

    protected abstract UpdateResult<T> onUpdate(String key);

    protected abstract long onError(String key, Throwable t);

    private static class ElementWrapper <T> {

        private String key;
        private volatile T element;
        private AtomicLong expireTime = new AtomicLong(0L);
        private AtomicInteger updateCounter = new AtomicInteger(0);
        private AtomicInteger updateConcurrentCounter = new AtomicInteger(0);

    }

    public static class UpdateResult <T> {

        private T element;
        private long expireThreshold;

        public UpdateResult(T element, long expireThreshold) {
            this.element = element;
            this.expireThreshold = expireThreshold;
        }
    }

}
