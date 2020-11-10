ignite服务端启动流程

在查看源码前我们先从启动日志了解下大概流程

日志记录：

1. IgniteConfiguration配置项设置，读取各种配置，线程池，节点发现SPI，节点通信SPI，数据分区SPI等

2. 插件配置

3. 失败处理配置，默认StopNodeOrHaltFailureHandler

4. 绑定tcp通信端口，比如默认的47100

5. 启动rest服务，监听11211和jetty的8080两个端口

6. 开启节点发现服务，监听47500端口

7. 指定集群mvcc版本的协调员（Assigned mvcc coordinator），开始集群拓扑

8. 集群信息路由初始化

9. ...

   

整个流程其实可以分为这几个阶段

- 配置解析阶段
- 节点发现
- 节点通信

### 配置解析

这部分没什么好分析的，主要是为了最终生成一个IgniteConfiguration

只不过ignite这里面实现了自己的IOC，又集成了spring的configuration解析



### 节点交互

ignite节点交互包括了节点发现和节点通信两部分，底层是依赖java的Nio，这里先看下他的Nio实现

#### ignite NIO server

ignite Nio部分的源码，大体设计和netty类似，selector分为接收连接的和读写数据的。数据处理也都是一个piepline的形式，配置监听器管理sockect信息处理生命周期。相对来说更轻量级。

GridNioServer启动流程

```java
public void start() {
    //先初始化启动过滤链
    filterChain.start();

    if (acceptWorker != null)
        //开启接收连接线程，内部包含一个selector
        new IgniteThread(acceptWorker).start();

    //启动客户端线程，这里的客户端线程分为直接内存和byteBuffer堆内存两种clientWorker
    for (IgniteThread thread : clientThreads)
        thread.start();
}
```

### 节点发现

节点发现主要是IgniteDiscoverySpi这部分逻辑,主要是实现是TcpDiscoverySpi

```java
//org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi#spiStart
@Override public void spiStart(@Nullable String igniteInstanceName) throws IgniteSpiException {
    //初始化一些sockect参数，ssl配置，响应超时时间，sockect超时这类的，还又metrics更新频率
    initializeImpl();

    registerMBean(igniteInstanceName, new TcpDiscoverySpiMBeanImpl(this), TcpDiscoverySpiMBean.class);
	
    //这根据clientMode分为服务节点还是客户端节点启动
    impl.spiStart(igniteInstanceName);
}
```

我们先看服务端的节点发现这里默认实现是ServerImpl

```java
//org.apache.ignite.spi.discovery.tcp.ServerImpl#spiStart
@Override public void spiStart(String igniteInstanceName) throws IgniteSpiException {
    synchronized (mux) {
        spiState = DISCONNECTED;
    }

    lastRingMsgReceivedTime = 0;

    utilityPool = new IgniteThreadPoolExecutor("disco-pool",
        spi.ignite().name(),
        0,
        1,
        2000,
        new LinkedBlockingQueue<Runnable>());

    // Clear addresses collections.
    fromAddrs.clear();
    noResAddrs.clear();

    //创建环形拓扑工作线程
    msgWorker = new RingMessageWorker(log);

    new MessageWorkerDiscoveryThread(msgWorker, log).start();

    if (tcpSrvr == null)
        tcpSrvr = new TcpServer(log);

    spi.initLocalNode(tcpSrvr.port, true);

    locNode = spi.locNode;

    // Start TCP server thread after local node is initialized.
    //这个TcpServer是个简单的sockectServer
    //这一步里主要又接收信息，分类处理信息的逻辑
    new TcpServerThread(tcpSrvr, log).start();

    ring.localNode(locNode);

    if (spi.ipFinder.isShared())
        registerLocalNodeAddress();
    else {
        if (F.isEmpty(spi.ipFinder.getRegisteredAddresses()))
            throw new IgniteSpiException("Non-shared IP finder must have IP addresses specified in " +
                "TcpDiscoveryIpFinder.getRegisteredAddresses() configuration property " +
                "(specify list of IP addresses in configuration).");

        ipFinderHasLocAddr = spi.ipFinderHasLocalAddress();
    }

    if (spi.getStatisticsPrintFrequency() > 0 && log.isInfoEnabled()) {
        statsPrinter = new StatisticsPrinter();
        statsPrinter.start();
    }

    spi.stats.onJoinStarted();

    joinTopology();

    spi.stats.onJoinFinished();

    if (spi.ipFinder.isShared()) {
        ipFinderCleaner = new IpFinderCleaner();
        ipFinderCleaner.start();
    }

    spi.printStartInfo();
}
```

节点发现处理的消息主要分几类：

- 加入环形拓扑请求RingMessage
- 待定消息PendingMessage

