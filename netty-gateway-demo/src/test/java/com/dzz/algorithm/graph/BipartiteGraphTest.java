package com.dzz.algorithm.graph;

import org.junit.Test;

import java.util.Arrays;

/**
 * @author zoufeng
 * @date 2020-8-3
 * <p>
 * 二分图
 * 定义：如果一个图的所有顶点可以分为x,y两个集合，且所有的边的两个顶点分别属于x,y集合
 * <p>
 * 例子：
 * 现在有一个二分图 ,左侧一个集合，右侧一个集合
 * x --- x1
 * x --- y1
 * y --- y1
 * y --- z1
 * z --- x1
 * <p>
 * 如何求得最大匹配数：
 * 首先从左边的x出发，先找出一条他的边，匹配x1,即x-x1
 * 接着左侧第二个点y,也找出一条他可以匹配的边，匹配y1,即y-y1
 * 最后剩下z，他只能匹配x1,但是x1已经和x匹配了，
 * 于是x1问x是否可以找到其他人匹配，若可以的话x1则和z匹配，
 * x接着从可以连接的边匹配，发现剩下的y1已经和y匹配，于是y1问y是否可以和其他点匹配，如果可以的话，y1将和x匹配（递归上面的过程）
 * 后面y找到了新的匹配点z1,于是递归回溯x匹配y1,z匹配x1
 * 这个过程结束多了一条匹配数，也叫增广路
 * <p>
 * 实现过程：
 * 邻接矩阵或者邻接表存储图
 * x数组存储match的y,即match[x]=y
 * y数组存储match的x,即match[y]=x
 * 最终随便遍历那个数组，不为0的即已匹配，总数即最大匹配数
 *
 * <p>
 * 应用：
 * 1、任务调度
 * 把一个工作完成一天的量当做是边
 * <p>
 * 2、二分图的最少顶点覆盖
 * 最小顶点覆盖要求用最少的点（X或Y中都行），让每条边都至少和其中一个点关联。
 * <p>
 * 3、有向无环图（DAG）的最小路径覆盖
 * 定义：在一个有向图中，找出最少的路径，使得这些路径经过了所有的点。
 * <p>
 * 最小路径覆盖分为最小不相交路径覆盖和最小可相交路径覆盖。
 * 最小不相交路径覆盖：每一条路径经过的顶点各不相同。如图，其最小路径覆盖数为3。即1->3>4，2，5。
 * 最小可相交路径覆盖：每一条路径经过的顶点可以相同。如果其最小路径覆盖数为2。即1->3->4，2->3>5。
 * 特别的，每个点自己也可以称为是路径覆盖，只不过路径的长度是0。
 * <p>
 * 算法：
 * 把原图的每个点V拆成Vx和Vy两个点，如果有一条有向边A->B，那么就加边Ax−>By。
 * 这样就得到了一个二分图。那么最小路径覆盖=原图的结点数-新图的最大匹配数。
 * <p>
 * 证明：
 * 一开始每个点都是独立的为一条路径，总共有n条不相交路径。
 * 我们每次在二分图里找一条匹配边就相当于把两条路径合成了一条路径，
 * 也就相当于路径数减少了1。所以找到了几条匹配边，路径数就减少了多少。
 * 所以有最小路径覆盖=原图的结点数-新图的最大匹配数。
 * <p>
 * 因为路径之间不能有公共点，所以加的边之间也不能有公共点，这就是匹配的定义。
 * <p>
 * <p>
 * <p>
 * 如何判断一个图是二分图
 * -、如果是联通图
 * 随机选用一个点作为根节点，进行bfs遍历,按树阶染色，
 * 比如第一阶黑色，第二阶白色，如果最后黑色节点数==白色节点数，则是二分图，否则不是
 */
public class BipartiteGraphTest {

    /*
     * 测试数据
     * 6 5  （左3右3）
     * 1 1 1
     * 1 2 1
     * 2 2 1
     * 2 3 1
     * 3 1 1
     * */
    private int[][] map = {
            {0, 0, 0, 0},
            {0, 1, 1, 0},
            {0, 0, 1, 1},
            {0, 1, 0, 0}
    };

    private int[] x = new int[4]; //x匹配y的记录
    private int[] y = new int[4]; //y匹配x的记录
    private int[] book = new int[4];//标记已经访问过

    private int matchNum = 0;

    private boolean dfs(int left) {
        for (int right = 1; right < map.length; right++) {
            if (book[right] == 1) continue;
            if (map[left][right] == 1) {
                if (y[right] == 0) {//right没有匹配
                    x[left] = right;
                    y[right] = left;
                    return true;
                } else if (y[right] > 0) {//叫他尝试匹配别的
                    book[right] = 1;
                    if (dfs(y[right])) {
                        x[left] = right;
                        y[right] = left;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Test
    public void test() {
        for (int i = 1; i < map.length; i++) {
            if (dfs(i)) {
                matchNum++;
                book = new int[4];//清空标记
            }
        }
        System.out.println(matchNum);
        System.out.println(Arrays.toString(x));
        System.out.println(Arrays.toString(y));
    }


    private boolean dfs2(int left) {
        for (int right = 1; right < map.length; right++) {
            if (book[right] == 1) continue;
            if (map[left][right] == 1) {
                book[right] = 1; //先尝试占用，占用失败清掉
                //1.right没有匹配 2.right已经匹配，尝试匹配其他的成功
                if (y[right] == 0 || (y[right] > 0 && dfs2(y[right]))) {
                    x[left] = right;
                    y[right] = left;
                    return true;
                }
                book[right] = 0;
            }
        }
        return false;
    }

    @Test
    public void test2() {
        for (int i = 1; i < map.length; i++) {
            if (dfs2(i)) {
                matchNum++;
                book = new int[4];//清空标记
            }
        }
        System.out.println(matchNum);
        System.out.println(Arrays.toString(x));
        System.out.println(Arrays.toString(y));
    }
}

