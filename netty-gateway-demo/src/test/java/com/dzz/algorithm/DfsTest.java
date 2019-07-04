package com.dzz.algorithm;

import org.junit.Before;
import org.junit.Test;

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
     * 1->2->4->3
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
     * 多源最短路径，动态规划
     * 只要5行代码
     * <p>
     * 分别求出只经历1号节点，只经历1，2号节点的最短路径
     */
    @Test
    public void testFloyd() {

        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map.length; j++) {
                for (int k = 0; k < map.length; k++) {
                    if (map[j][k] > map[j][i] + map[i][k]) {
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
}
