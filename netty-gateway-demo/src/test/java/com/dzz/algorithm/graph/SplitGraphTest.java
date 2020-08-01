package com.dzz.algorithm.graph;

import org.junit.Test;

import java.util.Arrays;

/**
 * @author zoufeng
 * @date 2020-8-1
 * <p>
 * 图的割点
 * <p>
 * 对图进行一次深度遍历，构造成一棵树，
 * 记录每个节点的时间戳（先后顺序）为num[]，设每个子节点能访问的最小时间戳数组low[],
 * 树如果是连续的，所有的子节点记录的时间戳应该是父节点的时间戳，除了根节点的时间戳
 * 假设u的子节点是v,
 * 如果u是割点，那么v不通过u是访问不了祖先节点
 * 同样，如果u是割点，那么v可访问的时间戳一定是大于u可访问的时间戳
 * 即如果 low[v]>=num[u] 【除了u是根节点的情况】，表示v节点不能绕过u节点访问祖先节点
 *
 * <p>
 * 图的割边
 */
public class SplitGraphTest {
    int[] num = new int[7]; //访问时间戳,比如num[1]=1,num[2]=3表示第一个访问的是1，第二个访问的是3号顶点
    int[] low = new int[7];//去除父顶点后可以访问的最小时间戳
    int[] flag = new int[7];//标记点是否是割点
    int[][] map = {
            {0, 0, 0, 0, 0, 0, 0},
            {0, 0, -1, 1, 1, -1, -1},
            {0, -1, 0, 1, 1, 1, 1},
            {0, 1, 1, 0, -1, -1, -1},
            {0, 1, 1, -1, 0, -1, -1},
            {0, -1, 1, -1, -1, 0, 1},
            {0, -1, 1, -1, -1, 1, 0},
    };
    int count = 0; //构造树加入的节点树

    int root = 1;

    int asked = -2;//访问过的标识

    /*
     * 割点
     *
     * 图
     * 6 7
     * 1 3 1
     * 1 4 1
     * 4 2 1
     * 3 2 1
     * 2 5 1
     * 2 6 1
     * 5 6 1
     *
     * */
    @Test
    public void testSplitPoint() {
        dfs(1, 1);
        System.out.println(Arrays.toString(num));
        System.out.println(Arrays.toString(low));
        System.out.println(Arrays.toString(flag));
    }

    @Test
    public void test() {
        markNum();
        System.out.println(Arrays.toString(num));
    }

    //        图
    //         1
    //      /    \
    //     4      3
    //      \    /
    //        2
    //      /   \
    //     5  -  6
    private void dfs(int cur, int parent) {//当前点和父节点
        count++;//时间戳
        int child = 0;
        num[cur] = count; //记录当前顶点cur时间戳
        low[cur] = count;//初始化最早能访问到的时间戳，当然是自己了
        //枚举当前点可以访问的边
        for (int i = 1; i < map[cur].length; i++) {
            if (map[cur][i] == 1) {
                if (num[i] == 0) {//没有访问的点
                    child++;
                    dfs(i, cur);
                    // 递归完成，树已经形成，子结点和当前结点中使用能访问的较小时间戳
                    low[cur] = Math.min(low[cur], low[i]);
                    if (cur != root && low[i] >= num[cur]) {
                        flag[cur] = 1;
                    } else if (cur == root && child == 2) {
                        // 如果是根结点有两个儿子，就是可以确定是割点
                        flag[cur] = 1;
                    }
                } else if (low[i] != parent) {
                    //已经访问了，但不是当前节点的父节点，
                    // 说明该节点是cur节点的祖先节点，cur的节点可以通过i节点更新low值
                    low[cur] = Math.min(low[cur], low[i]);
                }
            }
        }
    }

    /*
     * 时间戳和图的割点时间戳不一致
     * */
    private void markNum() {
        int[] book = new int[7];//记录节点是否访问过了
        num[1] = 1;
        book[1] = 1;
        int i = 1;
        int count = 1;
        while (count < 6) {
            boolean flag = true;
            for (int j = 1; j < map[i].length; j++) {
                if (map[i][j] > 0 && book[j] == 0) {//未访问的点
                    count++;
                    num[count] = j;
                    map[i][j] = asked;
                    map[j][i] = asked;
                    book[j] = 1;
                    i = j;
                    flag = false;
                    break;
                }
            }
            if (flag && count > 1) {
                i = num[count - 1];
            }
        }
    }
}
