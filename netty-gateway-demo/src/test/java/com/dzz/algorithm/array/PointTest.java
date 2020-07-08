package com.dzz.algorithm.array;

import org.junit.Test;

/**
 * @author zoufeng
 * @date 2020-7-7
 */
public class PointTest {

    /**
     * 接雨水
     * 宽度为1，高度为数组的柱子，求收集的雨水量
     * <p>
     * 分解成求每个柱子可以接的雨水量
     * <p>
     * 双数组求作用最高的柱子
     */
    @Test
    public void collectRain() {
        int[] arr = {0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1};
        int[] leftMax = new int[arr.length];
        int[] rightMax = new int[arr.length];
        for (int i = 1; i < arr.length; i++) {
            leftMax[i] = Math.max(arr[i - 1], leftMax[i - 1]);
        }
        for (int i = arr.length - 2; i > -1; i--) {
            rightMax[i] = Math.max(arr[i + 1], rightMax[i + 1]);
        }
        int result = 0;
        for (int i = 1; i < arr.length - 1; i++) {
            int min = Math.min(leftMax[i], rightMax[i]);
            if (arr[i] < min) result += min - arr[i];
        }

        System.out.println(result);
    }


    /*
     * 双指针求最大接受量
     *
     * 两个指针，
     * left代表左边（0，left）最高的柱子
     * right代表右边(right,length)最高的柱子
     *
     * 边走边算
     * 因为接雨水的量只以最短的边为准
     * */
    @Test
    public void collectRain2() {
        int[] arr = {0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1};
        int leftMax = arr[0], rightMax = arr[arr.length - 1], result = 0;
        int left = 1, right = arr.length - 2;
        while (left < right) {
            leftMax = Math.max(arr[left], leftMax);
            rightMax = Math.max(arr[right], rightMax);

            if (leftMax >= rightMax) {
                result += rightMax - arr[right];
                right--;
            }

            if (leftMax < rightMax) {
                result += leftMax - arr[left];
                left++;
            }
        }

        System.out.println(result);
    }

    /*
     * 有一个1-N的数组，无序，一个数字重复，同时这样会导致一个数字缺失，求这俩个数字
     *
     * 使用映射的方法，循环数组，比如第一个值是2，将数组第2个值设置为负数，表示这个值有对应位置的元素
     * 如果2是重复值，肯定会再次找到数值第二个值，发现该值是负数
     * 而缺失的值肯定在数值中找不到相应位置，最终相应位置的元素一定是正数
     *
     * 一般求重复数字有这么几种方案，异或，映射
     * a^a=0 ,a^0=a
     * */
    @Test
    public void findOutNum() {
        int[] arr = {3, 5, 5, 6, 9, 2, 8, 7, 1};
        int dup = -1;
        int miss = -1;
        for (int i = 0; i < arr.length; i++) {
            int index = arr[i] > 0 ? arr[i] : -arr[i];
            if (arr[index - 1] > 0) {
                arr[index - 1] = -arr[index - 1];
            } else {
                dup = index;
            }
        }
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] > 0) miss = i + 1;
        }
        System.out.println(dup);
        System.out.println(miss);
    }

    /*
     *有序数组
     * 移除重复元素
     *
     * 单指针交换
     * */
    @Test
    public void removeDup() {
        int[] arr = {0, 0, 1, 1, 1, 2, 2, 3, 3, 4};
        int left = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > arr[left]) {
                left++;
                arr[left] = arr[i];
            }
        }
        System.out.println(left);
        for (int i = 0; i <= left; i++) {
            System.out.print(arr[i]);
        }
    }

}
