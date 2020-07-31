package com.dzz.algorithm;

import org.junit.Test;

/**
 * @author zoufeng
 * @date 2019/7/9
 */
public class QuickSortTest {

    private int[] arr = new int[]{4, 6, 7, 1, 2, 3, 9, 11, 13};

    private void quickSort(int left, int right) {
        if (left > right) return;
        int temp = arr[left];
        int i = left;
        int j = right;
        while (i != j) {
            //右边往左边走，直到数字比基准数小
            while (arr[j] >= temp && i < j) {
                j--;
            }
            //左边往右边走，直到数字比基准数大
            while (arr[i] <= temp && i < j) {
                i++;
            }
            //交换数据
            if (i < j) {
                int t = arr[i];
                arr[i] = arr[j];
                arr[j] = t;
            }
        }

        //最终基数归位
        arr[left] = arr[i];
        arr[i] = temp;

        quickSort(left, i - 1);//继续处理左边的，这里是一个递归的过程
        quickSort(i + 1, right);//继续处理右边的 ，这里是一个递归的过程
    }

    @Test
    public void testQs() {
        quickSort(0, arr.length - 1);
        for (int i = 0; i < arr.length; i++) {
            System.out.print(arr[i] + " ");
        }
    }
}
