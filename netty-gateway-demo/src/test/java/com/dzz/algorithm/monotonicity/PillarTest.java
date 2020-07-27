package com.dzz.algorithm.monotonicity;

import org.junit.Test;

import java.util.Arrays;
import java.util.Stack;

/**
 * @author zoufeng
 * @date 2020-7-21
 */
public class PillarTest {

    /*
     * 有一排柱子，高度为[6, 4, 5, 2, 4, 3, 9]
     * 求柱子能构成的最大矩阵
     *
     * 思路：
     * 使用单调栈,单调栈的特性是可以找到当前元素的后面第一个大于或者小于他的元素，
     *
     * 不过这里需要知道左边和右边的边界
     *
     * 这里我们先求一个简单的单调栈
     * 柱子：[6, 4, 5, 2, 4, 3, 9]，记录索引的数组：[0,0,0,0,0,0,0]
     * 构建一个单调递减的单调栈,放入的元素如果比栈顶小才入栈，比栈顶大则取出栈顶的元素，直到栈顶元素比放入的元素大
     * 【每次栈的元素取出时，表示遇到了比他大的元素加入的栈】 栈放入的元素是柱子的索引
     * 放入第一个元素时，栈为空，放入栈顶
     *
     *
     * 这里应该用单调递增栈，找到后面第一个比他小的元素
     * */
    @Test
    public void test() {
        int[] zuZhi = {6, 4, 5, 2, 4, 3, 9};
        Stack<Integer> stack = new Stack<>();
        //单调递增栈下标
        int[] right = new int[zuZhi.length];
        int[] left = new int[zuZhi.length];
        for (int i = 0; i < zuZhi.length; i++) {
            while (!stack.isEmpty() && zuZhi[i] < zuZhi[stack.peek()]) {
                right[stack.pop()] = i;
            }
            stack.push(i);
            right[i] = -1;//-1表示暂没有遇到比ta小的数
        }

        stack = new Stack<>();
        for (int i = zuZhi.length - 1; i >= 0; i--) {
            while (!stack.isEmpty() && zuZhi[i] < zuZhi[stack.peek()]) {
                left[stack.pop()] = i;
            }
            stack.push(i);
            left[i] = -1;//-1表示暂没有遇到比他小的数
        }
        System.out.println(Arrays.toString(left));
        System.out.println(Arrays.toString(right));

        //根据左右边界 求出矩阵大小
        int max = 0;
        for (int i = 0; i < zuZhi.length; i++) {
            int l = left[i] == -1 ? 0 : left[i] + 1;
            int r = right[i] == -1 ? zuZhi.length : right[i];
            max = Math.max(max, (r - l) * zuZhi[i]);
        }
        System.out.println(max);
    }

    /*
     * 优化思路
     * 我们计算矩形面积的时候用到了两个单调栈，
     * 分别计算了某一个高度向左、向右能够延伸到的最远距离，其实这并没有必要。
     * 因为我们用一个栈也可以同时计算出两边的边界。
     * 举个例子：[1, 3, 6, 7]，当前元素是5。我们需要把6，7出栈，5入栈。
     * 我们知道了5的左边界是3，但仔细想一想，对于7来说，我们知道了它的左右边界。7的左边界是6，右边界是5。
     * 也就是说对于栈顶的元素而言，它的左边界是stack[top-1]，右边界是当前的位置i，宽就是i - stack[top-1] - 1。
     * */
    @Test
    public void test2() {
        int[] zuZhi = {6, 4, 5, 2, 4, 3, 9};
        Stack<Integer> stack = new Stack<>();
        //单调递增栈下标
        int[] right = new int[zuZhi.length];
        int[] left = new int[zuZhi.length];
        for (int i = 0; i < zuZhi.length; i++) {
            while (!stack.isEmpty() && zuZhi[i] < zuZhi[stack.peek()]) {
                right[stack.pop()] = i;
            }
            if (stack.isEmpty()) {
                left[i] = -1;
            } else {
                left[i] = stack.peek();
            }

            stack.push(i);
            right[i] = -1;//-1表示暂没有遇到比ta小的数

        }


        //根据左右边界 求出矩阵大小
        int max = 0;
        for (int i = 0; i < zuZhi.length; i++) {
            int l = left[i] == -1 ? 0 : left[i] + 1;
            int r = right[i] == -1 ? zuZhi.length : right[i];
            max = Math.max(max, (r - l) * zuZhi[i]);
        }
        System.out.println(max);
    }

    /*
     * 进阶
     * 求包含1的最大矩阵
     *
     * 让每一行递增构建直方图，连续的1即为直方图的柱子高度
     * */
    @Test
    public void testMax() {
        int[][] arr = {
                {1, 0, 1, 0, 0},
                {1, 0, 1, 1, 1},
                {1, 1, 1, 1, 1},
                {1, 0, 0, 1, 0}
        };

        int n = arr.length;
        int m = arr[0].length;
        int[][] height = new int[n][m];
        int max = 0;
        Stack<Integer> stack = new Stack<>();

        for (int i = 0; i < n; i++) {
            //每一行的直方图
            for (int j = 0; j < m; j++) {
                //预处理
                if (i == 0) {
                    height[i][j] = arr[i][j];
                } else {
                    height[i][j] = arr[i][j] == 0 ? 0 : height[i - 1][j] + 1;
                }
            }

            int[] right = new int[m];
            int[] left = new int[m];
            //每一行的最大矩阵
            for (int j = 0; j < m; j++) {
                while (!stack.isEmpty() && height[i][j] < height[i][stack.peek()]) {
                    right[stack.pop()] = j;
                }
                if (stack.isEmpty()) {
                    left[j] = -1;
                } else {
                    left[j] = stack.peek();
                }
                stack.push(j);
                right[j] = -1;
            }

            for (int j = 0; j < m; j++) {
                int l = left[j] == -1 ? 0 : left[j] + 1;
                int r = right[j] == -1 ? m : right[j];
                max = Math.max(max, (r - l) * height[i][j]);
            }
        }

        System.out.println(max);

    }
}
