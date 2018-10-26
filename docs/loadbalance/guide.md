# 负载均衡的HttpClient使用手册

* 支持配置多个后端地址, 平均分配流量(随机, 不可指定)
* 支持被动/主动方式探测后端是否可用, 自动选择可用的后端发送请求
* 支持同步/异步方式请求

> Slate 11.X 版本后该客户端的包路径从sviolet.slate.common.modelx.loadbalance调整为sviolet.slate.common.x.net.loadbalance <br>

# 设计思路

> 在分布式架构中, 为了消除热点, 服务方会以集群方式提供服务. <br>
> 对外提供接口时, 通常有运营商/硬件负载(F5)/软件负载(Nginx/Apache等)提供负载均衡, 即请求方向唯一的域名发送请求, 负载均衡负责将流量分配给服务集群. <br>
> 对内部系统提供接口时, 通常不会使用可靠的硬件负载均衡设备, 软负载(Nginx)也存在宕机的风险, 简单的域名映射无法探知后端状态, 当服务方集群中的部分机器停机或故障时, 会导致大量的请求失败. <br>
> 本HttpClient就是为了内部系统访问设计的, 在请求方实现负载均衡, 平均分配流量, 并提供探知后端状态的能力, 尽可能地将请求向健康的服务器发送. <br>

# 关于阻断机制

> 当主动探测器发现某个后端不可用(telnet或http-get), 会将其标记为阻断状态, 称为主动阻断<br>
> 当网络请求发生特定的异常, 会将对应后端标记为阻断状态, 称为被动阻断<br>
> 程序尽可能不向被阻断的后端发送请求, 直到阻断时间结束<br>
> 被动阻断时, 阻断时间结束会进入恢复期, 期间只允许向该后端发起一次请求, 若请求成功, 则放开流量限制, 否则继续阻断<br>
> 当所有后端都被标记为阻断状态时, 会将请求随机发给这些后端(可配置为拒绝请求)<br>
> 主动阻断时长由主动探测间隔决定, 主动探测间隔可配置, 被动阻断时长可配置, 恢复期时长可配置<br>
> 如果服务方支持, 使用http-get方式做主动探测更可靠(需要服务方提供一个http-get接口, 返回200)<br>

# 配置客户端

> 注意: 每个HttpClient实例必须对应一个服务方集群(它们提供相同的服务), 不同的服务方集群应该配置不同的HttpClient实例与之对应. <br>
> 例如, 请求方系统需要向 A系统(150.1.1.1,155.1.1.2) / B系统(150.1.2.1,150.1.2.2) 发送请求 <br>
> 则需要配置两个客户端: clientA(http://150.1.1.1:8080,http://155.1.1.2:8080) / ClientB(http://150.1.2.1:8080,http://155.1.2.2:8080) <br>

* [Spring XML配置](https://github.com/shepherdviolet/slate/blob/master/docs/loadbalance/config-xml.md)
* [Spring 注解配置](https://github.com/shepherdviolet/slate/blob/master/docs/loadbalance/config-annotation.md)
* [SpringBoot YML自动配置](https://github.com/shepherdviolet/slate/blob/master/docs/loadbalance/config-springboot.md)

# 调用客户端

* [同步发送请求](https://github.com/shepherdviolet/slate/blob/master/docs/loadbalance/invoke-sync.md)
* [异步发送请求](https://github.com/shepherdviolet/slate/blob/master/docs/loadbalance/invoke-async.md)

# 关于日志

### 常规日志

* SLF4J日志包路径: `sviolet.slate.common.x.net.loadbalance`
* 推荐日志级别: `INFO`
* 日志关键字: `HttpClient` / `LoadBalance`
* 包含: 请求/响应日志, Hosts变化日志, 主动探测日志, 被动阻断日志等
* 另外, 建议将`sviolet.slate.common.x.net.loadbalance.LoadBalancedInspectManager`日志输出到单独文件中(主动探测日志)

### SpringBoot自动配置日志

* SLF4J日志包路径: `sviolet.slate.springboot.x.net.loadbalance`
* 推荐日志级别: `INFO`
* 日志关键字: `HttpClients`
* 包含: 客户端自动配置日志, 客户端配置实时调整日志等
