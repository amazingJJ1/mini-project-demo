package com.dzz.io;

import org.junit.Test;

/**
 * @author zoufeng
 * @date 2019/6/19
 */
public class FileChannelTest {
    @Test
    public void test0() {
        System.out.println(0xfff);
        System.out.println(0xff);
        System.out.println(0xf);

        System.out.println("======================");
        int droppedPages = 1 >> 12;
        System.out.println(droppedPages);
        System.out.println(droppedPages << 12);

        System.out.println("======================");
        droppedPages = 16384 >> 12;
        System.out.println(droppedPages);
        System.out.println(droppedPages << 12);

        int newPosition = 1 & 0xfff;
        System.out.println(newPosition);
    }

}
