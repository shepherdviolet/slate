# 支持软负载的Http请求客户端使用手册

* 包路径:sviolet.slate.common.modelx.loadbalance, sviolet.slate.springboot.modelx.loadbalance
* 支持配置多个后端地址, 平均分配流量
* 支持被动/主动方式探测后端是否可用, 自动选择可用的后端发送请求
* 支持同步/异步方式请求

# 配置

* [Spring XML配置](https://github.com/shepherdviolet/slate/blob/master/docs/loadbalance/config-xml.md)
* [Spring 注解配置](https://github.com/shepherdviolet/slate/blob/master/docs/loadbalance/config-annotation.md)
* [SpringBoot YML自动配置](https://github.com/shepherdviolet/slate/blob/master/docs/loadbalance/config-springboot.md)

# 调用

* [同步发送请求](https://github.com/shepherdviolet/slate/blob/master/docs/loadbalance/invoke-sync.md)
* [同步发送请求](https://github.com/shepherdviolet/slate/blob/master/docs/loadbalance/invoke-async.md)

# 其他

* [参数说明](https://github.com/shepherdviolet/slate/blob/master/docs/loadbalance/properties.md)
