package com.dzz;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.spi.SelectorProvider;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 * @author zoufeng
 * @date 2019/3/26
 */

public class ProxyNettyClientTest {

    private static Logger logger = Logger.getLogger(ProxyNettyClientTest.class.getName());

    @Test
    public void test() throws InterruptedException {
        ProxyNettyClient proxyNettyClient = new ProxyNettyClient();
        Channel channel = proxyNettyClient.createConnector("localhost", 8080);
        proxyNettyClient.proxyWriteMsg(channel, new Random().nextLong() + "");
        channel.closeFuture().sync();
    }


    private Channel getChannel() throws InterruptedException {
        Bootstrap b = new Bootstrap();
        b.group(new NioEventLoopGroup(1));//NioEventLoopGroup
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, true);
//        b.option(ChannelOption.SO_TIMEOUT, this.timeout);
        b.handler(new ChannelInitializer<io.netty.channel.socket.SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                //http编码解码
                socketChannel.pipeline().addFirst(new HttpClientCodec());
                socketChannel.pipeline().addLast(new ReadHandler());
            }
        });

        // Start the client.
        ChannelFuture f = b.connect("localhost", 8080).addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                logger.info("connect ok!");
            }
        }).await();
        Channel channel = f.channel();

        return channel;
    }

    @Test
    public void testHttpRequest() throws InterruptedException {
        Channel channel = null;

        channel = new ProxyNettyClient().getProxyClientChannel("localhost", 8080);
//        channel = getChannel();

        QueryStringEncoder encoder = new QueryStringEncoder("http://localhost:8080/api/hello");
        URI uriGet = null;
        try {
            uriGet = new URI(encoder.toString());
            //System.out.println(uriGet);
        } catch (URISyntaxException e) {
        }
        DefaultFullHttpRequest defaultFullHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uriGet.getRawPath());

        defaultFullHttpRequest.headers().add("Host", "localhost:8080");
        defaultFullHttpRequest.headers().add("Connection", "keep-alive");
        channel.pipeline().writeAndFlush(defaultFullHttpRequest).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                logger.info("write ok");
            }
        });
        Thread.sleep(200000);
        channel.closeFuture().sync();
    }


    private class ReadHandler extends ChannelInboundHandlerAdapter {
        private DefaultHttpResponse response;
        private ByteBuf body;

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof HttpResponse) {
                this.response = (DefaultHttpResponse) msg;
            }

            if (msg instanceof HttpContent) {
                HttpContent content = (HttpContent) msg;
                if (this.body == null) {
                    this.body = Unpooled.buffer();
                }
                this.body.writeBytes(content.content());

                if (msg instanceof LastHttpContent) {
                    Response nr = new Response(this.response, this.body);
                    byte[] bytes = new byte[body.readableBytes()];
                    body.readBytes(bytes);
                    logger.info(new String(bytes));
                    logger.info(nr.toString());
                }
            }
        }
    }

    public class Response {
        private DefaultHttpResponse response;
        private ByteBuf body;

        public Response(DefaultHttpResponse response, ByteBuf body) {
            this.response = response;
            this.body = body;
        }


        public ByteBuf getBody() {
            return body;
        }

        public void setBody(ByteBuf body) {
            this.body = body;
        }

        public DefaultHttpResponse getResponse() {
            return response;
        }

        public void setResponse(DefaultHttpResponse response) {
            this.response = response;
        }
    }


    @Test
    public void Server() {

        EventLoopGroup bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("netty.gateway.boss", Thread.MAX_PRIORITY));
        EventLoopGroup workerGroup = new NioEventLoopGroup(2, new DefaultThreadFactory("netty.gateway.worker", Thread.MAX_PRIORITY));

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
                            ChannelPipeline p = ch.pipeline()
                                    //解码编码
                                    .addLast(new HttpRequestDecoder())
//                                    .addLast(new HttpObjectAggregator(65535))
                                    .addLast(new HelloOutHandler())
                                    .addLast(new HttpResponseEncoder() {
                                    });
                        }
                    });

            Channel ch = b.bind(9090).sync().channel();
            ch.closeFuture().sync();


        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private PooledByteBufAllocator pooledByteBufAllocator = new PooledByteBufAllocator();

    class HelloOutHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof LastHttpContent) {
                System.out.println("测试http响应");
                byte[] bytes = "hehe".getBytes();
                ByteBuf byteBuf = pooledByteBufAllocator.heapBuffer(bytes.length);
                byteBuf.writeBytes(bytes);
                System.out.println(byteBuf.readableBytes());
                DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, byteBuf);
                response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
//            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=UTF-8");
                ChannelFuture channelFuture = ctx.pipeline().writeAndFlush(response);
                channelFuture.addListener(ChannelFutureListener.CLOSE);
            }
        }

    }


}
