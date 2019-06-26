package com.dzz.io.openmessaging;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
    private ByteBuffer blockBuffer;

    /**
     * block块的起始消息号(从0开始)
     */
    private long[] blockMessageNums;

    /**
     * block块的起始下标
     */
    private Future[] blockIndexs;

    /**
     * 队列当前的block块位置(从0开始)
     */
    private AtomicInteger blockNum;

    /**
     * 当前队列的消息序号
     */
    private AtomicLong messageNum;

    /**
     * 所属的物理文件
     */
    private BlockFile blockFile;

    public BlockQueue(String queueName, BlockFile blockFile) {
        this.queueName = queueName;
        this.blockFile = blockFile;
        //初始化当前块数为0,当前队列消息数为0
        blockNum = new AtomicInteger(0);
        messageNum = new AtomicLong(0);
        blockBuffer = ByteBuffer.allocateDirect(4 * 1024);
        //初始化消息和block块索引数组
        blockMessageNums = new long[16];
        //储存futrue对象，减少阻塞
        blockIndexs = new Future[16];
    }

    /**
     * todo 并发提交的问题
     *
     * @param message 字节消息
     */
    public synchronized void put(byte[] message) {

        //blockBuffer没有写入的空间则刷入pageBuffer
        if (blockBuffer.remaining() < message.length + 2) {
            flush();
        }

        int position = blockBuffer.position();
        blockBuffer.putShort((short) message.length);
        blockBuffer.put(message);
        long newMessageNum = messageNum.incrementAndGet();

        if (position == 0) {
            //消息块第一个消息记录消息号和索引
            blockMessageNums[blockNum.get()] = newMessageNum;
        }
    }

    public synchronized Collection<byte[]> get(long offset, long num) {
        //查看写缓存buffer是否有消息未落盘，先落盘

        //查找到消息所属块，获取块索引下标
        Integer index = null;
        for (int i = 1; i < blockMessageNums.length; i++) {
            if (blockMessageNums[i - 1] == offset ||
                    (blockMessageNums[i - 1] < offset && blockMessageNums[i] > offset)) {
                index = i - 1;
                break;
            }
        }
        if (index == null) {
            return QueueStoreImpl.EMPTY;
        }
        //获取

        return null;
    }

    private void flush() {
        Future<Integer> blockBufferFlushFuture = blockFile.flushBlockBufferToPageBuffer(blockBuffer);
        try {
            blockIndexs[blockNum.get()] = blockBufferFlushFuture;
            //刷盘成功后,块的下标+1
            int i = blockNum.incrementAndGet();

            //数组扩容
            if (i > blockIndexs.length * 0.75) {
                blockIndexs = Arrays.copyOf(blockIndexs, blockIndexs.length * 2);
                blockMessageNums = Arrays.copyOf(blockMessageNums, blockMessageNums.length * 2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
