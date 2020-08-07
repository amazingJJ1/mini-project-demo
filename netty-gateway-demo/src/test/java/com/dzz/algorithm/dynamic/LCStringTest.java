package com.dzz.algorithm.dynamic;

import org.junit.Test;

/**
 * @author zoufeng
 * @date 2020-8-7
 * <p>
 * 最长公共子序列
 * 有两个字符串s1和s2求最长公共字序列
 * dp[i][i]标识s1和s2的最长公共子序列
 * if s1[i]=s2[j] ,dp[i][j]=dp[i-1][j-1]+1
 * if s1[i]!=s2[j],dp[i][j]=max{dp[i-1][j],dp[i][j-1]}
 * <p>
 * 最长公共字串
 * dp表示s1 0-i和s2 0-j的最大公共字串
 * if s1[i]=s2[j] ,dp[i][j]=dp[i-1][j-1]+1
 * if s1[i]!=s2[j],dp[i][j]=0，和最大连续序列不同之处
 * <p>
 * <p>
 */
public class LCStringTest {

    //X = {a, c, b, b}; Y = {a, b, b, d, f}那么，{a, b, b}是X和Y的最长公共子序列，但不是它们的最长公共字串。
    @Test
    public void testLis() {
        char[] s1 = {'a', 'c', 'b', 'b'};
        char[] s2 = {'a', 'b', 'b', 'd', 'f'};
        int[][] dp = new int[s1.length + 1][s2.length + 1];//dp[0][],dp[][0]=0
        for (int i = 1; i < s1.length + 1; i++) {
            for (int j = 1; j < s2.length + 1; j++) {
                if (s1[i - 1] == s2[j - 1])
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        System.out.println(dp[4][4]);
    }

    @Test
    public void testLCS() {
        char[] s1 = {'a', 'c', 'b', 'b'};
        char[] s2 = {'a', 'b', 'b', 'd', 'f'};
        int[][] dp = new int[s1.length + 1][s2.length + 1];//dp[0][],dp[][0]=0
        for (int i = 1; i < s1.length + 1; i++) {
            for (int j = 1; j < s2.length + 1; j++) {
                if (s1[i - 1] == s2[j - 1])
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                else {
                    dp[i][j] = 0;
                }
            }
        }
        int max = 0;
        int maxI = 0;
        int maxJ = 0;
        for (int i = 0; i < dp.length; i++) {
            for (int j = 0; j < dp[i].length; j++) {
                if (dp[i][j] > max) {
                    max = dp[i][j];
                    maxI = i;
                    maxJ = j;
                }
                System.out.print(dp[i][j] + " ");
            }
            System.out.println();
        }

        System.out.println(String.format("最长公共字串起始是： s1(%d,%d),s2(%d,%d)", maxI - max, maxI - 1, maxJ - max, maxJ - 1));
    }
}
