package com.dzz.algorithm.string;

import org.junit.Test;

import static java.lang.Math.max;

/**
 * @author zoufeng
 * @date 2019/7/9
 */
public class ByteDanceStringTest {

    /**
     * 求最长字符串子串的
     * <p>
     * 使用滑动窗口的思想,记录滑动窗口的起始和结束位置（left,right）
     * hash表记录字符上一次出现的位置 这里用一个256的整型数组充当hash表【256可以表示所有字符】
     *
     * @param s
     * @return
     */
    public int lengthOfLongestSubstring(String s) {
        char[] chars = s.toCharArray();
        int[] map = new int[256];
        int res = 0;
        int left = 0;
        for (int i = 0; i < s.length(); ++i) {
            //字符没有出现过或者出现的位置在滑动窗口前，更新子串长度[滑动窗口长度]
            if (map[chars[i]] == 0 || map[chars[i]] < left) {
                res = max(res, i - left + 1);
            } else {
                //记录最新的字符串位置作为左边界，如果她后面比以前的res记录长则后续替换
                left = map[chars[i]];
            }
            //记录字符出现的位置
            map[chars[i]] = i + 1;
        }
        return res;
    }

    @Test
    public void test() {
        int i = lengthOfLongestSubstring("agogewjgoewjoa[wig0igkaewfwegewakjgaw;lke");
        System.out.println(i);
    }

}
