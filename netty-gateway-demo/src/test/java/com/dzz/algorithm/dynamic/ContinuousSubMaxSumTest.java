package com.dzz.algorithm.dynamic;

import org.junit.Test;

/**
 * @author zoufeng
 * @date 2020-8-5
 * <p>
 * 给定一个数字序列A1,A2,…, An’ 求i, j (1<=i<=j<=n), 使得Ai+···+Ai 最大，输出这个
 * 最大和。
 * <p>
 * 样例
 * 输入
 * 6
 * -2 11 -4 13 -5 -2
 * 输出
 * 20
 * <p>
 * <p>
 * 解析思路：
 * 如果j-i是固定值，使用滑动窗口或者双指针求值即可，且高效便利
 * <p>
 * 如果j-i不是固定值，采用动态规划
 * 思路一
 * a[i]是数组i的值
 * 设dp[i][j]是最大的值
 * dp[i][j]=max{dp[i-1][j]+a[i],dp[i][j-1]+a[j]}
 * 最后遍历dp[i][j]，得到最大值
 * 这样的问题是当a[]长度很长是，dp占的空间很大
 * <p>
 * 假设起始是0
 * dp[i]是到i最大值,那么i一定是大于0的值
 * 那么dp[i]=max{dp[i-1]+a[i],a[i]}
 */
public class ContinuousSubMaxSumTest {

    @Test
    public void test() {
        int[] a = {-2, 11, -4, 13, -5, -2};
        int start = 0;
        int end = 0;
        int[] dp = new int[a.length];
        int max = dp[0] = -2;
        for (int i = 1; i < a.length; i++) {
            dp[i] = Math.max(dp[i - 1] + a[i], a[i]);
            if (dp[i] > max) {
                max = dp[i];
                end = i;
                if (dp[i - 1] < 0) {
                    start = i;
                }
            }
        }
        System.out.println(String.format("起始位置下标是 ：%d %d 和是： %d ", start, end, max));
    }
}
