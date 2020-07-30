package com.dzz.algorithm.string;

import org.junit.Test;

import java.util.Arrays;

/**
 * @author zoufeng
 * @date 2020-7-30
 * <p>
 * kmp算法
 * 字符串匹配。给你两个字符串，寻找其中一个字符串是否包含另一个字符串，如果包含，返回包含的起始位置
 * <p>
 * 主要原理是构建next函数跳过不必要的比较
 * 【next数组的含义就是一个固定字符串的最长前缀和最长后缀相同的长度】
 * <p>
 * 比如：abcjkdabc，那么这个数组的最长前缀和最长后缀相同必然是abc。
 * cbcbc，最长前缀和最长后缀相同是cbc。
 * abcbc，最长前缀和最长后缀相同是不存在的。
 * <p>
 * 这里的核心是子串的next函数，
 * 比如
 * 11111abcjkdabcz1111  //主串
 * -----abcjkdabcy---  //子串
 * -----------abcjkdabcy-----   //跳过y之前的相同前缀
 * 已匹配区域字符串abcjkdabc,在y这里失配，
 * 已经匹配的abcjkdabc这个字符串的相同的最长前缀和最长后缀是abc,那么该串可以移动3位匹配
 * <p>
 * 求字串的问题这里其实就变成了如何求next函数
 */
public class KmpTest {

    /*
     * 思路：
     * 因为next字串的前缀和后缀相等
     * 这里采用双指针方式，pre和after指针
     * pre指向第一个字符串，假设是字符串是a，after开始时指向第二个字符串
     * after指针向后移动，直到匹配到一个a字符串。
     * 默认这个字符后面的字符串和前缀字符串相等，然后循环匹配该字串
     * 失败则继续移动after指针，循环匹配
     *
     * */
    private int[] getNextArray(String pString) {
        int[] next = new int[pString.length()];
        char[] chars = pString.toCharArray();
        int len = 0;
        for (int i = 1; i < chars.length; i++) {
            int after = 1;
            while (after <= i) {
                if (chars[after] == chars[0]) {
                    len = i - after + 1;//后面比较的长度
                    boolean flag = true;
                    for (int j = 0; j < i - after + 1; j++) {
                        if (chars[j] != chars[after + j]) {
                            len = 0;
                            flag = false;
                        }
                    }
                    if (flag) break;//全中则一定是目前最长的，跳出循环
                }
                after++;
            }
            next[i] = len;
        }
        return next;
    }

    /*
     * next获取 优化思路
     * 思路二
     *
     * 核心是复用以前的结果
     * 比如 abca abcab
     * abca的最长相同串是a,长度是1
     * 后面加上b字符串和第2个字符相同，想当于abca的最长字串+1=2
     * 如果后面添加的字符串和最长公共字符串后的字符不一样，
     * 比如abcab abcaby新追加的字符是y,前面的最长公共后缀长度是2，但是y和第3个字符c不匹配
     * 需要退回到next[5]=2处开始匹配，这里开始匹配abc 和aby，还是不能匹配，继续回退到next[1]=0处开始匹配
     *
     * */
    private int[] getNext(String pString) {
        int length = pString.length();
        int[] next = new int[length];
        char[] chars = pString.toCharArray();

        for (int i = 1; i < length; i++) {
            int pre = next[i - 1];
            while ((chars[pre] != chars[i]) && pre > 0) {//回退到可能可以匹配的地方
                pre = next[pre];
            }
            if (chars[pre] == chars[i])
                next[i] = pre + 1;
        }
        return next;
    }

    private int kmp(String tString, String pString) {
        char[] tcs = tString.toCharArray();
        char[] pcs = pString.toCharArray();
        int[] next = getNext(pString);

        int i = 0; //长串的游标
        int j = 0; //匹配串的游标
        while (i < tcs.length && j < pcs.length) {
            if (tcs[i] == pcs[j]) {
                i++;
                j++;
            } else {
                if (j == 0) i++; //匹配串前面都没有匹配，主串移动下一位
                j = j > 0 ? next[j - 1] : 0;
            }
        }
        if (i > tcs.length - pcs.length)
            return -1;
        return i - pcs.length;
    }


    @Test
    public void test() {
        String tString = "bacbababadababacambabacaddababacasdsd";
        String pString = "ababaca";
        int[] next = getNextArray(pString);//暴力求next
        System.out.println(Arrays.toString(next));
        int[] next1 = getNext(pString);
        System.out.println(Arrays.toString(next1)); //复用前面next
        System.out.println(kmp(tString, pString));
    }
}
