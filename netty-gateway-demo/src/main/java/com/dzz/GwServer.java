package com.dzz;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.nio.channels.spi.SelectorProvider;

/**
 * @author zoufeng
 * @date 2019/3/25
 */
public class GwServer {

    public static void main(String[] args) {

        EventLoopGroup bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("netty.gateway.boss", Thread.MAX_PRIORITY));
        EventLoopGroup workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2, new DefaultThreadFactory("netty.gateway.worker", Thread.MAX_PRIORITY));

        ProxyClient proxyClient = new ProxyClient();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channelFactory(new ChannelFactory<NioServerSocketChannel>() {
                        @Override
                        public NioServerSocketChannel newChannel() {
                            return new NioServerSocketChannel(SelectorProvider.provider());
                        }
                    })
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new HttpRequestDecoder());
                            //聚合
                            p.addLast(new HttpObjectAggregator(65536));
                            //支持chunk流编码 用于文件传输
                            // p.addLast("http-chunked", new ChunkedWriteHandler());
                            p.addLast(new HttpResponseEncoder());
                            p.addLast(new GwServerHandler(proxyClient));
                        }
                    });

            Channel ch = b.bind(9090).sync().channel();
            ch.closeFuture().sync();


        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            proxyClient.stop();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }
}
