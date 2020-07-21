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
     * 单调栈,单调栈的特性是可以找到当前元素的下个大于他的元素，
     * 在当前元素这里起始就能得到以当前元素高的矩阵
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
     * */
    @Test
    public void test() {
        int[] zuZhi = {6, 4, 5, 2, 4, 3, 9};
        Stack<Integer> stack = new Stack<>();
        //单调递减栈下标
        int[] arr = new int[zuZhi.length];
        for (int i = 0; i < zuZhi.length; i++) {
            while (!stack.isEmpty() && zuZhi[i] > zuZhi[stack.peek()]) {
                arr[stack.pop()] = i;
            }
            stack.push(i);
            arr[i] = -1;//-1表示暂没有遇到比他大的数
        }
        System.out.println(Arrays.toString(arr));
    }
}
