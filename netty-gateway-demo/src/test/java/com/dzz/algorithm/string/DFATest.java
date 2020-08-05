package com.dzz.algorithm.string;

import org.junit.Test;

/**
 * @author zoufeng
 * @date 2020-8-5
 * <p>
 * DFA 全称是有限状态自动机
 * 举例说明
 * 一个仅有ab的字符串，要求b需要成对出现，否则不合法。就是(a|bb)*正则的匹配。我们可以用dfa来做这个题。
 * 我们可以通过要求生成这样一个自动机：
 * 字符串一共有3种状态，分别是没有b的状态或者b合法的状态，“a”，只有一个b的临时状态“ab”，b不匹配的“aba”状态。
 * <p>
 * 设合法状态是1，中间状态是2，非法状态是3
 * 没有输入的时候处于状态1，当输入一个a的时候还是处于状态1。
 * 当输入一个b的时候处于状态2。变成“xxxxab”
 * 当状态2再输入一个b，这是变成“abb”合法，又回到状态1.
 * 当状态2再输入一个a，这时变成了“aba”不合法状态，成为状态3
 * 状态3无论输入什么都是不合法的，都是状态3。
 * <p>
 * 这时候可以用一个数组表示这个状态机：
 * ---a   b
 * 1  1   2
 * 2  3   1
 * 3  3   3
 */
public class DFATest {

    int[][] dfa = {
            {},
            {1, 2},
            {3, 1},
            {3, 3}
    };

    @Test
    public void testDFA() {
        String string = "abbaaa";
        char[] array = string.toCharArray();
        int state = 1;//初始化是合法状态
        for (int i = 0; i < array.length; i++) {
            if (array[i] == 'a')
                state = dfa[state][0];
            if (array[i] == 'b')
                state = dfa[state][1];
            if (state == 3) break;
        }
        System.out.println(state);
    }

    /**
     * 敏感词过滤
     */
    @Test
    public void test2() {

    }

}
