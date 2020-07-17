package com.dzz.io;

import org.junit.Test;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

/**
 * @author zoufeng
 * @date 2020-7-17
 */
public class FileLockTest {

    @Test
    public void testLock() throws IOException, InterruptedException {
        File file = new File("D:\\temp\\filelock.txt");

        //给该文件加锁
        RandomAccessFile fis = new RandomAccessFile(file, "rw");
        FileChannel channel = fis.getChannel();
        FileLock lock = null;
        try {
            lock = channel.tryLock();
            System.out.println("锁是否共享：" + lock.isShared());
            System.out.println("获取锁开始读文件");
        } catch (Exception e) {
            System.out.println("有其他线程正在操作该文件，当前线程休眠1000毫秒");
            sleep(1000);
        }
        readFile(fis);

        new Thread(() -> {
            while (true) {
                try {
                    readFile(fis);
                    System.out.println(Thread.currentThread().getName() + "读取成功，休眠1s");
                    TimeUnit.SECONDS.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        System.out.println(Thread.currentThread().getName() + "都取失败，休眠1s");
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }, "线程2").start();

        TimeUnit.SECONDS.sleep(10);

        System.out.println("休眠完毕，释放锁");
        if (lock != null)
            lock.release();
        System.out.println("锁释放完成");

        Thread.sleep(Integer.MAX_VALUE);

    }

    private void readFile(RandomAccessFile fis) throws IOException {
        byte[] buf = new byte[1024];
        StringBuilder sb = new StringBuilder();
        while ((fis.read(buf)) != -1) {
            sb.append(new String(buf, "utf-8"));
            buf = new byte[1024];
        }
        System.out.println(Thread.currentThread().getName() + " : " + sb);
    }
}
