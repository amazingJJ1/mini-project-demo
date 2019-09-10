package com.dzz.nio;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import org.junit.Test;

import java.net.InetSocketAddress;

/**
 * @author zoufeng
 * @date 2019-9-10
 */
public class ChannelPoolTest {

    public class DemoPoolHandler implements ChannelPoolHandler {
        @Override
        public void channelReleased(Channel ch) throws Exception {

        }

        @Override
        public void channelAcquired(Channel ch) throws Exception {

        }

        @Override
        public void channelCreated(Channel ch) throws Exception {
            System.out.println("channelCreated");
            NioSocketChannel channel = (NioSocketChannel) ch;
            channel.config().setKeepAlive(true)
                    .setTcpNoDelay(true);
            channel.pipeline().addLast(new HttpRequestEncoder())
                    .addLast(new HttpResponseDecoder())
                    .addLast(new DemoChannelOutHandler())
                    .addLast(new DemoChanelInboundHandler());
        }
    }

    public class DemoChannelOutHandler extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            System.out.println(msg);
            if (msg instanceof String) {
               /* String ms = (String) msg;
                byte[] bytes = ms.getBytes();
                ByteBuf buffer = Unpooled.buffer(bytes.length);*/
                DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
                request.headers().add("Host", "localhost:8080");
                request.headers().add("Connection", "keep-alive");
                super.write(ctx, request, promise);
            } else {
                ReferenceCountUtil.release(msg);
            }
        }
    }

    public class DemoChanelInboundHandler extends ChannelInboundHandlerAdapter {
        private byte[] body;

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof HttpContent) {
                HttpContent httpContent = (HttpContent) msg;
                ByteBuf content = httpContent.content();
                body = new byte[content.readableBytes()];
                content.readBytes(body);
                if (msg instanceof LastHttpContent) {
                    System.out.println(new String(body));
                }
            }
            super.channelRead(ctx, msg);
        }
    }

    final EventLoopGroup group = new NioEventLoopGroup();
    final Bootstrap strap = new Bootstrap();
    InetSocketAddress addr1 = new InetSocketAddress("127.0.0.1", 8080);

    private ChannelPoolMap<InetSocketAddress, SimpleChannelPool> poolMap;

    public void build() throws Exception {
        strap.group(group).channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true);

        poolMap = new AbstractChannelPoolMap<InetSocketAddress, SimpleChannelPool>() {
            @Override
            protected SimpleChannelPool newPool(InetSocketAddress key) {
                return new FixedChannelPool(strap.remoteAddress(key), new DemoPoolHandler(), 2);
            }
        };
    }


    @Test
    public void SimpleChannelPoolTest() throws Exception {
        ChannelPoolTest client = new ChannelPoolTest();
        client.build();
        final String ECHO_REQ = "Hello Netty.$_";
        for (int i = 0; i < 2; i++) {
            // depending on when you use addr1 or addr2 you will get different pools.
            final SimpleChannelPool pool = client.poolMap.get(client.addr1);
            Future<Channel> f = pool.acquire();
            f.addListener((FutureListener<Channel>) future -> {
                if (future.isSuccess()) {
                    Channel ch = future.getNow();
                    ch.writeAndFlush(ECHO_REQ);
                    // Release back to pool
                    pool.release(ch);
                }
            });
        }
        Thread.sleep(20000);

    }

}
