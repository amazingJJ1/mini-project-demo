package com.dzz;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;

import java.util.logging.Logger;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author zoufeng
 * @date 2019/3/25
 */
public class GwServerHandler extends SimpleChannelInboundHandler {

    private static final Logger logger = Logger.getLogger(GwServerHandler.class.getName());

    private PooledByteBufAllocator allocator = new PooledByteBufAllocator(true);

    private ProxyClient proxyClient;
    public static AttributeKey<Channel> PARENT_CHANNEL_KEY = AttributeKey.valueOf("parent.channel");
    public static AttributeKey<ProxyClient> Client = AttributeKey.valueOf("parent.proxynettyclient");

    public GwServerHandler(ProxyClient proxyClient) {
        super();
        this.proxyClient = proxyClient;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        try {
            super.channelReadComplete(ctx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        FullHttpRequest fullHttpRequest = null;
        //todo 这里是聚合请求发送，参考zuul2的流式代理，分请求头和请求体代理转发，提高性能，减少内存使用
        if (msg instanceof FullHttpRequest) {
            fullHttpRequest = (FullHttpRequest) msg;
        }

        Channel parentChannel = ctx.channel();
        ByteBuf content = fullHttpRequest.content();
        ByteBuf reqmsg = allocator.directBuffer(content.readableBytes());
        reqmsg.writeBytes(content);
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, fullHttpRequest.method(), fullHttpRequest.uri(), reqmsg);

        //模拟路由 省略
        logger.info("proxy host is : ");

        Channel proxyClientChannel = proxyClient.getProxyClientChannel("localhost", 8080, parentChannel);
        HttpHeaders headers = fullHttpRequest.headers();
        request.headers().add(headers);
        request.headers().remove("Host");
        request.headers().remove("Connection");
        request.headers().add("Host", "localhost:8080");
        request.headers().add("Connection", "keep-alive");

        parentChannel.attr(Client).set(proxyClient);
        proxyClientChannel.pipeline().writeAndFlush(request);
    }
}
