package com.dzz.algorithm.graph;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * @author zoufeng
 * @date 2020-8-4
 * <p>
 * 二分图的应用
 * <p>
 * 一、最大网络流
 * <p>
 * 网络流的相关定义：
 * <p>
 * 源点：有n个点，有m条有向边，有一个点很特殊，只出不进，叫做源点。
 * <p>
 * 汇点：另一个点也很特殊，只进不出，叫做汇点。
 * <p>
 * 容量和流量：每条有向边上有两个量，容量和流量，从i到j的容量通常用c[i,j]表示,流量则通常是f[i,j].
 * 通常可以把这些边想象成道路，流量就是这条道路的车流量，容量就是道路可承受的最大的车流量。
 * 很显然的，流量<=容量。
 * 而对于每个不是源点和汇点的点来说，可以类比的想象成没有存储功能的货物的中转站，
 * 所有“进入”他们的流量和等于所有从他本身“出去”的流量。
 * <p>
 * 最大流：把源点比作工厂的话，问题就是求从工厂最大可以发出多少货物，是不至于超过道路的容量限制，也就是，最大流。
 * <p>
 * 求解思路：
 * <p>
 * 首先，假如所有边上的流量都没有超过容量(不大于容量)，那么就把这一组流量，或者说，这个流，称为一个可行流。
 * <p>
 * 一个最简单的例子就是，零流，即所有的流量都是0的流。
 * <p>
 * (1).我们就从这个零流开始考虑，假如有这么一条路，这条路从源点开始一直一段一段的连到了汇点，
 * 并且，这条路上的每一段都满足流量‘<’容量，注意，是严格的<,而不是<=。
 * (2).那么，我们一定能找到这条路上的每一段的(容量-流量)的值当中的最小值delta。
 * 我们把这条路上每一段的流量都加上这个delta，一定可以保证这个流依然是可行流，这是显然的。
 * (3).这样我们就得到了一个更大的流，他的流量是之前的流量+delta，
 * 而这条路就叫做增广路。我们不断地从起点开始寻找增广路，每次都对其进行增广，直到源点和汇点不连通，也就是找不到增广路为止。
 * (4).当找不到增广路的时候，当前的流量就是最大流，这个结论非常重要。
 * 补充：
 * <p>
 * (1).寻找增广路的时候我们可以简单的从源点开始做BFS，并不断修改这条路上的delta 量，直到找到源点或者找不到增广路。
 * (2).在程序实现的时候，我们通常只是用一个c 数组来记录容量，而不记录流量，当流量+delta 的时候，我们可以通过容量-delta 来实现，以方便程序的实现。
 * <p>
 * 实现最大网络流中得问题：
 * 以下图为例
 * 1 2 1
 * 1 3 1
 * 2 3 1
 * 2 4 1
 * 3 4 1
 * 我们第一次找到了1-2-3-4这条增广路，这条路上的delta值显然是1。
 * 于是我们修改后得到了下面这个流。（图中的数字是容量）
 * 1 2 0
 * 1 3 1
 * 2 3 0
 * 2 4 1
 * 3 4 0
 * 这时候(1,2)和(3,4)边上的流量都等于容量了，我们再也找不到其他的增广路了，当前的流量是1。
 * 但实际情况 我们可以通过 1-2-4 和1-3-4两条路径访问，得到当前流量是2.
 * 那么我们刚刚的算法问题在哪里呢？
 * 问题就在于我们没有给程序一个“后悔”的机会，应该有一个不走(2-3-4)而改走(2-4)的机制。
 * 那么如何解决这个问题呢？
 * 我们利用一个叫做反向边的概念来解决这个问题。即每条边(i,j)都有一条反向边(j,i)，反向边也同样有它的容量。
 * 我们直接来看它是如何解决的：
 * 在第一次找到增广路之后，在把路上每一段的容量减少delta的同时，也把每一段上的反方向的容量增加delta。
 * c[x,y]-=delta;
 * c[y,x]+=delta;
 */
public class BipartiteGraphAppTest {

    private int n = 4;//4个顶点

    private int[][] cap = {//记录图的容量
            {0, 0, 0, 0, 0},
            {0, 0, 1, 1, 0},
            {0, 0, 0, 2, 2},
            {0, 0, 0, 0, 3},
            {0, 0, 0, 0, 0},
    };

    private int edmondsKarp(int start, int end) {
        int maxflow = 0;
        Queue<Integer> q = new ArrayDeque<>();

        while (true) {
            int[] visit = new int[n + 1];
            q.add(start);
            int index = 1;
            int delta = 999;
            visit[1] = 1;
            int[] history = new int[n + 1];//记录一条增广路径
            history[index] = start;
            while (!q.isEmpty()) //BFS寻找增广路
            {
                int u = q.poll();
                for (int v = 1; v <= end; v++)
                    if (visit[v] == 0 && u != v && cap[u][v] > 0) {//容量大于流量
                        delta = Math.min(delta, cap[u][v]);
                        //记录v的父亲，并加入队列中
                        history[++index] = v;
                        q.add(v);
                        visit[v] = 1;
                        break;
                    }
            }
            if (history[index] != end) return maxflow;//表示到不了end，即找不到增广路了
            //更新增广路的最大流
            maxflow += updateFlow(history, index, delta);
        }
    }

    private int updateFlow(int[] history, int index, int delta) {
        int num = 1;
        while (num + 1 <= index) {
            cap[history[num]][history[num + 1]] -= delta;
            cap[history[num + 1]][history[num]] += delta;
            num++;
        }
        return delta;
    }

    //dinic算法
    /*
     * 测试数据是
     * 4 5
     * 1 2 1
     * 1 3 1
     * 2 3 1
     * 2 4 1
     * 3 4 1
     * */
    @Test
    public void testMaxNetStream() {
        int i = edmondsKarp(1, 4);
        System.out.println(i);
    }

}
