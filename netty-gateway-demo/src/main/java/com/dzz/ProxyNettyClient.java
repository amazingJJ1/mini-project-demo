package com.dzz;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author zoufeng
 * @date 2019/3/26
 */
public class ProxyNettyClient {

    private final EventLoopGroup group = new NioEventLoopGroup(2);

    private final Bootstrap clientBootstrap = new Bootstrap();

    public static ConcurrentHashMap<String, ArrayBlockingQueue<Channel>> channelMap = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<ChannelId, ArrayBlockingQueue<Channel>> channelIdMap = new ConcurrentHashMap();

    PooledByteBufAllocator allocator = new PooledByteBufAllocator(false);

    public ProxyNettyClient() {
        init();
    }

    private void init() {
        clientBootstrap.group(group);
        clientBootstrap.channel(NioSocketChannel.class);
        clientBootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                socketChannel.pipeline()
                        .addLast(new ClientHandler())
                        .addLast("http-decoder", new HttpRequestDecoder())
                        //聚合
                        .addLast("http-objectAggregator", new HttpObjectAggregator(65536))
                        //支持chunk流编码 用于文件传输
                        // p.addLast("http-chunked", new ChunkedWriteHandler());
                        .addLast("http-encoder", new HttpResponseEncoder());
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


    public Channel getProxyClientChannel(String inetHost, int inetPort) {
        ArrayBlockingQueue<Channel> queue = channelMap.get(inetHost + ":" + inetPort);
        if (queue == null) {
            return createConnector(inetHost, inetPort);
        } else {
            return queue.poll();
        }
    }

    /**
     * 队列
     *
     * @param inetHost
     * @param inetPort
     * @return
     */
    public Channel createConnector(String inetHost, int inetPort) {
        ArrayBlockingQueue<Channel> channelQueue = channelMap.get(inetHost + ":" + inetPort);
        if (channelQueue == null) {
            channelQueue = new ArrayBlockingQueue<Channel>(10);
            channelMap.put(inetHost + ":" + inetPort, channelQueue);
        }
        try {
            Channel channel = clientBootstrap.connect(inetHost, inetPort).sync().channel();
            ChannelId id = channel.id();
            channel.closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    System.out.println("------------------");
                    System.out.println(id.asShortText() + "is close!!");
                }
            });

            return channelQueue.take();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void proxyWriteMsg(Channel channel, Object obj) {
        if (obj instanceof String) {
            String msg = (String) obj;
            ByteBuf byteBuf = allocator.heapBuffer(msg.length());
            byteBuf.writeBytes(msg.getBytes());
            channel.writeAndFlush(byteBuf);
        } else {
            System.out.println("nothing todo");
        }
    }
}
