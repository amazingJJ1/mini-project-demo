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

    int prices[] = new int[]{7, 1, 5, 3, 6, 4};

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
}
