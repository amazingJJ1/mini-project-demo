package com.dzz.algorithm.cat;

import org.junit.Test;

/**
 * @author zoufeng
 * @date 2020-10-28
 */
public class CatTest {

    /*
    * 旋转字符
    * 输入:  str="abcdefg", offset = 3
    输出:  str = "efgabcd"
    样例解释:  注意是原地旋转，即str旋转后为"efgabcd"
    * */
    @Test
    public void test() {
        char[] str = "abcdefg".toCharArray();
        int offset = 3;
        if (str.length == 0) {
            return;
        }
        offset = offset % str.length;
        for (int i = 0; i < 1; i++) {

        }
    }

    /*
     * 2 * n + 1个数字，除其中一个数字之外其他每个数字均出现两次，找到这个数字。
     *
     * 一次遍历，常数级的额外空间复杂度
     * */
    @Test
    public void test2() {
        int[] arr = {1, 1, 2, 2, 3, 4, 4};
        //异或
        int temp = arr[0];
        for (int i = 1; i < arr.length; i++) {
            temp ^= arr[i];
        }
        System.out.println(temp);
    }


}
