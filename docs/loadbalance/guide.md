# 支持软负载的HttpClient使用手册

* 支持配置多个后端地址, 平均分配流量
* 支持被动/主动方式探测后端是否可用, 自动选择可用的后端发送请求
* 支持同步/异步方式请求

```text
Slate 11.X 后该客户端的包路径从sviolet.slate.common.modelx.loadbalance调整为sviolet.slate.common.x.net.loadbalance
```

# 关于阻断机制

* 当主动探测器发现某个后端不可用(telnet或http-get), 会将其标记为阻断状态, 称为主动阻断
* 当网络请求发生特定的异常, 会将对应后端标记为阻断状态, 称为被动阻断
* 程序尽可能不向被阻断的后端发送请求, 直到阻断时间结束
* 被动阻断时, 阻断时间结束会进入恢复期, 期间只允许向该后端发起一次请求, 若请求成功, 则放开流量限制, 否则继续阻断
* 当所有后端都被标记为阻断状态时, 会将请求随机发给这些后端(可配置为拒绝请求)
* 主动阻断时长由主动探测间隔决定, 主动探测间隔可配置
* 被动阻断时长可配置
* 恢复期时长可配置

# 配置

* [Spring XML配置](https://github.com/shepherdviolet/slate/blob/master/docs/loadbalance/config-xml.md)
* [Spring 注解配置](https://github.com/shepherdviolet/slate/blob/master/docs/loadbalance/config-annotation.md)
* [SpringBoot YML自动配置](https://github.com/shepherdviolet/slate/blob/master/docs/loadbalance/config-springboot.md)

# 调用

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
