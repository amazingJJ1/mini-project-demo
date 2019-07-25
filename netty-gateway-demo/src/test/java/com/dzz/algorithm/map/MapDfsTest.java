package com.dzz.algorithm.map;

import org.junit.Test;

/**
 * @author zoufeng
 * @date 2019/7/10
 */
public class MapDfsTest {

    private int[][] map = new int[][]{
            {0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0},
            {0, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 0, 0},
            {0, 1, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0}};

    int[][] dir = new int[][]{{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

    private int dfs(int[][] map, int x0, int y0) {
        int n = map.length;
        int m = map[0].length;

        //标记探索过的领域
        map[x0][y0] = 0;
        int sum = 1;
        for (int i = 0; i < 4; i++) {
            int x = x0 + dir[i][0];
            int y = y0 + dir[i][1];
            //坐标不越界,探索的位置是陆地（=1）时才深度遍历
            if (x >= 0 && x < n && y >= 0 && y < m && map[x][y] == 1)
                sum += dfs(map, x, y);
        }
        return sum;
    }

    /**
     * 求出岛屿连续陆地最大的面积
     */
    @Test
    public void test() {
        int max = 0;
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[0].length; j++) {
                if (map[i][j] == 1) {
                    max = Math.max(max, dfs(map, i, j));
                }
            }
        }
        System.out.println(max);
    }
}
