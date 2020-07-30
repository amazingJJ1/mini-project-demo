package com.dzz.algorithm.tree;

import org.junit.Test;

/**
 * @author zoufeng
 * @date 2020-7-28
 * 线段树
 * 可以把他先简单当成一个满二叉树，对于满二叉树，包含n个叶子节点的完全二叉树，它一定有n-1个非叶节点，总共2n-1个节点
 * 满二叉树可以用一个数组arr[]表示：
 * 这里假设根节点是arr[1],它的左子节点是arr[1*2],右子节点是arr[1*2+1]
 * 递推到arr[i],它的左子节点是arr[i*2],右子节点是arr[i*2+1]
 * <p>
 * 使用场景：
 * 一般是用来解决区间最值，区间求和的问题
 * <p>
 * 比如现在有数组[2, 5, 1, 4, 9, 3]，求（m,n）区间里的和
 *              d[1] (1-6)
 *              |           \
 *      d[2](1-3)               d[3](4-6)
 *      |       \                 |      \
 *  d[4](1-2)   d[5](3-3)     d[6](4-5)   d[7](6-6)
 *    |      \                  |      \
 * d[8](1-1)  d[9](2-2)     d[12](4-4) d[13](5-5)
 *
 */
public class SegTreeTest {

    //区间和,为了方便计算，segTree[0]不参与构建，从下标1开始
    private int buildSegTree(int root, int[] segTree, int[] arr, int start, int end) {
        //分隔到了最小区间，segTree root
        if (start == end) {
            segTree[root] = arr[start];
            return arr[start];
        }
        int mid = (start + end) / 2;
        segTree[root] = buildSegTree(root * 2, segTree, arr, start, mid) + buildSegTree(root * 2 + 1, segTree, arr, mid + 1, end);
        return segTree[root];
    }

    private int query(int root, int[] segTree, int start, int end, int qStart, int qEnd) {
        //线段树区间小于查询区间，直接返回结果,或者已经是最小区间被包含在边界
        if ((qStart <= start && qEnd >= end) || (start == end && (start == qStart || start == qEnd)))
            return segTree[root];
        //不在区间内，因为是求和，直接返回0
        if (start > qEnd || end < qStart)
            return 0;
        //包含一部分区间
        int mid = (start + end) / 2;
        return query(root * 2, segTree, start, mid, qStart, qEnd) + query(root * 2 + 1, segTree, mid + 1, end, qStart, qEnd);
    }

    /*
    * 求区间和
    * */
    @Test
    public void testSumArea() {
        int[] arr = {2, 5, 1, 4, 9, 3};
        int[] segTree = new int[arr.length * 2 + 2];
        int sum = buildSegTree(1, segTree, arr, 0, 5);
        System.out.println(sum);
        int query24 = query(1, segTree, 0, 5, 2, 4);
        System.out.println(query24);
        int query13 = query(1, segTree, 0, 5, 1, 3);
        System.out.println(query13);
        int query36 = query(1, segTree, 0, 5, 3, 6);
        System.out.println(query36);
    }

    /*
    * 习题 URAL1989 Subpalindromes  https://blog.csdn.net/zearot/article/details/38921403
    * 给定一个字符串，有两个操作。1：改变某个字符。 2：判断某个区间是否构成回文串。 直接判断会超时。
    * 方法：多项式哈希+线段树。
    * 对于任意一个区间L,L+1,...,R
    * KeyL=str[ L] +str[L+1]*K  + str[L+2]* K^2  +...str[R] * K^(R-L)
    * KeyR=str[R] +str[R-1]*K  + str[R-2]* K^2  +...str[L] * K^(R-L)
    * 只要是回文串，则KeyL 与 KeyR 会相等，K为某常数。
    * 用线段树维护每个区间的KeyL 和 KeyR.
    *
    * */
    public void test(){

    }
}
