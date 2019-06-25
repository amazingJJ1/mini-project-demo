package com.dzz;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

/**
 * @author zoufeng
 * @date 2019/3/26
 */
public class ClientHandler extends ChannelInboundHandlerAdapter {

    Logger logger = Logger.getLogger("ClientHandler");

    // 接收server端的消息，并打印出来
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.info("HelloClientIntHandler.channelRead");
        ByteBuf result = (ByteBuf) msg;
        byte[] result1 = new byte[result.readableBytes()];
        result.readBytes(result1);
        System.out.println("Server said:" + new String(result1));
        result.release();
    }

    // 连接成功后，保存channel
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        SocketAddress socketAddress = channel.remoteAddress();
        if (socketAddress instanceof InetSocketAddress) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
            ArrayBlockingQueue<Channel> channels = ProxyNettyClient.channelMap.get(inetSocketAddress.getHostName() + ":" + inetSocketAddress.getPort());
            if (channels == null) {
                channels = new ArrayBlockingQueue<>(10);
            }
            channels.put(channel);
        }

        System.out.println("channel active : " + socketAddress.toString());
        logger.info(channel.id().asShortText() + "is channelActive");
    }


}
