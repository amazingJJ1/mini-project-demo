package com.dzz.algorithm;

import org.junit.Test;

import java.util.Stack;

/**
 * @author zoufeng
 * @date 2019/7/9
 */
public class StackTest {

    /**
     * 题目：给定一个整型数组，数组元素随机无序的，要求打印出所有元素右边第一个大于该元素的值。
     * 没有的则打出-1
     * <p>
     * 如数组A=[1,5,3,6,4,8,9,10] 输出[5, 6, 6, 8, 8, 9, 10, -1]
     */

    private int[] arr = new int[]{1, 5, 3, 6, 4, 8, 9, 10};

    @Test
    public void stackMonotone() {
        Stack<Integer> stack = new Stack<>();
        int[] result = new int[arr.length];
        int num = 0;
        int index = 0;
        while (num < arr.length) {
            //当栈为空时或当前的数小于栈顶数的时候，直接入栈，原数组下标+1
            if (stack.isEmpty() || arr[stack.peek()] > arr[num]) {
                stack.push(num);
                num++;
            } else {
                while (!stack.isEmpty() && arr[stack.peek()] < arr[num]) {
                    stack.pop();
                    result[index] = arr[num];
                    index++;
                }
            }
        }
        //确保元素全部出栈
        while (!stack.isEmpty()) {
            stack.pop();
            result[index] = -1;
            index++;
        }

        for (int index1 : result) {
            System.out.print(index1 + " ");
        }

    }
}
