package com.dzz.thread;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zoufeng
 * @date 2019-9-27
 */
public class SimpleThreadTest {

    /**
     * 交替打印数组
     */
    @Test
    public void test() throws InterruptedException {
        Object lock = new Object();
        int[] arr = new int[100];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = i + 1;
        }

        new Thread(() -> {
            for (int value : arr) {
                synchronized (lock) {
                    if (value % 2 == 0) {
                        System.out.println(value);
                        lock.notify();
                    } else {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
        new Thread(() -> {
            for (int value : arr) {
                synchronized (lock) {
                    if (value % 2 != 0) {
                        System.out.println(value);
                        lock.notify();
                    } else {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();

        Thread.sleep(1000);
    }


    @Test
    public void test2() throws InterruptedException {
        AtomicInteger integer = new AtomicInteger(1);
        new Thread(() -> {
            while (integer.get() < 101) {
                if (integer.get() % 2 == 0 && integer.get() < 101) {
                    System.out.println(integer.get());
                    integer.incrementAndGet();
                }
            }
        }).start();
        new Thread(() -> {
            while (integer.get() < 101) {
                if (integer.get() % 2 != 0 && integer.get() < 101) {
                    System.out.println(integer.get());
                    integer.incrementAndGet();
                }
            }
        }).start();

        while (integer.get() < 101) {
        }
    }


    private double add(String s1, String s2) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        s1 = s1.trim();
        s2 = s2.trim();
        return new BigDecimal(s1).add(new BigDecimal(s2)).doubleValue();
    }

    @Test
    public void test3() {
        double add = add("1", "-0.123");
        System.out.println(add);
        add = add("0", "-0.123");
        System.out.println(add);
        add = add("-0.0000000000001", "-0.123");
        System.out.println(add);
        add = add("1000", "250");
        System.out.println(add);
    }
}
