package com.dzz.io.openmessaging;

import java.nio.ByteBuffer;
import java.util.Collection;

/**
 * @author zoufeng
 * @date 2019/6/24
 */
public class BlockQueue {

    private String queueName;

    private final static short MESSAGE_LENGTH = 2;

    /**
     * block块写入缓冲区
     */
    private ByteBuffer blockBuffer = ByteBuffer.allocateDirect(4 * 1024);

    /**
     * block块的起始消息号
     */
    private int[] blockMessageNum;

    /**
     * block块的起始下标
     */
    private int[] blockIndex;

    /**
     * 所属的物理文件
     */
    private BlockFile blockFile;

    public BlockQueue(String queueName, BlockFile blockFile) {
        this.queueName = queueName;
        this.blockFile = blockFile;
    }

    public void put(byte[] message) {
        if (blockBuffer.remaining() < message.length + 2) {
            flush();
        }
    }

    public synchronized Collection<byte[]> get(long offset, long num) {
        return null;
    }

    private void flush() {

    }
}
