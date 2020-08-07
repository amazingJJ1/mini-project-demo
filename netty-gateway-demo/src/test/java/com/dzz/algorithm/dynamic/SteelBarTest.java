package com.dzz.algorithm.dynamic;

import org.junit.Test;

/**
 * @author zoufeng
 * @date 2020-8-7
 * 长度i      1   2   3  4  5    6     7   8    9  10
 * <p>
 * 价格Pi   1   5   8  9  10  17   17  20  24  30
 * <p>
 * 上图分别是长度为i的钢条的价格；那么现在一根长度为n的钢条，求如何切割，使得利润最大？
 * <p>
 * 非常套路的类背包，动态规划问题
 * 这里长度相当于物品的种类和重量，放入背包物品数量不设限
 * 我们很快可以得到状态转移方程
 * 放入第i种长度时得最大价值是,不放，放k件
 * dp[i][j]=max{dp[i-1][j],max{dp[i-1][j-k*w[i]]+k*v[i]}} 1<=kw[i]<j
 * <p>
 * 空间优化
 * 因为j需要依赖上一层得j-kw,需要逆向枚举
 * dp[j]=max{dp[j],max{dp[j-k*w[i]]+k*v[i]}} 1<=kw[i]<j
 */
public class SteelBarTest {

    int[] len = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    int[] price = {1, 5, 8, 9, 10, 17, 17, 20, 24, 30};

    @Test
    public void test() {
        int n = 25;
        int[][] dp = new int[len.length + 1][n + 1];
        for (int i = 1; i <= len.length; i++) {
            for (int j = 1; j < n + 1; j++) {
                if (len[i - 1] <= j) {
                    for (int k = 1; k <= j / len[i - 1]; k++) {
                        dp[i][j] = Math.max(dp[i - 1][j], dp[i - 1][j - (k * len[i - 1])] + k * price[i - 1]);
                    }
                } else {
                    dp[i][j] = dp[i - 1][j];
                }
            }
        }
        System.out.println(dp[len.length][n]);
    }

    //空间优化版
    @Test
    public void test2() {
        int n = 25;
        int[] dp = new int[n + 1];
        for (int i = 1; i <= len.length; i++) {
            for (int j = n; j > 0; j--) {
                if (len[i - 1] <= j) {
                    for (int k = 1; k <= j / len[i - 1]; k++) {
                        dp[j] = Math.max(dp[j], dp[j - (k * len[i - 1])] + k * price[i - 1]);
                    }
                }
            }
        }
        System.out.println(dp[n]);
    }
}
