package com.dzz.algorithm.dynamic;

import org.junit.Test;

import java.util.Arrays;

/**
 * @author zoufeng
 * @date 2020-1-15
 * <p>
 * 俄罗斯套娃信封：
 * <p>
 * 给定一些标记了宽度和高度的信封，宽度和高度以整数对形式 (w, h) 出现。当另一个信封的宽度和高度都比这个信封大的时候，
 * 这个信封就可以放进另一个信封里，如同俄罗斯套娃一样。
 * <p>
 * 请计算最多能有多少个信封能组成一组“俄罗斯套娃”信封（即可以把一个信封放到另一个信封里面）。
 * <p>
 * 说明:
 * 不允许旋转信封。
 * <p>
 * 示例:
 * <p>
 * 输入: envelopes = [[5,4],[6,4],[6,7],[2,3]]
 * 输出: 3
 * 解释: 最多信封的个数为 3, 组合为: [2,3] => [5,4] => [6,7]。
 * <p>
 * 解题思路：
 * 先按照宽度升序，然后根据高度取最长递增序列，即可得出最多的信封数量
 */
public class Envelopes {


    @Test
    public void test() {
        int[][] arrs = {{5, 4}, {6, 4}, {6, 7}, {2, 3}};
        System.out.println(maxEnvelopes(arrs));
    }

    public int maxEnvelopes(int[][] envelopes) {

        int[] wight = new int[envelopes.length];
        int[] high = new int[envelopes.length];
        int index = 0;
        for (int[] envelope : envelopes) {
            wight[index] = envelope[0];
            high[index] = envelope[1];
            index++;
        }
        //先按宽排序,这里使用冒泡排序，减少空间复杂度
        for (int i = 0; i < wight.length; i++) {
            for (int j = 0; j < wight.length - 1 - i; j++) {
                if (wight[j] > wight[j + 1]) {
                    int tempW = wight[j];
                    wight[j] = wight[j + 1];
                    wight[j + 1] = tempW;

                    int tempH = high[j];
                    high[j] = high[j + 1];
                    high[j + 1] = tempH;
                }
            }
        }
        //再按照高找最高递增子序列 动态规划
        int[] temp = new int[high.length];
        int len = 0;
        for (int i = 0; i < high.length; i++) {
            if (len == 0 || high[i] > temp[len - 1]) {
                temp[len] = high[i];
                len++;
            } else {
                //二分查找值能替换的位置
                int index2 = Arrays.binarySearch(temp, 0, len, high[i]);
                if (index2 < 0) {
                    index2 = -(index2 + 1);
                }
                temp[index2] = high[i];
            }
        }

        return len;
    }
}
