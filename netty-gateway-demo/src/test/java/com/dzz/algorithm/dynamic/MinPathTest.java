package com.dzz.algorithm.dynamic;

import org.junit.Test;

import java.util.Arrays;

/**
 * @author zoufeng
 * @date 2020-8-4
 * <p>
 * 动态规划的最小路径
 */
public class MinPathTest {

    private int[][] map = {
            {1, 3, 5, 9},
            {8, 1, 3, 4},
            {5, 0, 6, 1},
            {8, 8, 4, 0}
    };

    /**
     * 给定一个矩阵，从左上角开始每次只能向右或者向下移动，
     * 最后到达右下角的位置，路径上的所有的数字累加起来作为这条路径的路劲和。
     * 要求返回所有路径和中的最小路径和。
     * <p>
     * 一，暴力解法
     * dp[i][j]表示到达i,j点的最短路径
     * dp[i][j]=min{dp[i-1][j],dp[i][j-1]}+map[i][j]}
     * dp[0][0]=1
     * <p>
     * <p>
     * 二、优化空间复杂度
     * 上面用的是二维数组，空间复杂度O(n^2)
     * 可以使用一维数组压缩
     * dp[j]表示n行的第j列最小值，下一行复用上一行的值
     * dp[j]=Min{dp[j-1],dp[j]}+map[i][j]  ,dp[j-1]表示同一行的i,j-1的最小值，dp[j]是上一行i-1,j的最小值
     */
    @Test
    public void test() {
        int[][] dp = new int[map.length][map[0].length];
        //动态规划求dp
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                if (i == 0 || j == 0)
                    dp[i][j] = map[i][j];
                else
                    dp[i][j] = Math.min(dp[i - 1][j], dp[i][j - 1]) + map[i][j];
            }
        }

        //空间压缩
        int[] dp2 = new int[map[0].length];
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                if (i == 0 || j == 0)
                    dp2[j] = map[i][j];
                else
                    dp2[j] = Math.min(dp2[j - 1], dp2[j]) + map[i][j];
            }
        }


        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                System.out.print(dp[i][j] + " ");
            }
            System.out.println();
        }

        System.out.println();
        System.out.println(Arrays.toString(dp2));
    }
}
