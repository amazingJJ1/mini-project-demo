package com.dzz;

import org.junit.Test;

/**
 * @author zoufeng
 * @date 2019/6/3
 */
public class CacheLineTest {

    //考虑一般缓存行大小是64字节，一个 long 类型占8字节
    static long[][] arr;

    @Test
    public void cacheLineTest() {
        arr = new long[1024 * 1024][];
        for (int i = 0; i < 1024 * 1024; i++) {
            arr[i] = new long[8];
            for (int j = 0; j < 8; j++) {
                arr[i][j] = 0L;
            }
        }

        //从缓存行取数据 ，从前面的数据结构可知arr[i]填充了一行缓存行
        long sum = 0L;
        long marked = System.currentTimeMillis();
        for (int i = 0; i < 1024 * 1024; i += 1) {
            for (int j = 0; j < 8; j++) {
                sum = arr[i][j];
            }
        }
        System.out.println("Loop times:" + (System.currentTimeMillis() - marked) + "ms");

        //不从缓冲行取数据
        marked = System.currentTimeMillis();
        for (int i = 0; i < 8; i += 1) {
            for (int j = 0; j < 1024 * 1024; j++) {
                sum = arr[j][i];
            }
        }
        System.out.println("Loop times:" + (System.currentTimeMillis() - marked) + "ms");
    }
}
