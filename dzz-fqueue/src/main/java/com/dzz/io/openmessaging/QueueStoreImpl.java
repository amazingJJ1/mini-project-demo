package com.dzz.io.openmessaging;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zoufeng
 * @date 2019/6/24
 */
public class QueueStoreImpl extends QueueStore {

    public static final String DIR = "/opt/dzz/fqueue/";

    private static final int FILE_NUM = Runtime.getRuntime().availableProcessors() * 2;

    private ConcurrentHashMap<String, BlockQueue>[] queueMaps;

    private BlockFile[] blockFiles;

    public QueueStoreImpl() {
        for (int i = 0; i < FILE_NUM; i++) {
            RandomAccessFile memoryMappedFile = null;
            try {
                memoryMappedFile = new RandomAccessFile(DIR + i + ".data", "rw");
                queueMaps[i] = new ConcurrentHashMap<>();
                blockFiles[i] = new BlockFile(i, memoryMappedFile.getChannel());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static Collection<byte[]> EMPTY = new ArrayList<>();

    @Override
    void put(String queueName, byte[] message) {
        int i = Math.abs(queueName.hashCode()) % FILE_NUM;
        BlockQueue queue;
        queue = queueMaps[i].get(queueName);
        if (queue == null) {
            synchronized (this) {
                // 双重检测
                queue = queueMaps[i].get(queueName);
                if (queue == null) {
                    queue = new BlockQueue(queueName, blockFiles[i]);
                    queueMaps[i].put(queueName, queue);
                }
            }
        }
        queue.put(message);
    }

    @Override
    Collection<byte[]> get(String queueName, long offset, long num) {
        return null;
    }
}
