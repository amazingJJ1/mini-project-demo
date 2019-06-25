package com.dzz.io;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;

import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author zoufeng
 * @date 2019/6/5
 */
public class MmpTest {


    /**
     * mmpbuffer测试
     *
     * @throws Exception 文件异常
     */
    @Test
    public void test1() throws Exception {
        String dir = "D:\\temp";
        RandomAccessFile memoryMappedFile;
        int size = 1 * 1024 * 1024;
        try {
            memoryMappedFile = new RandomAccessFile(dir + "/tmps.txt", "rw");
            MappedByteBuffer mappedByteBuffer = memoryMappedFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, size);
            for (int i = 0; i < 100; ) {
                mappedByteBuffer.position(i);
                int index = RandomUtils.nextInt(1, 10);
                String random = RandomStringUtils.randomAlphabetic(index);
                byte[] bytes = random.getBytes();
                mappedByteBuffer.put(bytes);
                i = i + bytes.length;
            }
            memoryMappedFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
