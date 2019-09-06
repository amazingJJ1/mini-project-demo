package com.dzz;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.util.Attribute;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


/**
 * @author zoufeng
 * @date 2019/3/26
 */
public class ProxyNettyClient {

    private Logger logger = Logger.getLogger(ProxyNettyClient.class.getName());

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
        clientBootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        clientBootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                socketChannel.pipeline()
                        .addFirst(new HttpClientCodec())
//                        .addLast("http-decoder", new HttpRequestDecoder())
                        //聚合
//                        .addLast("http-objectAggregator", new HttpObjectAggregator(65536))
                        //支持chunk流编码 用于文件传输
                        // p.addLast("http-chunked", new ChunkedWriteHandler());
//                        .addLast("http-encoder", new HttpResponseEncoder())
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


    public Channel getProxyClientChannel(String inetHost, int inetPort) {
        ArrayBlockingQueue<Channel> queue = channelMap.get(inetHost + ":" + inetPort);
        if (queue == null || queue.size() == 0) {
            return createConnector(inetHost, inetPort);
        }
        return queue.poll();
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
        Channel take = null;
        try {

            if (channelQueue == null) {
                channelQueue = new ArrayBlockingQueue<Channel>(10);
                channelMap.put(inetHost + ":" + inetPort, channelQueue);

                ChannelFuture f = clientBootstrap.connect(inetHost, inetPort).addListener(new GenericFutureListener<Future<? super Void>>() {
                    @Override
                    public void operationComplete(Future<? super Void> future) throws Exception {
                        logger.info("connect ok!");
                    }
                }).await();
                Channel channel = f.channel();
                channelQueue.add(channel);


            }
            //todo 超时释放
            take = channelQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return take;
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

    public Channel getProxyClientChannel(String inetHost, int inetPort, Channel parentChannel) {
        Channel proxyClientChannel = getProxyClientChannel(inetHost, inetPort);
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
