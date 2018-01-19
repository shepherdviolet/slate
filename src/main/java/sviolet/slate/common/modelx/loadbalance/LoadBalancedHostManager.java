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
 * Project GitHub: https://github.com/shepherdviolet/thistle
 * Email: shepherdviolet@163.com
 */

package sviolet.slate.common.modelx.loadbalance;

import sviolet.thistle.model.thread.LazySingleThreadPool;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 负载均衡--远端URL管理器
 * 
 * <pre>{@code
 *      //实例化
 *      LoadBalancedHostManager hostManager = new LoadBalancedHostManager();
 *      //设置/刷新远端清单(线程安全/异步更新)
 *      hostManager.setHostArray(new String[]{
 *          "http://www.baidu.com",
 *          "https://api.beeb.com.cn",
 *          "http://127.0.0.1:8080",
 *          "http://127.0.0.1:8081"
 *      });
 * }</pre>
 *
 * @author S.Violet
 */
public class LoadBalancedHostManager {

    private AtomicInteger mainCounter = new AtomicInteger(0);
    private AtomicInteger refugeCounter = new AtomicInteger(0);

    private AtomicReference<Host[]> hostArray = new AtomicReference<>(new Host[0]);
    private Map<String, Integer> hostIndexMap = new HashMap<>(0);

    private boolean returnNullIfAllBlocked = false;

    /**
     * [线程安全的]
     * @return 获取一个远端
     */
    public Host nextHost(){

        if (noSettings){
            return null;
        }

        /*
            因为设置Hosts是异步操作, 如果在初次设置完成前执行该方法, 会获取不到Host, 因此需要挂起线程等待设置完成(挂起60秒).
            这里不使用循环锁, 因为被意外唤醒的可能性极低, 而本类主要用于后端, 实际上在SPRING初始化的时候就会完成异步设置,
            一般不会发生问题. 即使被意外唤醒, 获取不到Host也只会导致个别交易失败.
         */
        if (!initialized) {
            try {
                initLock.lock();
                if (!initialized){
                    try {
                        initCondition.await(60, TimeUnit.SECONDS);
                    } catch (InterruptedException ignore) {
                    }
                }
            } finally {
                initLock.unlock();
            }
        }

        Host[] hostArray = this.hostArray.get();

        if (hostArray.length <= 0){
            return null;
        } else if (hostArray.length == 1){
            return hostArray[0];
        }

        long currentTimeMillis = System.currentTimeMillis();
        int mainCount = mainCounter.getAndIncrement() % hostArray.length;
        Host host = hostArray[mainCount];

        if (!host.isBlocked(currentTimeMillis)) {
            return host;
        }

        int refugeCount = refugeCounter.getAndIncrement() % hostArray.length;

        for (int i = 0 ; i < hostArray.length ; i++) {
            host = hostArray[refugeCount];
            if (!host.isBlocked(currentTimeMillis)) {
                return host;
            }
            refugeCount = (refugeCount + 1) % hostArray.length;
        }

        return returnNullIfAllBlocked ? null : hostArray[mainCount];

    }

    /*****************************************************************************************************************
     * settings
     */

    private LazySingleThreadPool settingThreadPool = new LazySingleThreadPool("LoadBalancedHostManager-Setting-%d");
    private AtomicReference<List<String>> newSettings = new AtomicReference<>(null);

    private volatile boolean noSettings = true;
    private volatile boolean initialized = false;
    private ReentrantLock initLock = new ReentrantLock();
    private Condition initCondition = initLock.newCondition();

    /**
     * [线程安全的/异步的]
     * 设置/刷新远端列表
     *
     * @param hosts 远端列表
     */
    public void setHostArray(String[] hosts) {
        if (hosts == null){
            setHostList(new ArrayList<String>(0));
        } else {
            setHostList(Arrays.asList(hosts));
        }
    }

    /**
     * [线程安全的/异步的]
     * 设置/刷新远端列表
     *
     * @param hosts 远端列表
     */
    public void setHostList(List<String> hosts){
        if (hosts == null){
            hosts = new ArrayList<>(0);
        }

        for (int i = 0 ; i < hosts.size() ; i++) {
            if (hosts.get(i) == null){
                hosts.remove(i);
                i--;
            }
        }

        newSettings.set(hosts);
        settingThreadPool.execute(settingInstallTask);

        /*
            置为已设置Host的状态
         */
        noSettings = false;
    }

    /**
     * 如果设置为false(默认), 当所有远端都被阻断时, nextHost方法返回一个后端.
     * 如果设置为true, 当所有远端都被阻断时, nextHost方法返回null.
     */
    public void setReturnNullIfAllBlocked(boolean returnNullIfAllBlocked) {
        this.returnNullIfAllBlocked = returnNullIfAllBlocked;
    }

    /**
     * 获得当前远端状态
     * @return value=true:可用, value=false:不可用
     */
    public Map<String, Boolean> getHostsStatus(){
        Host[] hostArray = this.hostArray.get();

        if (hostArray.length <= 0){
            return new HashMap<>(0);
        }

        long currentTimeMillis = System.currentTimeMillis();

        Map<String, Boolean> status = new HashMap<>(hostArray.length);
        for (Host host : hostArray){
            status.put(host.getUrl(), !host.isBlocked(currentTimeMillis));
        }
        return status;
    }

    Host[] getHostArray(){
        return this.hostArray.get();
    }

    private Runnable settingInstallTask = new Runnable() {
        @Override
        public void run() {
            List<String> newSettings;
            while ((newSettings = LoadBalancedHostManager.this.newSettings.getAndSet(null)) != null){

                Host[] hostArray = LoadBalancedHostManager.this.hostArray.get();

                int newSize = newSettings.size();
                Host[] newHostArray = new Host[newSize];
                Map<String, Integer> newHostIndexMap = new HashMap<>(newSize);

                for (int i = 0 ; i < newSize ; i++){

                    String newUrl = newSettings.get(i);
                    Integer oldIndex = hostIndexMap.get(newUrl);

                    if (oldIndex != null){
                        try {
                            newHostArray[i] = new Host(newUrl, hostArray[oldIndex].blockingTime);
                        } catch (Throwable ignore){
                            newHostArray[i] = new Host(newUrl, new AtomicLong(0));
                        }
                    } else {
                        newHostArray[i] = new Host(newUrl, new AtomicLong(0));
                    }

                    newHostIndexMap.put(newUrl, i);

                }

                LoadBalancedHostManager.this.hostArray.set(newHostArray);
                hostIndexMap = newHostIndexMap;

                /*
                    置为初始化完成状态, 并通知挂起的线程
                 */
                if (!initialized) {
                    initialized = true;
                    try {
                        initLock.lock();
                        if (!initialized){
                            initCondition.signalAll();
                        }
                    } finally {
                        initLock.unlock();
                    }
                }
            }
        }
    };

    public static class Host {

        private String url;
        private AtomicLong blockingTime;

        private Host(String url, AtomicLong blockingTime) {
            this.url = url;
            this.blockingTime = blockingTime;
        }

        /**
         * @return URL
         */
        public String getUrl() {
            return url;
        }

        /**
         * 阻断远端
         * @param duration 阻断的时间ms
         */
        public void block(long duration){
            blockingTime.set(System.currentTimeMillis() + duration);
        }

        /**
         * 该远端是否被阻断
         * @param currentTimeMillis 当前时间戳
         * @return true:被阻断(不可用), false:未阻断(可用)
         */
        private boolean isBlocked(long currentTimeMillis){
            return currentTimeMillis < blockingTime.get();
        }

        @Override
        public String toString() {
            return "Host<" + url + ">";
        }
    }

}
