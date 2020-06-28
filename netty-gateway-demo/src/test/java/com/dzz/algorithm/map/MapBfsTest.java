package com.dzz.algorithm.map;

import javafx.util.Pair;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zoufeng
 * @date 2020-6-28
 */
public class MapBfsTest {

    //寻找连续面积最大的地图
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

    private AtomicInteger atomicInteger = new AtomicInteger();

    @Test
    public void test() {
        ArrayDeque<Pair<Integer, Integer>> deque = new ArrayDeque<>(10);
        int rs = 0;
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[0].length; j++) {
                if (map[i][j] == 1) {
                    deque.addLast(new Pair<>(i, j));
                    rs = Integer.max(rs, bfs(deque));
                }
            }
        }
        System.out.println(rs);
        System.out.println(atomicInteger.get());
    }

    private int bfs(ArrayDeque<Pair<Integer, Integer>> deque) {
        int sum = 1;
        do {
            atomicInteger.incrementAndGet();
            Pair<Integer, Integer> pair = deque.removeFirst();
            Integer x = pair.getKey();
            Integer y = pair.getValue();
            map[x][y] = -1;
            for (int[] ints : dir) {
                int x0 = x + ints[0];
                int y0 = y + ints[1];
                if (x0 >= 0 && x0 < map.length && y0 >= 0 && y0 < map[0].length && map[x0][y0] == 1) {
                    deque.addLast(new Pair<>(x0, y0));
                    sum++;
                }
            }
        } while (!deque.isEmpty());
        return sum;
    }
}
