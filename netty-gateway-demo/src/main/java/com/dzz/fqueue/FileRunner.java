package com.dzz.fqueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author zoufeng
 * @date 2020-7-7
 */
public class FileRunner implements Runnable {

    private final Logger log = LoggerFactory.getLogger(FileRunner.class);

    private static final Queue<String> deleteQueue = new ConcurrentLinkedQueue<String>();

    private static final Queue<String> createQueue = new ConcurrentLinkedQueue<String>();

    private String baseDir = null;

    private int fileLimitLength = 0;

    public static void addDeleteFile(String path) {
        deleteQueue.add(path);
    }

    public static void addCreateFile(String path) {
        createQueue.add(path);
    }

    public FileRunner(String baseDir, int fileLimitLength) {
        this.baseDir = baseDir;
        this.fileLimitLength = fileLimitLength;
    }

    @Override
    public void run() {
        String filePath, fileNum;
        while (true) {
            filePath = deleteQueue.poll();
            fileNum = createQueue.poll();
            if (filePath == null && fileNum == null) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
                continue;
            }
            if (filePath != null) {
                File delFile = new File(filePath);
                delFile.delete();
            }

            if (fileNum != null) {
                filePath = baseDir + fileNum + ".idb";
                try {
                    if (!create(filePath))
                        log.error("预创建数据文件失败");
                } catch (IOException e) {
                    log.error("预创建数据文件失败", e);
                }
            }
        }

    }

    private boolean create(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            if (!file.createNewFile()) {
                return false;
            }
            RandomAccessFile raFile = new RandomAccessFile(file, "rwd");
            FileChannel fc = raFile.getChannel();
            MappedByteBuffer mappedByteBuffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, this.fileLimitLength);
            mappedByteBuffer.put(LogEntity.MAGIC.getBytes());
            mappedByteBuffer.putInt(1);
            mappedByteBuffer.putInt(-1);
            mappedByteBuffer.putInt(-2);
            mappedByteBuffer.force();
            return true;
        } else {
            return false;
        }
    }
}