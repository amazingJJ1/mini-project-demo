package com.dzz.algorithm.graph;

import org.junit.Test;

/**
 * @author zoufeng
 * @date 2020-7-27
 * <p>
 * 图的最小生成树
 * <p>
 * 应用：比如有6个城市，每个城市与城市之间过路的费用不一致，有的城市之间可以互通，有的不可以
 * 求通过所有城市花费最小费用
 * 城市的边示例 u v w，代表u城市到v城市的花费w
 * 有以下边
 * 6 9
 * 1 2 1
 * 1 3 2
 * 4 6 3
 * 5 6 4
 * 2 3 6
 * 4 5 7
 * 3 4 9
 * 2 4 11
 * 3 5 13
 * <p>
 * 原理，n个顶点的图至少需要n-1条边联系起来，也就是说我们需要找到n-1条边，包含n个顶点且边值和最小。
 * 算法：
 * 1.并查集判断生成树 Kruskal算法
 * 先按边的权重值从小到大快排，遍历加入边，加入的时候用并查集判断边是否导致联通，没有则选用这条边，直到选用n-1条边
 * 复杂度分析，快排复杂度是O(nlogn)，找出边的复杂度是MlogN (M条边，N个顶点)
 * 2.类似dijkstra处理 prim算法
 * 因为最终会包含所有的顶点，开始先随机选中一个顶点a，然后选择第二个离他最近的顶点，即求a顶点的最小出边，选第三个顶点时是选离1，2个顶点最近的顶点，
 * 即1，2顶点最短的出边，依次构建完所有顶点
 * 优点，算法较为简洁
 * 缺点，相比并查集的算法，复杂度略高，为O(n^2)
 */
public class TreeGraphTest {

    int n = 6;//顶点数
    int[] u = {5, 2, 4, 1, 1, 4, 3, 2, 3};//边的左顶点
    int[] v = {6, 3, 5, 2, 3, 6, 4, 4, 5};//边的右顶点
    int[] w = {4, 6, 7, 1, 2, 3, 9, 11, 13};//边的权重

    //排序后
/*    int[] u = {1, 1, 4, 5, 2, 4, 3, 2, 3};//边的左顶点
    int[] v = {2, 3, 6, 6, 3, 5, 4, 4, 5};//边的右顶点
    int[] w = {1, 2, 3, 4, 6, 7, 9, 11, 13};//边的权重*/

    int[] f = {0, 1, 2, 3, 4, 5, 6};//并查集数组，0是无效数字只是占位

    /*
     * 并查集判断添加边生成树
     *
     * Kruskal算法
     * */
    @Test
    public void testAndCheckSet() {
        //快排根据边的权重从小到大排序
        quickSort(0, w.length - 1);
        //加入最小权重边，并查集判断关联关系
        int num = 0;
        for (int i = 0; i < w.length; i++) {//枚举每一条边
            if (andCheckSet(i)) continue;
            num++;
            System.out.println(String.format("加入的边是 %d %d %d", u[i], v[i], w[i]));
            if (num == n - 1) break;
        }

    }

    private void quickSort(int left, int right) {
        if (left >= right) return;
        int temp = w[left];//默认采用第一个做比较值
        int i = left;
        int j = right;
        while (i != j) {
            //一定是先走右边，保证走到临界点的数是小于temp的
            while (w[j] >= temp && i < j) {
                j--;
            }
            while (w[i] <= temp && i < j) {
                i++;
            }
            if (i < j) {
                swap(i, j);
            }
        }
        swap(left, i);
        quickSort(left, i - 1);
        quickSort(i + 1, right);
    }

    private void swap(int left, int right) {
        swap(left, right, w);
        swap(left, right, u);
        swap(left, right, v);
    }

    private void swap(int left, int right, int[] arr) {
        int temp = arr[left];
        arr[left] = arr[right];
        arr[right] = temp;
    }

