package com.dzz.algorithm.string;

import org.junit.Test;

import java.util.Arrays;

/**
 * @author zoufeng
 * @date 2020-8-5
 * <p>
 * 字典树
 * <p>
 * 构造方式有两种
 * 1、tree
 * 每个节点存储下个节点的集合，一般为了保证查询0(1)的效率，这个集合一般为hashMap或者数组
 * 这样存储会极大占用空间，会有很大hashMap或者数组
 * <p>
 * 2、双数组方式
 * 双数组构建了一个DFA(有限状态自动机)
 * 双数组的原理是，将原来需要多个数组才能表示的Trie树，使用两个数据就可以存储下来，可以极大的减小空间复杂度。具体来说：
 * <p>
 * 使用两个数组base和check来维护Trie树，base负责记录状态，check负责检查各个字符串是否是从同一个状态转移而来，当check[i]为负值时，表示此状态为字符串的结束。
 * <p>
 * 上面的有点抽象，举个例子，假定两个单词ta,tb,base和check的值会满足下面的条件：
 * base[t] + a.code = base[ta]
 * base[t] + b.code = base[tb]
 * check[ta] = check[tb]
 */
public class TireTreeTest {

    //字典表 将字符映射成连续的数字
    int getIndex(char ch) {
        if (ch >= 'a' && ch <= 'z')
            return ch - 'a';
        else if (ch >= '0' && ch <= '9')
            return ch - '0';
        else
            return -1;
    }

    @Test
    public void testIndex(){
        char[] chars = "abcdefg123456".toCharArray();
        StringBuilder sb=new StringBuilder();
        for (char aChar : chars) {
            sb.append(getIndex(aChar));
        }
        System.out.println(sb);
    }

}
