package com.dzz.algorithm;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author zoufeng
 * @date 2019/7/3
 */
public class DfsTest {


    /**
     * 有如下邻接矩阵表表示的图
     * 1.深度优先遍历
     * 2.广度优先遍历
     * 3. dijikstra 计算最短路径
     * <p>
     * 4 5
     * 1 2 5
     * 1 3 7
     * 1 4 9
     * 2 4 6
     * 4 3 8
     * <p>
     * <p>
     * 4个顶点 5条边
     * 顶点1到2 长度是5
     */

    private int[][] map;

    private static final int UNATTAINABLE = 999;

    private static final int USED = -1;

    @Before
    public void init() {
        map = new int[][]{
                {0, 1, 12, 999, 999, 999},
                {999, 0, 9, 3, 999, 999},
                {999, 999, 0, 999, 5, 999},
                {999, 999, 4, 0, 13, 15},
                {999, 999, 999, 999, 0, 4},
                {999, 999, 999, 999, 999, 0}};
    }

    public void dfsForeach(int index, int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            if (index == arr[i]) return;
            if (arr[i] == 0) {
                arr[i] = index;
                break;
            }
        }
        for (int i = 0; i < map.length; i++) {
            if (map[index - 1][i] != 0 && map[index - 1][i] != UNATTAINABLE && map[index - 1][i] != USED) {
                map[index - 1][i] = USED;
                dfsForeach(i + 1, arr);
            }
        }
    }

    @Test
    public void dfsTest() {
        int arr[] = new int[10];
        dfsForeach(1, arr);
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == 0) break;
            System.out.println(arr[i]);
        }

    }

    /**
     * Dijikstra求单源最短路径，不能解决负权边问题
     * 单源最短路径
     */
    @Test
    public void normalDijikstra() {
        int[] dis = new int[map.length];
        int[] book = new int[map.length];
        //book数组初始化，表示确认好的最短路径点
        //dis数组初始化，记录最短路径
        //初始化其他点到1号点的路径值
        for (int j = 0; j < map.length; j++) {
            book[j] = 0;
            dis[j] = map[0][j];
        }
        book[0] = 1;

        for (int i = 1; i < map.length; i++) {
            int min = dis[i];
            int u = 1;
            //找到离初始点最近的点（离一号点最近的点）
            for (int j = 1; j < map.length; j++) {
                if (dis[j] < min && book[j] == 0) {
                    min = dis[j];
                    u = j;
                }
            }
            //标记已找到离1号点最近的顶点
            book[u] = 1;

            //按边松弛，核心代码
            for (int j = 1; j < map.length; j++) {
                if (map[u][j] < UNATTAINABLE) {
                    if (dis[j] > dis[u] + map[u][j]) {
                        dis[j] = dis[u] + map[u][j];
                    }
                }
            }
        }

        for (int i = 0; i < dis.length; i++) {
            System.out.println(dis[i]);
        }
    }

    /**
     * 多源最短路径，动态规划 Floyd算法，不能解决负权边问题
     * 只要5行代码
     * <p>
     * 依次加入节点，查看加入节点后对整个路径的影响，选择最短路径
     */
    @Test
    public void testFloyd() {

        for (int i = 0; i < map.length; i++) {//加入的i个节点
            for (int j = 0; j < map.length; j++) { //j行
                for (int k = 0; k < map.length; k++) { //k列
                    if (map[j][k] > map[j][i] + map[i][k]) { //查看经过第i个节点是否会把距离变短
                        map[j][k] = map[j][i] + map[i][k];
                    }
                }
            }
        }

        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map.length; j++) {
                System.out.print(map[i][j] + " ");
            }
            System.out.println();
        }
    }


    //动态规划求图的最短路径
    /*
     * 有5个城市，1-5，求1号城市到5号城市的最短路径
     * 数据如下：
     * 5 8
     * 1 2 2
     * 1 5 10
     * 2 3 3
     * 2 5 7
     * 3 1 4
     * 3 4 4
     * 4 5 5
     * 5 3 3
     * 5个城市8条边，1到2距离是2
     *
     * 思路：
     * 假设到起始城市可以路过i个城市到达目的地
     * 依次加入城市，查看加入的城市是否会把到不同城市的路径缩短
     *
     * 其实这就是单源最短路径，Dijikstra算法
     *
     * 这个其实也属于动态规划思想
     *
     * 动态决策问题的特点：
     * 1.系统所处的状态和时刻是进行决策的重要因素；
     * 2.即在系统发展的不同时刻（或阶段）根据系统所处的状态，不断地做出决策；
     * 3.找到不同时刻的最优决策以及整个过程的最优策略。
     *
     * */
    @Test
    public void testDp() {
        int[][] map = {
                {0, 2, 999, 999, 10},
                {999, 0, 3, 999, 7},
                {4, 999, 0, 4, 999},
                {999, 999, 999, 0, 5},
                {999, 999, 3, 999, 0}
        };

        //初始化dp 表示只经过1号城市时，1号到其他城市的最短距离
        int[] dp = map[0];

        for (int i = 1; i < 5; i++) {//加入的第i个城市
            //dp数组更新
            for (int j = 1; j < 5; j++) {
                if (map[i][j] != 999) //i号城市不能到达j号城市，没有意义
                    dp[j] = Math.min(dp[j], dp[i] + map[i][j]);
            }
        }
        System.out.println(Arrays.toString(dp));
    }

    /*
     * 使用正权边验证Bellman-ford算法,果然牛逼
     *      * 5 8
     * 1 2 2
     * 1 5 10
     * 2 3 3
     * 2 5 7
     * 3 1 4
     * 3 4 4
     * 4 5 5
     * 5 3 3
     * */
    @Test
    public void testDp2() {
        int[] dp = {0, 2, 999, 999, 10};
        int[] u = {1, 1, 2, 2, 3, 3, 4, 5};
        int[] v = {2, 5, 3, 5, 1, 4, 5, 3};
        int[] w = {2, 10, 3, 7, 4, 4, 5, 3};
        for (int k = 0; k < 4; k++) {
            int[] dpPre = Arrays.copyOf(dp, 5);
            for (int i = 0; i < 8; i++) {
                int dis = dp[u[i] - 1] + w[i];
                if (dp[v[i] - 1] > dis)
                    dp[v[i] - 1] = dis;
            }
            if (Arrays.equals(dpPre, dp)) {
                System.out.println(k);
                break;
            }
        }
        System.out.println(Arrays.toString(dp));
    }

    /*
     * 一般邻接表是数组+链表形式实现，其实就是一个哈希表
     * 假设5个顶点 ,其中顶点在数组中，链表放的是顶点可以到达的其他顶点
     * 1 -> 2 -> 5
     * 2 -> 3 -> 4
     * 3 -> 4
     * 4 -> 2 -> 5
     * 5
     *
     * 数组实现邻接表
     *
     * 以下面图
     * 数据如下：
     * 4 5
     * 1 4 9
     * 2 4 6
     * 1 2 5
     * 4 3 8
     * 1 3 7
     *
     * 4个点5条边，1到4距离是9
     *
     * 这里需要5个数组实现邻接表,
     * u,v,w,first,next
     * u,v,w表示第i条表的 起始点，目的点，权重值
     * first表示第i个顶点 的第一条边，next则表示第i条表的下一条边
     *
     * 例子：
     * u={1,4,1,2,1}
     * v={4,3,2,4,3}
     * w={9,8,5,6,7}
     *
     * first={5,4,-1,2}
     * next={-1,-1,1,-1,3}
     * 这里我们就可以通过邻接表来遍历图了，
     * 比如第一个顶点1，他的第一条边是第5条,下一条是next[5]=3,第3条边，第3条边的下一条是第1条边
     * 因为是数组表示，实际0索引是第一个节点
     * 最终结果是
     * first={4,3,-1,1}
     * next={-1,-1,0,-1,2}
     * */
    @Test
    public void testLingJieTable() {
        int[] u = {1, 4, 1, 2, 1};
        int[] v = {4, 3, 2, 4, 3};
        int[] w = {9, 8, 5, 6, 7};
        int[] first = {-1, -1, -1, -1};//-1表示暂时顶点暂时没有相应的边
        int[] next = new int[5];
        for (int i = 0; i < 5; i++) {
            int fi = first[u[i] - 1];
            next[i] = fi;//记录上一条边，就算上一条没有，等到是-1，和结果是一致的
            first[u[i] - 1] = i;
        }
        System.out.println(Arrays.toString(next));
    }

    /*
     * Bellman-ford
     * 单源最短路径，可解决负权边问题
     * 5 5
     * 2 3 2
     * 1 2 -3
     * 1 5 5
     * 4 5 2
     * 3 4 3
     * */
    @Test
    public void testBellmanFord() {
        int[] dp = {0, 999, 999, 999, 999};//因为有负权边，初始假设1号顶点到其他顶点距离不可达，为999
        int[] u = {2, 1, 1, 4, 3};
        int[] v = {3, 2, 5, 5, 4};
        int[] w = {2, -3, 5, 2, 3};

        /*
         *外层循环k这里最多循环4次，是因为最短路径最多n-1条边
         *外层每循环一次，代表图多用一条边改善路径
         * */
        for (int k = 0; k < 4; k++) {
            int[] dpPre = Arrays.copyOf(dp, 5);//对算法优化，如何第n轮循环和之前没有变化，表示已是最优
            for (int i = 0; i < 5; i++) {
                int dis = dp[u[i] - 1] + w[i];
                if (dp[v[i] - 1] > dis) //表示如果1号点到v[i]的距离可以通过第i条边变小，获取最短路径，外层第二次循环可以在此基础上考察接用其他边是否同样缩短距离
                    dp[v[i] - 1] = dis;
            }
            if (Arrays.equals(dpPre, dp)) {
                System.out.println(k);
                break;
            }
        }
        System.out.println(Arrays.toString(dp));

    }

    /*
     * 在上面可以看到BellmanFord算法对边有重复计算（松弛）
     * 
     *
     * */
    @Test
    public void testBellmanFord2() {
        int[] dp = {0, 999, 999, 999, 999};//因为有负权边，初始假设1号顶点到其他顶点距离不可达，为999
        int[] u = {2, 1, 1, 4, 3};
        int[] v = {3, 2, 5, 5, 4};
        int[] w = {2, -3, 5, 2, 3};

        /*
         *外层循环k这里最多循环4次，是因为最短路径最多n-1条边
         *外层每循环一次，代表图多用一条边改善路径
         * */
        for (int k = 0; k < 4; k++) {
            int[] dpPre = Arrays.copyOf(dp, 5);//对算法优化，如何第n轮循环和之前没有变化，表示已是最优
            for (int i = 0; i < 5; i++) {
                int dis = dp[u[i] - 1] + w[i];
                if (dp[v[i] - 1] > dis) //表示如果1号点到v[i]的距离可以通过第i条边变小，获取最短路径，外层第二次循环可以在此基础上考察接用其他边是否同样缩短距离
                    dp[v[i] - 1] = dis;
            }
            if (Arrays.equals(dpPre, dp)) {
                System.out.println(k);
                break;
            }
        }
        System.out.println(Arrays.toString(dp));

    }
}
