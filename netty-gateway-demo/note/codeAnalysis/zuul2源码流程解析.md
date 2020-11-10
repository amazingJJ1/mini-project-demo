## zuul2总结

### 大体流程：

首先整个流程大体分为3个组件

- Netty Server  负责监听客户端端连接，初始化channel,触发filter chain
- fliter chain 这部分主要是加载的groovy filter 
- Netty client 

1. 客户端请求Netty Server
2. Netty Server 建立连接,初始化channel pipeline
3. ser

 

### 源码流程解析:

以zuul-sample为例。

BaseServerStartup初始化NettyServer时，首先初始化ServerChannelInitializer。

对应不同协议采用不同的ChannelInitializer：

- HTTP是ZuulServerChannelInitializer

- Http2是Http2SslChannelInitializer

- Https是Http1MutualSslChannelInitializer

- Websocket是SampleWebSocketPushChannelInitializer

  

我们这里分析Http的流程，也就是ZuulServerChannelInitializer。

可以看到server channel 初始化主要是构造pipeline,添加以下的handler

- addTimeoutHandlers(pipeline) 设置读写和空闲超时，发送相关超时事件
- addPassportHandler(pipeline) 主要是状态标识
- addTcpRelatedHandlers(pipeline) tcp相关配置：最大连接数，负载均衡代理haproxy等
- addHttp1Handlers(pipeline)  http编码解码，关闭连接，连接过期处理
- addHttpRelatedHandlers(pipeline) http body计数，httpServer生命周期管理，metrics，访问日志，请求头处理，限流处理,黑白名单处理
- **addZuulHandlers(pipeline)**  主要的业务处理，获取ZuulFilterChain

跟进addZuulFilterChainHandler（）

1. 首先是加载所有的ZuulFilter,简单分为Inbound,Outbound两种类型
2. 将所有的zuulFilter组成链，包装成ZuulFilterChainRunner
3. 构建成ZuulFilterHandler（requestFilterChain, responseFilterChain）

ZuulHandler是业务处理的入口，channelRead触发runFilter。

后面触发ZuulEndPointRunner的filter(),执行的过程中会根据请求生成一个**ProxyEndpoint**。

ProxyEndpoint执行可以设置同步和异步模式，异步模式是用rxjava线程切换实现，其他其实差不多，这里跟进同步执行的代码。

```java
if (filter.getSyncType() == FilterSyncType.SYNC) {
    final SyncZuulFilter<I, O> syncFilter = (SyncZuulFilter) filter;
    final O outMesg = syncFilter.apply(inMesg);
    recordFilterCompletion(SUCCESS, filter, startTime, inMesg, snapshot);
    return (outMesg != null) ? outMesg : filter.getDefaultOutput(inMesg);
}

// async filter
filter.incrementConcurrency();
resumer = new FilterChainResumer(inMesg, filter, snapshot, startTime);
filter.applyAsync(inMesg)
    .observeOn(Schedulers.from(getChannelHandlerContext(inMesg).executor()))
    .doOnUnsubscribe(resumer::decrementConcurrency)
    .subscribe(resumer);
```

#### ProxyEndpoint 

执行代理请求的类，进入proxyRequestToOrigin();

```java
// We pass this AtomicReference<Server> here and the origin impl will assign the chosen server to it.
//获取netty client连接目标服务的channel,双向队列的方式构建了channel池
promise = origin.connectToOrigin(zuulRequest, channelCtx.channel().eventLoop(), attemptNum, passport, chosenServer, chosenHostAddr);

storeAndLogOriginRequestInfo();
currentRequestAttempt = origin.newRequestAttempt(chosenServer.get(), context, attemptNum);
requestAttempts.add(currentRequestAttempt);
passport.add(PassportState.ORIGIN_CONN_ACQUIRE_START);

if (promise.isDone()) {
    //netty client channel连接成功执行的方法
    operationComplete(promise);
} else {
    promise.addListener(this);
}
```

进入operationComplete（），跟进onOriginConnectSucceeded（）

```java
private void writeClientRequestToOrigin(final PooledConnection conn, int readTimeout) {
    final Channel ch = conn.getChannel();
    passport.setOnChannel(ch);

    // set read timeout on origin channel
    ch.attr(ClientTimeoutHandler.ORIGIN_RESPONSE_READ_TIMEOUT).set(readTimeout);

    context.set(ORIGIN_CHANNEL, ch);
    context.set(POOLED_ORIGIN_CONNECTION_KEY, conn);

    preWriteToOrigin(chosenServer.get(), zuulRequest);

    //关键步骤，将ProxyEndpoint嵌入netty client channel的流程
    final ChannelPipeline pipeline = ch.pipeline();
    originResponseReceiver = getOriginResponseReceiver();
    pipeline.addBefore("connectionPoolHandler", OriginResponseReceiver.CHANNEL_HANDLER_NAME, originResponseReceiver);

    // check if body needs to be repopulated for retry
    repopulateRetryBody();

    //netty client channel将请求写给目标服务
    ch.write(zuulRequest);
    writeBufferedBodyContent(zuulRequest, ch);
    ch.flush();

    //让clent channel注册read事件
    //Get ready to read origin's response
    ch.read();

    originConn = conn;
    channelCtx.read();
}
```

