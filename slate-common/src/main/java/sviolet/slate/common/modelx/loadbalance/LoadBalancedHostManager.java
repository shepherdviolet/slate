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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sviolet.thistle.util.concurrent.ThreadPoolExecutorUtils;
import sviolet.thistle.util.judge.CheckUtils;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 负载均衡--远端URL管理器
 *
 * <pre>{@code
 *      //实例化
 *      LoadBalancedHostManager hostManager = new LoadBalancedHostManager()
 *              //设置/刷新远端清单(线程安全/异步更新)
 *              .setHostArray(new String[]{
 *                  "http://www.baidu.com",
 *                  "https://api.beeb.com.cn",
 *                  "http://127.0.0.1:8080",
 *                  "http://127.0.0.1:8081"
 *              });
 * }</pre>
 *
 * @author S.Violet
 */
public class LoadBalancedHostManager {

    private Logger logger = LoggerFactory.getLogger(getClass());
    private String tag = "";

    private AtomicInteger mainCounter = new AtomicInteger(0);
    private AtomicInteger refugeCounter = new AtomicInteger(0);

    private volatile Host[] hostArray = new Host[0];
    private Map<String, Integer> hostIndexMap = new HashMap<>(0);

    private boolean returnNullIfAllBlocked = false;

    /**
     * [线程安全的]
     * @return 获取一个远端
     */
    public Host nextHost(){

        Host[] hostArray = this.hostArray;

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

    private ExecutorService settingThreadPool = ThreadPoolExecutorUtils.createLazy(60L, "svs-lbhm-set-%d");
    private AtomicReference<List<String>> newSettings = new AtomicReference<>(null);

    private boolean initialized = false;
    private ReentrantLock initLock = new ReentrantLock();

    /**
     * [线程安全/异步生效/可运行时修改]
     * 设置/刷新远端列表, 该方法可以反复调用设置新的后端(但不是同步生效)
     *
     * @param hosts 远端列表, 格式:"http://127.0.0.1:8081/,http://127.0.0.1:8082/"
     */
    public LoadBalancedHostManager setHosts(String hosts){
        if (CheckUtils.isEmptyOrBlank(hosts)){
            setHostList(new ArrayList<String>(0));
            return this;
        }
        setHostArray(hosts.split(","));
        return this;
    }

    /**
     * [线程安全/异步生效/可运行时修改]
     * 设置/刷新远端列表, 该方法可以反复调用设置新的后端(但不是同步生效)
     *
     * @param hosts 远端列表
     */
    public LoadBalancedHostManager setHostArray(String[] hosts) {
        if (hosts == null || hosts.length <= 0){
            setHostList(new ArrayList<String>(0));
        } else {
            setHostList(Arrays.asList(hosts));
        }
        return this;
    }

    /**
     * [线程安全/异步生效/可运行时修改]
     * 设置/刷新远端列表, 该方法可以反复调用设置新的后端(但不是同步生效)
     *
     * @param hosts 远端列表
     */
    public LoadBalancedHostManager setHostList(List<String> hosts){
        if (hosts == null){
            hosts = new ArrayList<>(0);
        }

        //剔除空数据
        for (int i = 0 ; i < hosts.size() ; i++) {
            if (CheckUtils.isEmptyOrBlank(hosts.get(i))){
                hosts.remove(i);
                i--;
            }
        }

        if (!initialized){
            try {
                initLock.lock();
                if (!initialized){
                    settingInstall(hosts);
                    initialized = true;
                    return this;
                }
            } finally {
                initLock.unlock();
            }
        }

        newSettings.set(hosts);
        settingThreadPool.execute(settingInstallTask);
        return this;
    }

    /**
     * [可运行时修改]
     * 如果设置为false(默认), 当所有远端都被阻断时, nextHost方法返回一个后端.
     * 如果设置为true, 当所有远端都被阻断时, nextHost方法返回null.
     */
    public LoadBalancedHostManager setReturnNullIfAllBlocked(boolean returnNullIfAllBlocked) {
        this.returnNullIfAllBlocked = returnNullIfAllBlocked;
        return this;
    }

    /**
     * 设置客户端的标识
     * @param tag 标识
     */
    public LoadBalancedHostManager setTag(String tag) {
        this.tag = tag != null ? tag + "> " : "";
        return this;
    }

    /**
     * 获得当前远端列表和状态
     * @return value=true:可用, value=false:不可用
     */
    public Map<String, Boolean> getHostsStatus(){
        Host[] hostArray = this.hostArray;

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

    /**
     * 文本方式输出当前远端列表和状态
     * @param prefix 文本前缀
     * @return 远端列表和状态
     */
    public String printHostsStatus(String prefix){
        Host[] hostArray = this.hostArray;

        StringBuilder stringBuilder = new StringBuilder(prefix != null ? prefix : "");

        if (hostArray.length <= 0){
            stringBuilder.append(" [No host]");
            return stringBuilder.toString();
        }

        long currentTimeMillis = System.currentTimeMillis();

        for (Host host : hostArray){
            stringBuilder.append(" [");
            stringBuilder.append(host.getUrl());
            stringBuilder.append("] ");
            stringBuilder.append(!host.isBlocked(currentTimeMillis));
        }
        return stringBuilder.toString();
    }

    Host[] getHostArray(){
        return this.hostArray;
    }

    private Runnable settingInstallTask = new Runnable() {
        @Override
        public void run() {
            List<String> newSettings;
            while ((newSettings = LoadBalancedHostManager.this.newSettings.getAndSet(null)) != null){
                settingInstall(newSettings);
            }
        }
    };

    private void settingInstall(List<String> newSettings) {
        Host[] hostArray = LoadBalancedHostManager.this.hostArray;

        int newSize = newSettings.size();
        Host[] newHostArray = new Host[newSize];
        Map<String, Integer> newHostIndexMap = new HashMap<>(newSize);

        for (int i = 0 ; i < newSize ; i++){

            //trim
            String newUrl = newSettings.get(i).trim();
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

        LoadBalancedHostManager.this.hostArray = newHostArray;
        hostIndexMap = newHostIndexMap;

        if (logger.isInfoEnabled()) {
            logger.info(printHostsStatus(tag + "New hosts set:"));
        }
    }

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
            long newTime = System.currentTimeMillis() + duration;
            if (newTime < blockingTime.get()){
                return;
            }
            blockingTime.set(newTime);
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
