package com.dzz.algorithm.string;

import org.junit.Test;

import java.util.Arrays;

/**
 * @author zoufeng
 * @date 2020-8-8
 * <p>
 * 后缀数组
 * 首先假设我们有一个字符串 BANANA
 * 他的所有后缀字符串有： BANANA,ANANA,NANA,ANA,NA,A
 * 先定义：
 * Suffix[i] ：Str下标为i ~ Len的连续子串(即后缀)
 * Rank[i] : Suffix[i]在所有后缀中的排名
 * SA[i] : 满足Suffix[SA[1]] < Suffix[SA[2]] …… < Suffix[SA[Len]],即排名为i的后缀为Suffix[SA[i]] (与Rank是互逆运算)
 * 这里
 * suffix[0]=BANANA,suffix[1]=BANANA..suffix[5]=A
 * <p>
 * 对所有的字符串进行排序排序：
 * 使用倍增的算法：
 * 先对所有后缀的第一个字符进行排序，每个字符转换为字典序，即
 * B A N A N A
 * 2 1 3 1 3 1
 * 然后对所有后缀的字符串前两个字符排序，因为后缀数组的连续关系，
 * 这里很巧妙的对应前面一轮的相邻关系，即21代表第1个后缀字符串的前两个字符，13代表第2个后缀字符串的前两个字符
 * 21 13 31 13 31 10
 * 3  2  4  2  4  1
 * 同理接着是后缀字符串的前4个字符串排序
 * 32 24 42 24 41 10
 * 3  2  4  2  4  1
 * 前6个排序，前4个+后两个字符
 * 34 22 44 21 40 10
 * 4  3  6  2  5  1
 * 前8个排序，前4个+前4个
 * 42 35 61 20 50 10
 * 4  3  6  2  5  1
 * 因为源字符串长度都不超过8个字符串，再往下排序没有必要
 * 所有可以得到字符串后缀数组的顺序是
 * Rank[]={4,3,6,2,5,1}
 * 那么通过后缀字符串顺序数组可以得到一个映射关系数组sa[]
 * sa[1]=5，表示排序一个后缀字符串是以5下标开始的字符串，在这个示例即‘A’字符串
 * 最后得到的
 * sa[]={5,3,1,0,4,2}
 * <p>
 * 后缀数组总结
 * 核心思想是对字符串排序
 * 排序的过程中为了效率，使用的倍增算法和基数排序
 * 因为基本是两位排序，基数排序的复杂度可以做到O（len）
 * <p>
 * 后缀数组的应用
 * 一、字串最长公共前缀
 * 定义 height[i]是suffix[sa[i]]与suffix[sa[i-1]]的最长公共前缀。即相邻排名的公共前缀长度。
 * 定义H[i] : 等于Height[Rank[i]]，也就是后缀Suffix[i]和它前一名的后缀的最长公共前缀
 * 而两个排名不相邻的最长公共前缀定义为排名在它们之间的Height的最小值。
 * <p>
 * 高效地得到Height数组
 * 如果一个一个数按SA中的顺序比较的话复杂度是O(N2)级别的，想要快速的得到Height就需要用到一个关于H数组的性质。
 * H[i] ≥ H[i - 1] - 1!
 * 如果上面这个性质是对的，那我们可以按照H[1]、H[2]……H[Len]的顺序进行计算，那么复杂度就降为O(N)了！
 * 让我们尝试一下证明这个性质 : 设Suffix[k]是排在Suffix[i - 1]前一名的后缀，则它们的最长公共前缀是H[i - 1]。
 * 都去掉第一个字符，就变成Suffix[k + 1]和Suffix[i]。
 * 如果H[i - 1] = 0或1,那么H[i] ≥ 0显然成立。
 * 否则，H[i] ≥ H[i - 1] - 1(去掉了原来的第一个,其他前缀一样相等)，
 * 所以Suffix[i]和在它前一名的后缀的最长公共前缀至少是H[i - 1] - 1。
 * 仔细想想还是比较好理解的。H求出来，那Height就相应的求出来了，这样结合SA，Rank和Height我们就可以做很多关于字符串的题了！
 */
public class SuffixArrayTest {

    /*
    * 基数排序
    * 一般情况下分的桶是二维数组，用来存储相同基数的值
    * 这个在赋值和取值过程中时间和空间复杂度都较高，可以使用一维数组进行优化。
    *
    * bucket数组存储的不再是具体的值，而是符合相关基数值得个数
    * 比如测试数据data{12, 24, 23, 78, 56, 43}，先用一个临时数组temp把数据存储
    * 在第一轮个位基数分组时，只有基数2，3，4，6，8有值，且基数为3得数匹配了23和43，即
    * bucket[]={0,0,0,2,1,0,1,0,1,0}
    * 回写data得时候就可以根据bucket数组中得数量确定写入得位置
    *
    * */
    public void radixSort(int[] data, int d) {
        int rate = 1;
        int radix = 10;
        // 缓存数组
        int[] tmp = new int[data.length];
        // buckets用于记录待排序元素的信息
        // buckets数组定义了max-min个桶
        int[] buckets = new int[radix];

        for (int i = 0; i < d; i++) {
            // 重置count数组，开始统计下一个关键字
            Arrays.fill(buckets, 0);
            // 将data中的元素完全复制到tmp数组中
            System.arraycopy(data, 0, tmp, 0, data.length);

            // 计算每个待排序数据的子关键字
            for (int j = 0; j < data.length; j++) {
                int subKey = (tmp[j] / rate) % radix;
                buckets[subKey]++;
            }

            for (int j = 1; j < radix; j++) {
                buckets[j] = buckets[j] + buckets[j - 1];
            }

            // 按子关键字对指定的数据进行排序
            for (int m = data.length - 1; m >= 0; m--) {
                int subKey = (tmp[m] / rate) % radix;
                data[--buckets[subKey]] = tmp[m];
            }
            rate *= radix;
        }
    }

    @Test
    public void testRadixSort() {
        int[] arr = {12, 24, 23, 78, 56, 43};
        radixSort(arr, 2);
        System.out.println(Arrays.toString(arr));
    }

    @Test
    public void test() {

    }
}
