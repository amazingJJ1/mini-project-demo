package com.dzz;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.rtsp.RtspResponseStatuses.BAD_REQUEST;

/**
 * @author zoufeng
 * @date 2019/3/25
 */
public class GwServerHandler extends SimpleChannelInboundHandler {

    private HttpRequest request;
    private StringBuilder buffer = new StringBuilder();
    private String url = "";
    private String uri = "";
    private StringBuilder respone;
    private GlobalEventExecutor executor = GlobalEventExecutor.INSTANCE;
    private CountDownLatch latch = new CountDownLatch(1);

    private static final Logger logger = Logger.getLogger(GwServerHandler.class.getName());

    PooledByteBufAllocator allocator = new PooledByteBufAllocator(false);

    private ProxyNettyClient proxyNettyClient;
    public static AttributeKey<Channel> PARENT_CHANNEL_KEY = AttributeKey.valueOf("parent.channel");
    public static AttributeKey<ProxyNettyClient> Client = AttributeKey.valueOf("parent.proxynettyclient");

    public GwServerHandler(ProxyNettyClient proxyNettyClient) {
        super();
        this.proxyNettyClient = proxyNettyClient;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        try {
            super.channelReadComplete(ctx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*ByteBuf buf= (ByteBuf) msg;
       System.out.println(msg);
       ByteBuf reqmsg = allocator.heapBuffer(buf.readableBytes());
       Channel parentChannel = ctx.channel();
       reqmsg.writeBytes(buf);
       ctx.writeAndFlush(reqmsg);*/
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        //使用了HttpObjectAggregator 会将httpHeader和HttpContent合并成一个FullHttpRequest
        FullHttpRequest fullHttpRequest = null;
        if (msg instanceof FullHttpRequest) {
            fullHttpRequest = (FullHttpRequest) msg;
        }
        Channel parentChannel = ctx.channel();
        ByteBuf content = fullHttpRequest.content();
        ByteBuf reqmsg = allocator.heapBuffer(content.readableBytes());
        reqmsg.writeBytes(content);
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, fullHttpRequest.method(), fullHttpRequest.uri(), reqmsg);
        //模拟路由 省略
        Channel proxyClientChannel = proxyNettyClient.getProxyClientChannel("localhost", 8080, parentChannel);
        HttpHeaders headers = fullHttpRequest.headers();
        request.headers().add(headers);
        request.headers().remove("Host");
        request.headers().remove("Connection");
        request.headers().add("Host", "localhost:8080");
        request.headers().add("Connection", "keep-alive");

        parentChannel.attr(Client).set(proxyNettyClient);
        proxyClientChannel.pipeline().writeAndFlush(request);
    }

    private void writeResponse(StringBuilder respone, HttpObject current, ChannelHandlerContext ctx) {
        if (respone != null) {
            boolean keepAlive = HttpUtil.isKeepAlive(request);

            FullHttpResponse response = new DefaultFullHttpResponse(
                    HTTP_1_1, current == null ? OK : current.decoderResult().isSuccess() ? OK : BAD_REQUEST,
                    Unpooled.copiedBuffer(respone.toString(), Charset.forName("UTF-8")));

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=GBK");

            if (keepAlive) {
                response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }

            ctx.write(response);
        }
    }

    private static void notify100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
        ctx.write(response);
    }


    private void invoke(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            HttpRequest request = this.request = (HttpRequest) msg;
            //http 100-continue用于客户端在发送POST数据给服务器前，征询服务器情况，
            // 看服务器是否处理POST的数据，如果不处理，客户端则不上传POST数据，
            // 如果处理，则POST上传数据。在现实应用中，通常在POST大数据时，才会使用100-continue协议
            if (HttpUtil.is100ContinueExpected(request)) {
                notify100Continue(ctx);
            }

            buffer.setLength(0);
            uri = request.uri().substring(1);
        }

        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
            ByteBuf content = httpContent.content();
            if (content.isReadable()) {
                buffer.append(content.toString(Charset.forName("UTF-8")));
            }
            if (msg instanceof LastHttpContent) {
                LastHttpContent trace = (LastHttpContent) msg;
                System.out.println("[NETTY-GATEWAY] REQUEST : " + buffer.toString());
            }
        }
    }
}
