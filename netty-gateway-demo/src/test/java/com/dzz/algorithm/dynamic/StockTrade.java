package com.dzz.algorithm.dynamic;

import org.junit.Test;

/**
 * 股票交易，每天只能交易一次，求买入卖出利润最大的两天
 * 我们需要找出给定数组中两个数字之间的最大差值（即，最大利润）。此外，第二个数字（卖出价格）必须大于第一个数字
 *
 * @author zoufeng
 * @date 2020-1-14
 */
public class StockTrade {

    private int[] prices = {7, 1, 5, 3, 6, 4};

    @Test
    public void trade() {
        int minprice = Integer.MAX_VALUE;
        int maxprofit = 0;
        for (int i = 0; i < prices.length; i++) {
            if (prices[i] < minprice)
                minprice = prices[i];
            else if (prices[i] - minprice > maxprofit)
                maxprofit = prices[i] - minprice;
        }
        System.out.println(maxprofit);
    }

    /*
     *
     * 股票交易V，n天只允许k比交易 [买入卖出分别算一次，卖出必须在买入后]
     *
     * dp[i][k][1] 标识第i天 交易了k次手里持有股票的收益 ；
     *  dp[i][k][0] 标识第i天 交易了k次手里没有股票的收益
     * 状态转移：(买入股票的时候k+1)
     *
     * dp[i][k][0]=max(dp[i−1][k][0],dp[i−1][k][1]+prices[i])
     *  dp[i][k][1]=max(dp[i−1][k][1],dp[i−1][k-1][0]-prices[i])
     *
     * 可以看到i只和i-1天状态有关
     * 简化状态方程为
     *  dp[k][0]=max(dp[k][0],dp[k][1]+prices[i]) k次交易没持有股票的最大利润
     *  dp[k][1]=max(dp[k][1],dp[k-1][0]-prices[i]) k次交易持有股票的最大利润
     *
     *  dp 数组的初始化中，dp[0][1] 无意义，因为这里以买入股票作为开始一次交易，所以不存在 0 次交易，持有股票的情况。
     * dp[0][0]
     * dp[0][1]=-9999
     *
     *
     * 基础值
     * dp[0][0][0]=0
     * dp[0][0][1]=-99999
     * dp[0][1][0]=-99999
     * dp[0][1][1]=-prices[0]
     * dp[0][2][0]=-99999
     * dp[0][2][1]=-99999
     *
     * */
    @Test
    public void trade4() {
        int n = 1;
        int[][] dp = new int[n + 1][2];
        dp[0][0] = 0;
        dp[0][1] = -999;
        dp[1][0] = -999;
        dp[1][1] = -999;

        for (int i = 0; i < prices.length; i++) {
            for (int k = 1; k < n + 1; k++) {
                if (k > 1)
                    dp[k][0] = Math.max(dp[k][0], dp[k - 1][1] + prices[i]);
                dp[k][1] = Math.max(dp[k][1], dp[k - 1][0] - prices[i]);
            }
        }
        System.out.println(Math.max(dp[n][0], dp[n][1]));
    }
}
