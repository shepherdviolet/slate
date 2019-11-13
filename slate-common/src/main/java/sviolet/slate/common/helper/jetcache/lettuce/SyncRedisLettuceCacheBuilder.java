/*
 * Copyright (C) 2015-2019 S.Violet
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

package sviolet.slate.common.helper.jetcache.lettuce;

import com.alicp.jetcache.CacheGetResult;
import com.alicp.jetcache.CacheResult;
import com.alicp.jetcache.MultiGetResult;
import com.alicp.jetcache.redis.lettuce.RedisLettuceCache;
import com.alicp.jetcache.redis.lettuce.RedisLettuceCacheBuilder;
import com.alicp.jetcache.redis.lettuce.RedisLettuceCacheConfig;
import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * JetCache的Lettuce方式原生是异步的, 性能好, 但对于对JetCache不了解的人, 如果拿JetCache去存一致性要求的数据时,
 * 会因为异步发生问题, 因此这个CacheBuilder牺牲性能, 将Redis操作改为同步, 依赖: com.alicp.jetcache:jetcache-starter-redis-lettuce
 *
 * @author S.Violet
 */
public class SyncRedisLettuceCacheBuilder extends RedisLettuceCacheBuilder<SyncRedisLettuceCacheBuilder> {

    public static SyncRedisLettuceCacheBuilder createSyncRedisLettuceCacheBuilder() {
        return new SyncRedisLettuceCacheBuilder();
    }

    @SuppressWarnings("unchecked")
    public SyncRedisLettuceCacheBuilder() {
        buildFunc(config -> {
            RedisLettuceCacheConfig lettuceCacheConfig = (RedisLettuceCacheConfig)config;

            /*
                避免Redis集群拓扑变化时报出错误: Connection to ?:? not allowed. This connection point is not known in the cluster view
                开启集群拓扑刷新(topologyRefreshOptions): 当服务端拓扑发生变化时, 短时间内还会出现连接错误, 刷新后才恢复
                关闭集群节点验证(validateClusterNodeMembership): 当服务端拓扑发生变化时, 由于不验证, 能够更快恢复(但是, 官方默认开启验证, 应该是有某种原因的, 所以这里默认不用这个方案)
             */
            AbstractRedisClient redisClient = lettuceCacheConfig.getRedisClient();
            if (redisClient instanceof RedisClusterClient) {
                ((RedisClusterClient) redisClient).setOptions(ClusterClientOptions.builder()
                        .topologyRefreshOptions(ClusterTopologyRefreshOptions.builder()
                                .enablePeriodicRefresh()//开启定期刷新, 默认关闭
//                                .refreshPeriod(Duration.ofMinutes(1))//默认1分钟
                                .enableAdaptiveRefreshTrigger()//开启拓扑刷新, 默认关闭
                                .enableAllAdaptiveRefreshTriggers()//启用全部拓扑刷新定时器
                                .build())
//                        .validateClusterNodeMembership(false)//直接关闭集群节点检查, 默认true(避免Redis集群拓扑变化时报出错误: Connection to ?:? not allowed. This connection point is not known in the cluster view)
                        .build());
            }

            /*
                同步化
             */
            return new RedisLettuceCache(lettuceCacheConfig) {
            @Override
            protected CacheResult do_PUT(Object key, Object value, long expireAfterWrite, TimeUnit timeUnit) {
                CacheResult result = super.do_PUT(key, value, expireAfterWrite, timeUnit);
                result.getResultCode();
                return result;
            }

            @Override
            protected CacheResult do_PUT_ALL(Map map, long expireAfterWrite, TimeUnit timeUnit) {
                CacheResult result = super.do_PUT_ALL(map, expireAfterWrite, timeUnit);
                result.getResultCode();
                return result;
            }

            @Override
            protected CacheGetResult do_GET(Object key) {
                CacheGetResult result = super.do_GET(key);
                result.getResultCode();
                return result;
            }

            @Override
            protected MultiGetResult do_GET_ALL(Set keys) {
                MultiGetResult result = super.do_GET_ALL(keys);
                result.getResultCode();
                return result;
            }

            @Override
            protected CacheResult do_REMOVE(Object key) {
                CacheResult result = super.do_REMOVE(key);
                result.getResultCode();
                return result;
            }

            @Override
            protected CacheResult do_REMOVE_ALL(Set keys) {
                CacheResult result = super.do_REMOVE_ALL(keys);
                result.getResultCode();
                return result;
            }

            @Override
            protected CacheResult do_PUT_IF_ABSENT(Object key, Object value, long expireAfterWrite, TimeUnit timeUnit) {
                CacheResult result = super.do_PUT_IF_ABSENT(key, value, expireAfterWrite, timeUnit);
                result.getResultCode();
                return result;
            }
        };
        });
    }
}
