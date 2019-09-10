package com.dzz;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Attribute;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;


/**
 * @author zoufeng
 * @date 2019/3/26
 */
public class ProxyClient {

    private Logger logger = Logger.getLogger(ProxyClient.class.getName());

    private final EventLoopGroup group = new NioEventLoopGroup(2);

    private final Bootstrap clientBootstrap = new Bootstrap();

    private int maxChannelPoolSize = 10;

    private ConcurrentHashMap<String, ArrayBlockingQueue<Channel>> channelMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, AtomicInteger> channelSizeMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<ChannelId, ArrayBlockingQueue<Channel>> channelIdMap = new ConcurrentHashMap();

    private PooledByteBufAllocator allocator = new PooledByteBufAllocator(false);

    public ProxyClient() {
        init();
    }

    private void init() {
        clientBootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline()
                                .addFirst(new HttpClientCodec())
                                .addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS))
                                //支持chunk流编码 用于文件传输
                                // p.addLast("http-chunked", new ChunkedWriteHandler());
                                .addLast("proxyClient", new ProxyClientHandler());
                        ;
                    }
                });
    }


    public void stop() {
        try {
            group.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public Channel getProxyClientChannel(String host, int port) {
        ArrayBlockingQueue<Channel> queue = channelMap.get(host + ":" + port);

        if (queue == null || queue.size() == 0) {
            return createConnector(host, port);
        }

        Channel result = null;
        try {
            while (true) {
                if (queue.size() == 0) {
                    result = createConnector(host, port);
                    break;
                }
                Channel channel = queue.take();
                if (channel.isOpen()) {
                    result = channel;
                    break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 队列
     *
     * @param host 地址
     * @param port 端口
     * @return 新建的channel池
     */
    public Channel createConnector(String host, int port) {
        String key = host + ":" + port;
        ArrayBlockingQueue<Channel> channelQueue = channelMap.get(key);
        Channel take = null;
        try {
            if (channelQueue == null) {
                channelQueue = new ArrayBlockingQueue<Channel>(maxChannelPoolSize);
                channelMap.put(key, channelQueue);
                AtomicInteger atomicInteger = new AtomicInteger(0);
                channelSizeMap.put(key, atomicInteger);
                createChannel(host, port, channelQueue, atomicInteger);
            } else {

                AtomicInteger atomicInteger = channelSizeMap.get(key);
                if (channelQueue.size() == 0 && atomicInteger.get() < maxChannelPoolSize) {
                    createChannel(host, port, channelQueue, atomicInteger);
                }
            }
            //todo 超时释放
            take = channelQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return take;
    }


    private void createChannel(String host, int port, ArrayBlockingQueue<Channel> channelQueue, AtomicInteger channelSize) throws InterruptedException {
        clientBootstrap.connect(host, port).addListener(new GenericFutureListener<ChannelFuture>() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                logger.info("connect ok!");
                Channel channel = future.channel();
                if (channelQueue.offer(channel)) {
                    channelSize.getAndIncrement();
                }
                channel.closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
                    @Override
                    public void operationComplete(Future<? super Void> future) throws Exception {
                        logger.info(channel.id() + " is closed !! ");
                        channelSize.decrementAndGet();
                    }
                });
            }
        }).await();

    }

    public void proxyWriteMsg(Channel channel, Object obj) {
        if (obj instanceof String) {
            String msg = (String) obj;
            ByteBuf byteBuf = allocator.heapBuffer(msg.length());
            byteBuf.writeBytes(msg.getBytes());
            channel.writeAndFlush(byteBuf);
        } else {
            logger.info("nothing todo");
        }
    }

    public Channel getProxyClientChannel(String host, int port, Channel parentChannel) {
        Channel proxyClientChannel = getProxyClientChannel(host, port);
        Attribute<Channel> attr = proxyClientChannel.attr(GwServerHandler.PARENT_CHANNEL_KEY);
        attr.set(parentChannel);
        return proxyClientChannel;
    }

    public void release(Channel channel) {
        SocketAddress socketAddress = channel.remoteAddress();
        if (socketAddress instanceof InetSocketAddress) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
            String key = inetSocketAddress.getHostName() + ":" + inetSocketAddress.getPort();
            ArrayBlockingQueue<Channel> channels = channelMap.get(key);
            if (channels == null) {
                channels = new ArrayBlockingQueue<>(10);
                channelMap.put(key, channels);
            }
            try {
                channels.put(channel);
                channel.read();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        logger.info("channel active : " + socketAddress.toString());
        logger.info(channel.id().asShortText() + "is channelActive");
    }
}
