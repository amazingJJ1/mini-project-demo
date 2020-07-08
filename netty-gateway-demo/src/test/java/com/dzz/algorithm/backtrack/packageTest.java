package com.dzz.algorithm.backtrack;

import org.junit.Test;

/**
 * @author zoufeng
 * @date 2020-7-3
 * <p>
 * 回溯的算法，核心就是回这个点
 * 1.当前节点，获取可执行列表
 * 2.可执行列表节点复合条件则进行子节点递归遍历
 * 3.回到当前节点状态
 */
public class packageTest {

    /*
     * 回溯法解01背包
     * */
    @Test
    public void testPackage() {

    }

    /**
     * 八婆
     */
    @Test
    public void eq() {
        int[][] arr = new int[8][8];
        eight(arr, 0);
        System.out.println(eNum);
    }

    private int eNum = 0;

    private void printArr(int[][] arr) {
        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[i].length; j++) {
                System.out.print(arr[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

    private void eight(int[][] arr, int line) {
        if (line == arr.length) {
            eNum++;
            printArr(arr);
            return;
        }

        for (int i = 0; i < arr[line].length; i++) {
            if (isEOk(arr, line, i)) {
                arr[line][i] = 1;
                eight(arr, line + 1);
                arr[line][i] = 0;
            }
        }
    }

    private boolean isEOk(int[][] arr, int line, int i) {
        int num = 1;
        while (line >= num) {
            if (arr[line - num][i] == 1
                    || (i - num >= 0 && arr[line - num][i - num] == 1)
                    || (i + num < arr[line - num].length && arr[line - num][i + num] == 1)) {
                return false;
            }
            num++;
        }
        return true;
    }


    @Test
    public void testFullPermutations() {
        int[] arr = {1, 2, 3};
        fullPermutations(arr, 0);
    }

    private void print(int[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i : arr) {
            sb.append(i);
        }
        System.out.println(sb.toString());
    }

    private void swap(int[] arr, int from, int to) {
        int temp = arr[from];
        arr[from] = arr[to];
        arr[to] = temp;
    }

    private void fullPermutations(int[] arr, int index) {
        if (index == arr.length)
            print(arr);
        for (int i = index; i < arr.length; i++) {//从index开始，子序列继续递归
            swap(arr, index, i);
            fullPermutations(arr, index + 1);
            swap(arr, i, index);
        }
    }
}
