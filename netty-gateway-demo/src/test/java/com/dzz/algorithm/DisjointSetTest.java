package com.dzz.algorithm;

import org.junit.Test;

/**
 * 并查集算法（不相交集）
 *
 * @author zoufeng
 * @date 2019/7/8
 * <p>
 * <p>
 * 并查集的应用
 * 1.朋友圈，判断社交关系
 * 2.图的联通性判断，比如有1->2,1->3两条边，如果再加上2->3这条边的话
 * ，1，2，3这3个点的图就变成环联通了，所以在加入2->3这条边前可以判断2，3两个顶点是否在并查集的一个集合里
 */
public class DisjointSetTest {

    private int[] arr = new int[11];

    /**
     * 原理是快速合并线索，形成一颗颗子树
     * <p>
     * 有10个强盗 ，9条线索，判断其中有几个团伙
     * 10 9
     * ---
     * 1 2
     * 3 4
     * 5 2
     * 4 6
     * 2 6
     * 8 7
     * 9 7
     * 1 6
     * 2 4
     * <p>
     * 结果是三组
     */
    @Test
    public void test() {
        for (int i = 1; i < arr.length; i++) {
            arr[i] = i;
        }
        int[][] src = new int[][]{{1, 2}, {3, 4}, {5, 2}, {4, 6}, {2, 6}, {8, 7}, {9, 7}, {1, 6}, {2, 4}};
        for (int[] aSrc : src) {
            merge(aSrc[0], aSrc[1]);
        }

        int num = 0;
        for (int i = 1; i < arr.length; i++) {
            if (i == arr[i]) {
                num++;
            }
        }
        System.out.println(num);
        System.out.println();

        for (int i = 1; i < arr.length; i++) {
            arr[i] = getF(i);
            System.out.print(arr[i] + " ");
        }
    }


    private int getF(int v) {
        if (v == arr[v]) {
            return arr[v];
        } else {
            //父节点不是自己时，递归找父节点,并设置好根父节点（路径压缩）
            arr[v] = getF(arr[v]);
            return arr[v];
        }
    }

    private void merge(int x, int y) {
        //找到每个节点的根父节点
        int xf = getF(x);
        int yf = getF(y);

        //根父节点不一样时，让右边的节点强制把左边的当做父节点
        if (xf != yf) {
            arr[yf] = xf;
        }
    }
}
