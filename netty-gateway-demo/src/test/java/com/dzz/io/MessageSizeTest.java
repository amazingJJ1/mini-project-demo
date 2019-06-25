package com.dzz.io;

import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * @author zoufeng
 * @date 2019/6/21
 */
public class MessageSizeTest {

    @Test
    public void testShotSize() {
        short[] len = new short[]{1038, 556};
        ByteBuffer buffer = ByteBuffer.allocate(4);
        for (int i = 0; i < len.length; i++) {
            buffer.putShort(len[i]);
        }
        buffer.flip();
        for (int i = 0; i < len.length; i++) {
            ByteBuffer byteBuffer = buffer.get(new byte[2]);
            System.out.println(byteBuffer);
        }

    }

    @Test
    public void testIntSize() {
        int len = 1038;
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(len);
        System.out.println(buffer);
    }
}
