package sviolet.slate.springboot.x.net.loadbalance.auto;

import sviolet.slate.common.x.net.loadbalance.classic.SimpleOkHttpClient;

import java.util.Set;

/**
 * 使用slate.httpclients自动配置的SimpleOkHttpClient集合
 */
public interface HttpClients {

    /**
     * 获取Http请求客户端
     * @param tag tag(标识)
     * @return SimpleOkHttpClient
     */
    SimpleOkHttpClient get(String tag);

    /**
     * @return 返回集合中的客户端数量
     */
    int size();

    /**
     * @return 返回集合中所有客户端的tag
     */
    Set<String> tags();

}
