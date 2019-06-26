package com.dzz.io.openmessaging;

import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zoufeng
 * @date 2019/6/25
 */
public class BlockFile {

    public static final int PAGE_SIZE = 64 * 1024;

    private int file_suffix;

    public static final byte zeroByte = (byte) 0;

    private ByteBuffer pageBuffer = ByteBuffer.allocateDirect(PAGE_SIZE);

    private ThreadPoolExecutor flushThread = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "flush-blockbuffer-Thread=" + file_suffix);
        }
    });

    /**
     * 当前写入的页码
     */
    private AtomicInteger pageNum = new AtomicInteger(0);

    /**
     * 当前页的写入下标
     */
    public AtomicInteger fileWrotePosition = new AtomicInteger(0);

    private FileChannel channel;

    public BlockFile(int i, FileChannel channel) {
        this.file_suffix = i;
        this.channel = channel;
    }

    public Future<Integer> flushBlockBufferToPageBuffer(ByteBuffer blockBuffer) {
        //未写完的填0值补齐
        while (blockBuffer.remaining() > 0) {
            blockBuffer.put(zeroByte);
        }
        blockBuffer.flip();
        byte[] bytes = new byte[blockBuffer.remaining()];
        blockBuffer.get(bytes);
        blockBuffer.clear();

        return flushThread.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {

                if (pageBuffer.remaining() - bytes.length <= 0) {
                    pageBuffer.flip();
                    //写入pageCache,系统异步刷盘
                    channel.write(pageBuffer);
                    pageBuffer.clear();
                    int i = pageNum.incrementAndGet();
                    //设置block的下标为新页的初始下标
                    return i * PAGE_SIZE - 1;
                }
                //pageBuffer不满时，block下标为写入前的下标
                int position = pageBuffer.position();
                if (position == 0) {
                    return 0;
                }

                pageBuffer.put(bytes);
                return pageNum.get() * PAGE_SIZE - 1 + position;
            }
        });
    }
}