这块的处理可以查看ignite启动流程文档

### 节点通信部分

##### TcpCommunicationSpi



TcpCommunicationSpi启动方法查看

```java
public void spiStart(String igniteInstanceName) throws IgniteSpiException {
        assert locHost != null;

        // Start SPI start stopwatch.
        startStopwatch();

        if (!tcpNoDelay)
            U.quietAndWarn(log, "'TCP_NO_DELAY' for communication is off, which should be used with caution " +
                "since may produce significant delays with some scenarios.");

        registerMBean(igniteInstanceName, new TcpCommunicationSpiMBeanImpl(this), TcpCommunicationSpiMBean.class);
		//连接网关，这个网关只是一个读写锁
        connectGate = new ConnectGateway();

        if (shmemSrv != null) {
            //ipc通信工作线程
            shmemAcceptWorker = new ShmemAcceptWorker(shmemSrv);

            new IgniteThread(shmemAcceptWorker).start();
        }

        nioSrvr.start();

    //这个worker其实主要是处理断开连接的线程，保证无效连接关闭
        commWorker = new CommunicationWorker(igniteInstanceName, log);

        new IgniteSpiThread(igniteInstanceName, commWorker.name(), log) {
            @Override protected void body() {
                commWorker.run();
            }
        }.start();

        // Ack start.
        if (log.isDebugEnabled())
            log.debug(startInfo());
    }
```



默认是启动TCP_NODELAY，禁用了Nagle算法，允许小包的发送，这样可以避免消息延时

查看类结构

```java
private class CommunicationWorker{} //

private static class ConnectFuture{}

private static class HandshakeTimeoutObject<T> implements IgniteSpiTimeoutObject {}

private class HandshakeClosure extends IgniteInClosure2X<InputStream, OutputStream> {}

private class ConnectGateway {}

//断开的会话信息，里面包括节点的恢复信息GridNioRecoveryDescriptor和connect的索引
private static class DisconnectedSessionInfo {}
//连接策略
private static class FirstConnectionPolicy implements ConnectionPolicy {
        /** Thread connection index. */
        @Override public int connectionIndex() {
            return 0;
        }
}

//负载均衡的连接策略
private class RoundRobinConnectionPolicy implements ConnectionPolicy {
        /** {@inheritDoc} */
        @Override public int connectionIndex() {
            return (int)(U.safeAbs(Thread.currentThread().getId()) % connectionsPerNode);
        }
    }

//rmx信息监控
private class TcpCommunicationSpiMBeanImpl{}
```

其中的**ShmemWorker**是用来进程间通信的

IpcEndpoint里面的进程间通信主要是两种方式,tcp sockect和sharedMemory方式

然后统一抽象成流的方式获取通信信息，然后处理

```java
public interface IpcEndpoint extends Closeable {

    public InputStream inputStream() throws IgniteCheckedException;

    public OutputStream outputStream() throws IgniteCheckedException;

    @Override public void close();
}
```



>tmpfs是一套虚拟的文件系统，在其中创建的文件都是基于内存的，机器重启即消失。
>shmem是一套ipc，通过相应的ipc系统调用shmget能够以指定key创建一块的共享内存。需要使用这块内存的进程可以通过shmat系统调用来获得它。
>虽然是两套不同的接口，但是在内核里面的实现却是同一套。shmem内部挂载了一个tmpfs分区（用户不可见），shmget就是在该分区下获取名为"SYSV${key}"的文件。然后shmat就相当于mmap这个文件。
>所以我们接下来就把tmpfs和shmem当作同一个东西来讨论了。
>
>
>
>tmpfs/shmem是一个介于文件和匿名内存之间的东西。
>一方面，它具有文件的属性，能够像操作文件一样去操作它。它有自己inode、有自己的page cache；
>另一方面，它也有匿名内存的属性。由于没有像磁盘这样的外部存储介质，内核在内存紧缺时不能简单的将page从它们的page cache中丢弃，而需要swap-out；



##### GridWorker

ignite的GridWorker本质还是一个runnable，只不过是多了一些状态扩展，比如多了日志，心跳时间（执行任务时上传），是否完成，是否取消状态字段.

```java
    private final String igniteInstanceName;

    private final GridWorkerListener lsnr;

    private volatile boolean finished;

    protected volatile boolean isCancelled;

    private volatile Thread runner;
```

另外还配置了监听器GridWorkerListener，监听任务执行时一些生命周期的扩展

```java
public interface GridWorkerListener extends EventListener {

    public void onStarted(GridWorker w);
    
    public void onStopped(GridWorker w);
    
    public void onIdle(GridWorker w);
}
```

CommunicationWorker

