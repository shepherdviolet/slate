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

    /**
     * 按配置数据所描述的信息调整集合中的客户端及参数, 可以增减客户端实例, 调整客户端参数
     * @param config 配置数据(描述客户端及参数)
     */
    void update(String config);

}
