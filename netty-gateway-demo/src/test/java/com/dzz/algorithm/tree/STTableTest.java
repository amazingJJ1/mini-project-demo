package com.dzz.algorithm.tree;

import org.junit.Test;

/**
 * @author zoufeng
 * @date 2020-7-29
 * <p>
 * st表 数据结构最终是数学问题- -
 * ST表是一种解决RMQ（区间最值问题）的强有力的工具
 * 它可以做到O(nlogn)预处理，O(1)查询最值。
 * ST表其实是一种倍增的思想，我们就拿取最大值为例：
 * 目标数组是arr[],开一个二维数组Max
 * 其中Max[i][j]表示从第i位开始，包括第i位在内的2^j个数中最大的数，例如Max[i][1]表示第i个数和第i+1个数中大的那个数。
 * max[i][0] 这样其实就是arr[i]
 * <p>
 * 构建核心代码是max[i][j] = Math.max(max[i][j - 1], max[i + (1 << (j - 1))][j - 1]);
 * 原理：max[i][j]表示从i开始到i+2^j这段距离的最大值，可以分为 i->i+2^(j-1)和i+2^(j-1) +2^(j-1)两段距离，最值也是两段中的最大值
 * <p>
 * 查询的原理
 * 首先是一个定理：2^log(a)>a/2
 * 这个很简单，因为log(a)表示小于等于a的2的最大几次方。
 * 比如说log(4)=2,log(5)=2,log(6)=2,log(7)=2,log(8)=3,log(9)=3…….我们也可以使用2作为log的低
 * 那么我们要查询x到y的最大值。
 * 设len=y-x+1,t=log(len)
 * 根据上面的定理：2^t>len/2
 * 从位置上来说，x+2^t越过了x到y的中间！
 * 所以x到y的最大值可以表示为max(从x往后2^t的最大值，从y往前2^t的最大值)
 * 设后面（从y往前2^t的最大值）的初始位置是k，
 * 那么k+2^t-1=y，所以k=y-2^t+1
 * 所以后面的状态表示为max[t][y-2^t+1]
 * 所以x到y的最大值表示为max(Max[x][t],Max[y-t][y-2^t+1])，所以查询时间复杂度是O（1）
 * <p>
 * <p>
 * 优点是预处理复杂度是O(nlogn),查询复杂度只有O(1)
 * 缺点是无法修改
 */
public class STTableTest {

    private int MAXIMUM_CAPACITY = 1 << 30;

    //本质是复制移位运算，
    // 从最高bit位为1的位开始，从左边往右边复制bit位为1到相邻的bit位。相当于幂逐步+1
    private int tableSizeFor(int c) {
        int n = c - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    public int powerOfTwo(int c) {
        int i = 0;
        while (c != (1 >>> i)) {
            i++;
        }
        return i;
    }

    //获取一个数大于他的最小n次幂
    public int powerOfTwo2(int c) {
        int n = c - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        n++;
        int i = 0;
        while (n != (1 << i)) {
            i++;
        }
        return i;
    }

    @Test
    public void testST() {
        int[] arr = {2, 5, 1, 4, 9, 3};
        int m = arr.length;
        int n = powerOfTwo2(arr.length);
        int[][] max = new int[m][n];

        //预处理ST表
        for (int i = 0; i < m; i++) {
            max[i][0] = arr[i];
        }
        for (int j = 1; j < n; j++) {
            for (int i = 0; i + (1 << (j - 1)) < m; i++) {
                //i->i+2^j这段的最值分为 i->2^(j-1) 和 i+2^(j-1)->i+2^(j-1)+2^(j-1) 两段,求最值
                max[i][j] = Math.max(max[i][j - 1], max[i + (1 << (j - 1))][j - 1]);
            }
        }

        //查询2-5的最大值
        int x = 1;
        int y = 4;
        int k = (int) (Math.log(y - x) / Math.log(2));
        System.out.println(Math.max(max[x][k], max[y - (1 << k)][k]));
    }
}
