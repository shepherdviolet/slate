# HttpClient参数说明

* 仅提供部分配置的说明, 详见源码注释

### LoadBalancedHostManager配置

##### hosts

* 配置远端地址, 可配置多个, 逗号分隔
* 该参数可以在运行时`实时变更`. 例如使用apollo配置中心时, 可以在监听方法中使用LoadBalancedHostManager#setHosts()方法改变后端地址
* 例如:http://127.0.0.1:8081,http://127.0.0.1:8082

##### returnNullIfAllBlocked (可选项)

* 设置false时, 当所有后端都处于异常状态时, 随机选择一个后端发送请求
* 设置true时, 当所有后端都处于异常状态时, 抛出NoHostException
* 默认:false

### LoadBalancedInspectManager配置

##### hostManager

* 后端管理器(LoadBalancedHostManager, 必须配置)

##### inspectInterval (可选项)

* 探测间隔, 单位ms
* 默认:5000ms

##### inspector (可选项)

* 配置主动探测器, TelnetLoadBalanceInspector/HttpGetLoadBalanceInspector
* 可配置多个探测器, 依次探测
* 可自行实现探测逻辑
* 默认TelnetLoadBalanceInspector, 可不配置该选项

##### verboseLog (可选项)

* 开启更多的日志输出
* 默认:false

### MultiHostOkHttpClient配置

##### hostManager

* 后端管理器(LoadBalancedHostManager, 必须配置)

##### maxThreads

* 异步请求时该参数有效(asyncPost/asyncGet)
* 最大请求线程数
* 建议配置成较大值, 如:200
* 默认:64

##### maxThreadsPerHost

* 异步请求时该参数有效(asyncPost/asyncGet)
* 同一个后端地址的最大请求线程数
* 建议配置成较大值, 如:200
* 默认:64

##### connectTimeout

* 网络连接超时, 单位ms
* 默认:3000ms

##### writeTimeout

* 网络写超时, 单位ms
* 默认:10000ms

##### readTimeout

* 网络读超时, 单位ms
* 默认:10000ms

##### maxReadLength

* 允许读取的最大响应报文长度, 字节
* 默认:10L * 1024L * 1024L

##### passiveBlockDuration (可选项)

* 被动探测阻断时长, 单位ms
* 当请求发生网络异常(连接失败/超时等), 程序会暂停向该后端发送请求, 暂停时间由该参数决定
* 默认:6000ms

##### mediaType (可选)

* 请求报文的MediaType
* 默认:application/json;charset=utf-8

##### encode (可选)

* 请求报文的编码
* 默认:utf-8

##### headers (可选)

* HTTP报文头, Map

##### verboseLog (可选)

* 开启更多的日志输出
* 默认:false

##### verboseLogConfig (可选)

* 细粒度调整日志输出
* 在verboseLog=true时有效
* 默认:全输出

```text
1   (0x00000001) -> 打印: URL后缀 / URL参数(Map) / 请求报文体(Hex)
16  (0x00000010) -> 打印: 请求报文体(String)
256 (0x00000100) -> 打印: 未编码的完整URL(包括参数), http://host:port/app?key1=value1格式
4096(0x00001000) -> 打印: 响应码 / 响应信息

例如需要打印未编码的完整URL和请求报文体(String), 设置值为:256 + 16 = 272
```

##### cookieJar (可选)

* 实现请求的Cookie管理, 需自行实现
* 默认:无

##### proxy (可选)

* 代理配置
* 默认:无

##### dns (可选)

* DNS配置
* 默认:无
