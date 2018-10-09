# TxTimer 日志样例

* SLF4J日志级别`INFO`, 日志包路径`sviolet.slate.common.x.monitor.txtimer.def`
* `Group (...) Time (...)  Page ...`: 当前打印的交易组别, 统计时间区间(非精确), 页码
* 默认情况下, 当一个组别的输出记录超过20条时, 会分页, 每页的页码会+1
* ` ... > last ? min ( cnt: ... avg: ... max:... min:... ) `: 该交易最近N分钟的交易数(cnt), 平均耗时(avg), 最大耗时(max), 最小耗时(min)
* `total ( cnt:... ing:... est-avg:... )`: 该交易累计交易数(cnt), 当前执行中的交易数(ing), 平均耗时估计(est-avg)

```text
2018-10-03 09:12:38,978 INFO Slate-TxTimer-Report-0 s.s.common.utilx.txtimer.def.Reporter : 
TxTimer | ------------------------------------------------------------------------------------------------------------
TxTimer | Group (Scrunchy-Actions) Time (10/03 09:09 - 10/03 09:12)  Page 1
TxTimer | template.api.base.UserService#get > last 2 min ( cnt:44, avg:1ms, max:16ms, min:0ms ) total ( cnt:44, ing:0, est-avg: 1ms )
TxTimer | template.api.base.UserService#set > last 2 min ( cnt:44, avg:0ms, max:13ms, min:0ms ) total ( cnt:44, ing:0, est-avg: 0ms )
```