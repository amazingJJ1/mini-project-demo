package com.dzz;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.Attribute;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.logging.Logger;

/**
 * @author zoufeng
 * @date 2019-9-5
 */
public class ProxyClientHandler extends ChannelInboundHandlerAdapter {

    private Logger logger = Logger.getLogger(ProxyClientHandler.class.getName());

    private DefaultHttpResponse defaultHttpResponse;

    private ByteBuf body;

    private boolean keepAlive = true;
    /*boolean keepAlive = HttpUtil.isKeepAlive(request);*/

    private PooledByteBufAllocator pooledByteBufAllocator = new PooledByteBufAllocator();


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpResponse) {
            this.defaultHttpResponse = (DefaultHttpResponse) msg;
            int len = Integer.valueOf(defaultHttpResponse.headers().get(HttpHeaderNames.CONTENT_LENGTH));
            if (len > 0) {
                this.body = pooledByteBufAllocator.heapBuffer(len);
            }
        }

        if (msg instanceof HttpContent) {
            HttpContent content = (HttpContent) msg;
            this.body.writeBytes(content.content());

            if (msg instanceof LastHttpContent) {

                DefaultFullHttpResponse response = new DefaultFullHttpResponse(defaultHttpResponse.protocolVersion(), defaultHttpResponse.status(), body);
                Channel proxyChannel = ctx.channel();
                Attribute<Channel> attr = proxyChannel.attr(GwServerHandler.PARENT_CHANNEL_KEY);
                Channel parentChannel = attr.get();
                response.headers().set(defaultHttpResponse.headers());
                if (keepAlive) {
                    response.headers().remove(HttpHeaderNames.CONNECTION);
                    response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                }
                ProxyNettyClient proxyNettyClient = parentChannel.attr(GwServerHandler.Client).get();

                parentChannel.pipeline().writeAndFlush(response).addListener(new GenericFutureListener<ChannelFuture>() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        proxyNettyClient.release(proxyChannel);
                        clearProxyClientHandler();
                        logger.info("归还channel到池");
                    }
                }).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }


    private void clearProxyClientHandler() {
        body = null;
    }

}
