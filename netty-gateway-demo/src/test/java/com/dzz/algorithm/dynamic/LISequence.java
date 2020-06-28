package com.dzz.algorithm.dynamic;

import org.junit.Test;

import java.util.Arrays;

/**
 * @author zoufeng
 * @date 2020-1-15
 * <p>
 * 求最长连续子序列
 * <p>
 * 比如（1，5 ，2，6，9，10，3，15）
 * 那么它的最长上升子序列为：（1，2，6，9，10，15）
 */
public class LISequence {

    private int[] arr = new int[]{1, 5, 2, 6, 9, 10, 3, 15};

    @Test
    public void test() {
        int i = lengthOfLIS(arr);
        System.out.println(i);

        int dp = dp(new int[]{0, 8, 4, 12, 2});
        System.out.println(dp);
    }


    public int lengthOfLIS(int[] nums) {
        return lengthofLIS(nums, Integer.MIN_VALUE, 0);
    }

    //递归的方式暴力计算
    public int lengthofLIS(int[] nums, int prev, int curpos) {
        if (curpos == nums.length) {
            return 0;
        }
        int taken = 0;
        //比前一个值大，前面的序列可以包含当前值
        if (nums[curpos] > prev) {
            taken = 1 + lengthofLIS(nums, nums[curpos], curpos + 1);
        }
        int nottaken = lengthofLIS(nums, prev, curpos + 1);
        return Math.max(taken, nottaken);
    }

    /**
     * 动态规划+二分搜索 简化计算
     * input: [0, 8, 4, 12, 2]
     * dp: [0]
     * dp: [0, 8]
     * dp: [0, 4]
     * dp: [0, 4, 12]
     * dp: [0, 2, 12]
     * 虽然[0, 2, 12]不是最终需要的序列，但是不影响最终的长度
     */
    public int dp(int[] arr) {
        int[] temp = new int[arr.length];
        int len = 0;
        for (int i = 0; i < arr.length; i++) {
            if (len == 0 || arr[i] > temp[len-1]) {
                temp[len] = arr[i];
                len++;
            } else {
                //二分查找值能替换的位置
                int index = Arrays.binarySearch(temp, 0, len, arr[i]);
                if (index < 0) {
                    index = -(index + 1);
                }
                temp[index] = arr[i];
            }
        }
        return len;
    }
}
