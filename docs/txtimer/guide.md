# TxTimer 简单的交易耗时统计

* TxTimer支持`ThistleSpi`扩展, 并提供一个缺省的实现
* `缺省实现`实现了交易耗时的统计, 并通过日志定时输出报告
* 本文档讲述`缺省实现`的用法

### 全局禁用

* TxTimer默认开启, 如需关闭, 添加启动参数

```text
-Dslate.txtimer.enabled=false
```

### 特征信息

* 日志前缀`TxTimer`
* 日志包路径`sviolet.slate.common.utilx.txtimer.def`


