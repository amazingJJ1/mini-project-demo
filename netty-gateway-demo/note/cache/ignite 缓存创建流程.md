# ignite 缓存创建流程

客户端：

发起调用请求流程：

- 获取

服务端：

服务端启动时会启动ClientListenerProcessor线程服务，该线程启动时会启动GridNioServer,start()方法中

- 1、NioServer监听连接事件,NioServer.AbstractNioClientWorker负责处理相应的selectorKey。
- 2、nioserver消息读取完毕，调用Nio FilterChain链处理onMessageReceived(ses, readBuf)。调用链一般有两个过滤器[**GridNioAsyncNotifyFilter**,**GridNioCodecFilter**]
- 3、过滤链处理完毕后尾部的TailFilter调用**ClientListenerNioLister**的OnMessage
- 4、ClientListenNioListener从nioSession中获取消息解析器ClientListenerMessageParser和请求处理器ClientListenerRequestHandler，解码msg获取request,鉴权后处理request
- 5、请求分为3种，客户端请求，jdbc请求，Odbc请求，对应3个处理器
  - ClientRequestHandler
  - JdbcRequestHandler
  - OdbcRequestHandler
- 6、这里解析客户端请求流程，客户端请求有多种:
- ClientCacheGetNameRequest
  - ClientCacheGetOrCreateWithConfigrationRequest
  - ClientTxStartRequest , ClitentTxStartRequest
  - ClientCacheDestroyRequest
  - ClientCacheRequest
    - ClientCacheSqlFieldsQueryRequest
    - ClientCacheSqlQueryRequest
    - ClientCacheScanQueryRequest
    - ClientCacheKeysRequest
- 7、这里我们是通过配置创建缓存，也就是ClientCacheGetOrCreateWithConfigrationRequest，请求处理本质基本是代理给IgniteKernal去执行这些任务，而IgniteKernal最终将任务代理给GridCacheProcessor处理，这里创建缓存是通过GridCacheProcessor.dynamicStartCache
- 8、GridCacheProcessor初始化缓存配置initialize(cfg, cacheObjCtx)，初始化DynamicCacheChangeRequest，这里面会进行表，字段大小写的设置。另外会设置DynamicCacheStartFuture监听回调，并将请求放入DynamicCacheChangeRequest队列。
- 9、接着GridCacheProcessor将所有待发送的request打包成DynamicCacheChangeBatch使用GridDiscoveryManager发送sendCustomEvent。
- 10、GridDiscoveryManager 将发送动作代理给TcpDiscoverySpi，默认是ServerImpl发送消息
- 11、ServerImpl将消息包装成TcpDiscoveryCustomEventMessage，将消息交给RingMessageWork处理。
- 12、RingMessageWork将消息放入队列，让其他线程进行异步处理。其实这里还是RingMessageWork线程本身轮询拉取到消息进行处理，执行processMessage,这里会根据消息类型进行不同处理，这里我们是CustomMessage,RingMessageWork进行processCustomMessage
- 13、RingMessageWork处理消息的过程会通知监听器notifyDiscoveryListener，这里的监听器是DiscoveryManager,执行OnDisCovery流程
- 14、在OnDiscovery0()中，GridDiscoveryManager将custMsg封装成event放入DiscoverWork的evts队列中，DiscoverWork线程轮询拉取事件进行异步处理
- 15、discoveryWork根据事件不同进行不同处理，Custom event交给GridEventStorageManager存储，之后会通知监听器ServiceDiscoveryListener的OnEvent(),根据事件的不同进行相应处理，这里是DynamicCacheChangeBatch,通过onDiscoveryEvent流程将事件交给GridCachePartitionExchangeManager处理
- 16、在GridCchePartitionExchangeManager中customMsg被封装成exchangeAction,并设置相应的exchFut进行监听结果，通过exchfut.exchangeFuture（）将处理fut.onEvent打开EvtLatch的线程屏障锁。最后通过addFuture(exchFut)将消息传递给exchangeWork的FutQ队列中。
- 17、ExchangeWork线程轮询futQ,判断Task类型，这里如果是GridDhtPartitionsExchangeFuture类型，则进行exchFut.init(newCrd)，处理消息，进行onCacheChangeRequest(crdNode),这里面调用CacheAffinitySharedManager进行请求处理
- 18、CacheAffinitySharedManager.onCacheChangeRequest调用GridCacheProcessor.prepareStartCaches()，最终调用GridQueryProcessor.onCacheStart（），这里面会获取缓存表所需要的信息，比如schema,索引信息等等，然后进行缓存创建。见registerCache0(cacheName, schemaName, cacheInfo, cands, isSql);
- 最终：

  - IgniteH2Indexing.registerCache(),内部通过H2的sql创建了schema
  - registerType()通过H2创建了表格




### Thin Client 分布式sql执行流程

重点部分：

- 分布式sql汇总
- igniteH2索引处理

基本流程：

- NioServer selector监听---->filterChain 处理onMessageReceived。默认解析消息成JdbcQueryExecuteRequest
- JdbcRequestHandler处理，将sql包装成SqlFieldsQueryEx,设置sql参数，是否是分布式的，需不需要缓存，分页参数等
- igniteH2Indexing先本地查询h2获取QueryParserResult
- igntieH2Indexing执行executeSelector进行结果汇总，汇总阶段是两段式的
- 接着GridReduceQueryExecutor进行reduceQuery,本质是重新构建GridH2QueryRequest 发送到所有集群节点，然后awaitAllReplies等待所有响应结果进行汇总