这里将请求代理给了目标服务，代理请求前还会将originResponseReceiver嵌入Client channel的Pipeline，在目标服务响应之后，会通知ProxyEndpoint处理response

```java
public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof HttpResponse) {
        if (edgeProxy != null) {
            edgeProxy.responseFromOrigin((HttpResponse) msg);
        }
        ctx.channel().read();
    }
}    
```

跟进responseFromOrigin()最终调用

```java
protected void handleOriginSuccessResponse(final HttpResponse originResponse, Server chosenServer) {
    origin.recordSuccessResponse();
    if (originConn != null) {
        originConn.getServerStats().clearSuccessiveConnectionFailureCount();
    }
    final int respStatus = originResponse.status().code();
    long duration = 0;
    if (currentRequestStat != null) {
        currentRequestStat.updateWithHttpStatusCode(respStatus);
        duration = currentRequestStat.duration();
    }
    if (currentRequestAttempt != null) {
        currentRequestAttempt.complete(respStatus, duration, null);
    }
    // separate nfstatus for 404 so that we can notify origins
    final StatusCategory statusCategory = respStatus == 404 ? SUCCESS_NOT_FOUND : SUCCESS;
    zuulResponse = buildZuulHttpResponse(originResponse, statusCategory, context.getError());
    invokeNext(zuulResponse);
}
```

invokeNext(zuulResponse)的执行很简单,将response消息事件通知server channel，最终返回客户端

```java
private void filterResponse(final HttpResponseMessage zuulResponse) {
    if (responseFilters != null) {
        responseFilters.filter(zuulResponse);
    } else {
        //response 通过事件通知给netty server channel
        channelCtx.fireChannelRead(zuulResponse);
    }
}
```

源码流程结束。



### 其他相关

#### netty client的配置

NettyClientConnectionFactory 配置nettyClient bootstrap,连接时获取channel

```java
public ChannelFuture connect(final EventLoop eventLoop, String host, final int port, CurrentPassport passport) {

    Class socketChannelClass;
    if (Server.USE_EPOLL.get()) {
        socketChannelClass = EpollSocketChannel.class;
    } else {
        socketChannelClass = NioSocketChannel.class;
    }

    SocketAddress socketAddress = new InetSocketAddress(host, port);

    final Bootstrap bootstrap = new Bootstrap()
            .channel(socketChannelClass)
            .handler(channelInitializer)
            .group(eventLoop)
            .attr(CurrentPassport.CHANNEL_ATTR, passport)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connPoolConfig.getConnectTimeout())
            .option(ChannelOption.SO_KEEPALIVE, connPoolConfig.getTcpKeepAlive())
            .option(ChannelOption.TCP_NODELAY, connPoolConfig.getTcpNoDelay())
            .option(ChannelOption.SO_SNDBUF, connPoolConfig.getTcpSendBufferSize())
            .option(ChannelOption.SO_RCVBUF, connPoolConfig.getTcpReceiveBufferSize())
            .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, connPoolConfig.getNettyWriteBufferHighWaterMark())
            .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, connPoolConfig.getNettyWriteBufferLowWaterMark())
            .option(ChannelOption.AUTO_READ, connPoolConfig.getNettyAutoRead())
            .remoteAddress(socketAddress);

    ZuulBootstrap zuulBootstrap = new ZuulBootstrap(bootstrap);
    if (!zuulBootstrap.getResolver(eventLoop).isResolved(socketAddress)) {
        LOGGER.warn("NettyClientConnectionFactory got an unresolved server address, host: " + host + ", port: " + port);
        unresolvedDiscoveryHost.increment();
    }
    return bootstrap.connect();
}
```

#### clientchannel pipeline初始化配置

DefaultOriginChannelInitializer ，在DefaultClientChannelManager构造时触发

```java
protected void initChannel(Channel ch) throws Exception {
    final ChannelPipeline pipeline = ch.pipeline();

    pipeline.addLast(new PassportStateOriginHandler());

    if (connectionPoolConfig.isSecure()) {
        pipeline.addLast("ssl", sslContext.newHandler(ch.alloc()));
    }

    pipeline.addLast(HTTP_CODEC_HANDLER_NAME, new HttpClientCodec(
            BaseZuulChannelInitializer.MAX_INITIAL_LINE_LENGTH.get(),
            BaseZuulChannelInitializer.MAX_HEADER_SIZE.get(),
            BaseZuulChannelInitializer.MAX_CHUNK_SIZE.get(),
            false,
            false
    ));
    pipeline.addLast(PassportStateHttpClientHandler.PASSPORT_STATE_HTTP_CLIENT_HANDLER_NAME, new PassportStateHttpClientHandler());
    pipeline.addLast("originNettyLogger", nettyLogger);
    pipeline.addLast(httpMetricsHandler);
    addMethodBindingHandler(pipeline);
    pipeline.addLast("httpLifecycle", new HttpClientLifecycleChannelHandler());
    pipeline.addLast(new ClientTimeoutHandler());
    pipeline.addLast("connectionPoolHandler", connectionPoolHandler);
}
```