    //判断加入的第i条边是否有公共父节点,
    // 注意：这里和常规并查集不一样的地方是父节点的值默认设置最小的，防止无向图的父节点判断错误
    private boolean andCheckSet(int i) {
        int xf = getF(u[i]);
        int yf = getF(v[i]);
        if (xf != yf) {
            if (f[v[i]] < f[u[i]])
                f[u[i]] = f[v[i]];
            else
                f[v[i]] = f[u[i]];
            return false;
        }
        return true;
    }

    private int getF(int x) {
        if (x == f[x])
            return x;
        //路径压缩
        f[x] = getF(f[x]);
        return f[x];
    }


    /*
     * prim算法
     * 未用dijkstra优化，复杂度O(n^3)
     * */
    @Test
    public void testPrim() {
        int[] book = new int[n + 1];//标记已加入的顶点

        int INF = 999;//标记的不可达值
        //图存储,先初始化顶点
        int[][] map = new int[n + 1][n + 1];
        for (int i = 1; i < n + 1; i++) {
            for (int j = 1; j < n + 1; j++) {
                if (i == j)
                    map[i][j] = 0;
                else
                    map[i][j] = INF;
            }

        }
        //遍历边，初始化图
        for (int i = 0; i < u.length; i++) {
            map[u[i]][v[i]] = w[i];
            map[v[i]][u[i]] = w[i];
        }

        book[1] = 1;
        int num = 1;

        while (num < n) {
            int min = INF;
            int minI = 0;
            int minJ = 0;
            for (int i = 1; i <= n; i++) { //已存入的顶点
                if (book[i] == 1) {//找出离他最近的边，且边的顶点未加入book
                    for (int j = 1; j < map[i].length; j++) {
                        if (map[i][j] != 0 && map[i][j] != INF && book[j] != 1) {
                            if (map[i][j] < min) {
                                min = map[i][j];
                                minI = i;
                                minJ = j;
                            }
                        }
                    }
                }
            }
            System.out.println(String.format("依次加入的边是 %d %d %d", minI, minJ, map[minI][minJ]));
            book[minJ] = 1;
            num++;
        }
    }

    /*
     * dijkstra优化prim算法
     * 复杂度0(n^2)
     * */
    @Test
    public void testPrim2() {
        int[] book = new int[n + 1];//标记已加入的顶点
        int[] minJ = new int[n + 1];//记录缩短的边
        for (int i = 0; i < minJ.length; i++) {
            minJ[i] = 1;
        }

        int INF = 999;//标记的不可达值
        //图存储,先初始化顶点
        int[][] map = new int[n + 1][n + 1];
        for (int i = 1; i < n + 1; i++) {
            for (int j = 1; j < n + 1; j++) {
                if (i == j)
                    map[i][j] = 0;
                else
                    map[i][j] = INF;
            }
        }
        //遍历边，初始化图
        for (int i = 0; i < u.length; i++) {
            map[u[i]][v[i]] = w[i];
            map[v[i]][u[i]] = w[i];
        }
        book[1] = 1;
        int num = 1;
        int[] dis = map[1];

        while (num < n) {
            int min = INF;
            int minI = 0;
            for (int i = 1; i <= n; i++) {//遍历book数组
                if (book[i] == 0 && dis[i] < min) {//找到离存入点最近的点
                    min = dis[i];
                    minI = i;
                }
            }
            System.out.println(String.format("依次加入的顶点是 %d,加入的边是 %d %d %d", minI, minJ[minI], minI, map[minI][minJ[minI]]));//2 3 4 6 5
            book[minI] = 1;
            num++;
            //用新加入的顶点缩短未加入点的距离
            for (int i = 0; i <= n; i++) {
                if (book[i] == 0 && dis[i] > map[minI][i]) {
                    dis[i] = map[minI][i];
                    minJ[i] = minI;//记录优化用的顶点，相当于记录边
                }
            }
        }
    }



}
