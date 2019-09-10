package com.dzz;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.ReferenceCountUtil;
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
                this.body = pooledByteBufAllocator.directBuffer(len);
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
                ProxyClient proxyClient = parentChannel.attr(GwServerHandler.Client).get();

                //响应体的byteBuf由tailContext自动释放
                parentChannel.pipeline().writeAndFlush(response).addListener(new GenericFutureListener<ChannelFuture>() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        proxyClient.release(proxyChannel);
                        clearProxyClientHandler();
                        logger.info("归还channel到池");
                        parentChannel.close();
                    }
                }).addListener(ChannelFutureListener.CLOSE);
            }
        }
        ReferenceCountUtil.release(msg);
    }


    private void clearProxyClientHandler() {
        body = null;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            if (((IdleStateEvent) evt).state().equals(IdleState.READER_IDLE)) {
                logger.warning(ctx.channel().id() + " 已经30秒没有读取数据了，准备关闭这个channel");
                ctx.channel().close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
