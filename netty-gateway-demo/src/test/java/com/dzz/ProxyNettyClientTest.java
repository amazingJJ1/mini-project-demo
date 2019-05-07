package com.dzz;

import io.netty.channel.Channel;
import org.junit.Test;

import java.util.Random;

/**
 * @author zoufeng
 * @date 2019/3/26
 */

public class ProxyNettyClientTest {

    @Test
    public void test() throws InterruptedException {
        ProxyNettyClient proxyNettyClient = new ProxyNettyClient();
        Channel channel = proxyNettyClient.createConnector("localhost", 8080);
        proxyNettyClient.proxyWriteMsg(channel, new Random().nextLong() + "");
        channel.closeFuture().sync();
    }

}
