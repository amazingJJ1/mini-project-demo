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
 *
 * <p>
 * 图的割边
 */
public class SplitGraphTest {
    private int[] dfn = new int[7]; //访问时间戳,比如num[1]=1,num[2]=3表示第一个访问的是1，第二个访问的是3号顶点
    private int[] low = new int[7];//去除父顶点后可以访问的最小时间戳
    private int[] visit = new int[7];//记录点是否访问过 ,0,没有访问，1表示正在访问，2表示已访问（他和他的子树已遍历完毕）
    private int[] splitPoint = new int[7];//标记点是否是割点
    //    private int[] splitEdge = new int[7];//标记点是否是割边
    private int[] historyLow = new int[7];//记录节点访问完成前的最小值

    //        图
    //         1
    //      /    \
    //     4      3
    //      \    /
    //        2
    //      /   \
    //     5  -  6
    // 回边 ：比如上面的图是1->3->2->4->1 4->1这里就是回边，可以发现，对于每一条回边，大概率是指向了dfn值更小的点。
    //测试割点
/*    private int[][] map = {
            {0, 0, 0, 0, 0, 0, 0},
            {0, 0, -1, 1, 1, -1, -1},
            {0, -1, 0, 1, 1, 1, 1},
            {0, 1, 1, 0, -1, -1, -1},
            {0, 1, 1, -1, 0, -1, -1},
            {0, -1, 1, -1, -1, 0, 1},
            {0, -1, 1, -1, -1, 1, 0},
    };*/

    //测试割点和割边
    private int[][] map = {
            {0, 0, 0, 0, 0, 0, 0},
            {0, 0, -1, 1, 1, -1, -1},
            {0, -1, 0, 1, 1, 1, 1},
            {0, 1, 1, 0, -1, -1, -1},
            {0, 1, 1, -1, 0, -1, -1},
            {0, -1, 1, -1, -1, 0, 1},
            {0, -1, -1, -1, -1, 1, 0},
    };

    private int timestamp = 0; //构造树加入的节点树

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
        System.out.println(String.format("访问顺序是： %s", Arrays.toString(dfn)));
        System.out.println(String.format("不经过父节点访问时间戳是： %s", Arrays.toString(low)));
        System.out.println(Arrays.toString(historyLow));
        System.out.println(String.format("割点是： %s", Arrays.toString(splitPoint)));
    }

    @Test
    public void test() {
        System.out.println(Arrays.toString(nodeTime()));
    }

    private void dfs(int cur, int parent) {//当前点和父节点
        visit[cur] = 1;
        timestamp++;//时间戳
        int child = 0;
        dfn[cur] = timestamp; //记录当前顶点cur时间戳
        low[cur] = timestamp;//初始化最早能访问到的时间戳，当然是自己了
        historyLow[cur] = timestamp;

        //枚举当前点可以访问的边
        for (int i = 1; i < map[cur].length; i++) {
            if (map[cur][i] == 1) {
                if (i == parent || visit[cur] == 2) continue;

                //遇到已经访问过的点，即回边，是祖先节点
                if (visit[i] == 1) {
                    historyLow[cur] = Math.min(low[cur], low[i]);
                }

                if (visit[i] == 0) {//没有访问的点
                    child++;
                    dfs(i, cur);
                    // 获取当前节点和当前节点子树能访问的最小时间戳，因为当前节点可能有几个子树，遍历子树时需要historyLow记录最小值
                    historyLow[cur] = Math.min(historyLow[cur], Math.min(low[cur], low[i]));
                    if (cur != 1 && low[i] >= dfn[cur]) {//核心代码，子节点不通过父节点能访问的时间戳大于父节点的时间戳，说明子节点只能通过父节点访问其他祖先节点
                        splitPoint[cur] = 1;
                        if (low[i] > dfn[cur]) {
                            System.out.println(String.format("割边是：%d %d", cur, i));
                        }
                    } else if (cur == 1 && child > 1) {
                        // 如果是根结点有两个儿子，就是可以确定是割点
                        splitPoint[cur] = 1;
                    }
                }
            }
        }
        low[cur] = historyLow[cur];
        visit[cur] = 2;
    }

    /*
     * 时间戳和图的割点时间戳不一致
     * */
    private int[] nodeTime() {
        int[] visit = new int[7];//记录节点是否访问过了
        int[] time = new int[7];//记录节点访问顺序
        time[1] = 1;
        visit[1] = 1;
        int i = 1;
        int count = 1;
        while (count < 6) {
            boolean flag = true;
            for (int j = 1; j < map[i].length; j++) {
                if (map[i][j] == 1 && visit[j] == 0) {//未访问的点
                    count++;
                    time[count] = j;
                    visit[j] = 1;
                    i = j;
                    flag = false;
                    break;
                }
            }
            //当前节点无子节点，回到父节点
            if (flag && count > 1) {
                i = time[count - 1];
            }
        }
        return time;
    }

}
