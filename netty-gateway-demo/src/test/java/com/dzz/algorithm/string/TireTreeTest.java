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
 * 使用两个数组base和check来维护Trie树
 * 1、base负责记录状态
 * 2、【check负责检查各个字符串是否是从同一个状态转移而来】
 * 当check[i]为负值时，表示此状态为字符串的结束。
 * <p>
 * 举例子
 * 假设有一个字符集仅有 {a, b, c} 有单词 a, ab, bbc, bc 构建一个 DAT，首先给字符集编码 a: 1, b: 2, c: 3
 * 假设起始状态（空状态）base[state[empty]]=base[0]=0
 * base(state(a)) = base(state(empty)) + index(a) = base(0) + 1 = 1
 * base(state(ab)) = base(state(a)) + index(b) = base(1) + 2
 * 一开始我们没有设置base(state(a))任何值，先设置state(ab)=2
 * 可以反推 base(state(a))=state(ab)-index(b)=2-2=0
 * 同理
 * check(state(ab)) = state(a) = 1,
 * check[base(state(a)) + index(b)]
 * <p>
 * check举个例子，假定两个单词ta,tb,base和check的值会满足下面的条件：
 * base[t] + a.code = base[ta]
 * base[t] + b.code = base[tb]
 * check[ta] = check[tb]
 * <p>
 * <p>
 * <p>
 * 接着模拟构建一个DAT
 * 第三个单词 bbc， 第一个字母 b， state(b) 可以算出等于2...，
 * 而 2 已经被 state(ab) 占了！state(empty) 是不可能变的了，它变了 state(a), state(ab) 也会跟着变，
 * 所以考虑挪一下 state(ab)， 比如 7，玩大一点不那么容易起冲突。
 * 继续添加bb,bbc
 * <p>
 * 搜索过程
 * 现在，一个还有很多缺陷的DAT就创建好了，
 * 当需要查看 bbc 是否在字典里时分为下面几步：
 * state(b) = base(state(empty)) + index(b) = 2;
 * 检查发现 check(state(b)) = 0 等于 state(empty)，可以继续往下找 bb
 * state(bb) = base(state(b)) + index(b) = 3，检查发现 check(state(bb)) = 2 等于 state(b) 可以继续往下找 bbc
 * state(bbc) = base(state(bb)) + index(c) = 5检查发现 check(state(bbc)) = 3 等于 state(bb) 表示字典里有 bbc
 * <p>
 * 存在问题DAT的基本原理就这样，实际实现过程中会有相当多的问题，
 * 比如：
 * 1、有没有基本套路解决上面的状态冲突问题
 * 2、上面查找过程中可以得到 b 也在字典中，然而 b 并不是一个单词
 * 3、对于中文这种超大字符集应该怎样处理如果有超长的单词
 * 4、而且它的前几个字母就可以确定这个单词的了，那么还对后面的字母做索引，会影响效率，会占用多不少空间
 * 5、如何删除一个单词
 * <p>
 * 问题1：实时解决冲突效率是非常低的，不应该无序地，不定时地加入单词。
 * 一个建议的做法是首先将单词排序好，然后一层一层地去决定状态，比如有排序好的单词 a aa ab ba bba
 * 3         a
 * 2   a b a b
 * 1 a a a b b
 * 这样知道第一层有a b两个字母了，那预先占了状态1和2，到后面 aa 的时候就不会选到预先占的位置了。
 * <p>
 * 问题2：创建DAT时可添加 0x00 作为单词结尾，只有有结尾的才算完整的单词。
 * 问题3：可将中文用UTF8编码，每个字节当作一个字符。
 * 问题4：可以做一个后缀压缩的功能，详细可看 linux.thai.net
 * 问题5：可看 linux.thai.net
 *
 *
 * 原理看起来简单，实现起来还是蛮复杂的，算了，还是抄大佬的代码把
 */
public class TireTreeTest {

    //字典表 将字符映射成连续的数字
    int getIndex(char ch) {
        if (ch == endChar) return 27;
        if (ch >= 'a' && ch <= 'z')
            return (ch - 'a') + 1;
        else if (ch >= '0' && ch <= '9')
            return ch - '0';
        else
            return -1;
    }

    private Integer charsToInt(String s) {
        char[] chars = s.toCharArray();
        int temp = 1;
        int res = 0;
        for (int i = chars.length - 1; i >= 0; i--) {
            res += (getIndex(chars[i]) * temp);
            temp *= 10;
        }
        return res;
    }

    @Test
    public void testIndex() {
        char[] chars = "abcdefg123456".toCharArray();
        StringBuilder sb = new StringBuilder();
        for (char aChar : chars) {
            sb.append(getIndex(aChar));
        }
        System.out.println(sb);
    }


    @Test
    public void testDoubleArrayTrie() {
        String[] ss = {"a", "ab", "bbc", "bc"};
        int[] ssi = new int[ss.length];
        //先排序，防止添加的时候状态位冲突
        for (int i = 0; i < ss.length; i++) {
            ssi[i] = charsToInt(ss[i]);
        }
        //快排
        quickSort(0, ss.length - 1, ssi, ss);
        //添加数据，开始构造双数组trie
        //估算状态个数字典表数*单词数
        int[] base = new int[28];
        for (int i = 1; i < base.length; i++) {
            base[i] = -1;
        }
        int[] check = new int[base.length + 28];

        buildDAT(ss, base, check);
        System.out.println(Arrays.toString(base));
        System.out.println(Arrays.toString(check));

    }

    private void buildDAT(String[] strings, int[] base, int[] check) {
        for (String string : strings) {
            state(string + endChar, base, check);
        }
    }

    private int num = 0;
    private char endChar = '*';

    //state(st)=base[state(s)]+t,
    private int state(String s, int[] base, int[] check) {
        if (s.equals("")) return 0;
        String substring = s.substring(0, s.length() - 1);
        int i = base[state(substring, base, check)];
        if (i == -1) {
            num ++;
            i = base[state(substring, base, check)] = num;
        }
        int state = i + getIndex(s.charAt(s.length() - 1));
        check[state] = i;
        return state;
    }

    private void quickSort(int left, int right, int[] arr, String[] arr2) {
        if (left == right) return;
        int i = left;
        int j = right;
        while (i < j) {
            while (arr[j] >= arr[left] && j > i)
                j--;
            while (arr[i] < arr[left] && i < j)
                i++;

            int t = arr[i];
            arr[i] = arr[j];
            arr[j] = t;

            String t2 = arr2[i];
            arr2[i] = arr2[j];
            arr2[j] = t2;
        }
        //起始处归位
        int temp = arr[left];
        arr[left] = arr[i];
        arr[i] = temp;
        String temp2 = arr2[left];
        arr2[left] = arr2[i];
        arr2[i] = temp2;

        quickSort(left, i, arr, arr2);
        quickSort(i + 1, right, arr, arr2);
    }
}
