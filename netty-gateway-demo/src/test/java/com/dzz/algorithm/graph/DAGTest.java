package com.dzz.algorithm.graph;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zoufeng
 * @date 2020-8-5
 * 顶点：图中的一个点
 * 边：连接两个顶点的线段叫做边，edge
 * 相邻的：一个边的两头的顶点称为是相邻的顶点
 * 度数：由一个顶点出发，有几条边就称该顶点有几度，或者该顶点的度数是几，degree
 * 路径：通过边来连接，按顺序的从一个顶点到另一个顶点中间经过的顶点集合
 * 简单路径：没有重复顶点的路径
 * 环：至少含有一条边，并且起点和终点都是同一个顶点的路径
 * 简单环：不含有重复顶点和边的环
 * 连通的：当从一个顶点出发可以通过至少一条边到达另一个顶点，我们就说这两个顶点是连通的
 * 连通图：如果一个图中，从任意顶点均存在一条边可以到达另一个任意顶点，我们就说这个图是个连通图
 * 无环图：是一种不包含环的图
 * 稀疏图：图中每个顶点的度数都不是很高，看起来很稀疏
 * 稠密图：图中的每个顶点的度数都很高，看起来很稠密
 * 二分图：可以将图中所有顶点分为两部分的图
 * <p>
 * <p>
 * DAG即有向无环图
 * <p>
 * 前提：如何判断一个图有环？
 * <p>
 * 【对于无向图】：
 * 算法一、
 * 我们知道对于环1-2-3-4-1，每个节点的度都是2，基于此我们有如下算法（这是类似于有向图的拓扑排序）：
 * 1、求出图中所有顶点的度，
 * 2、删除图中所有度<=1的顶点以及与该顶点相关的边，把与这些边相关的顶点的度减一
 * 3、如果还有度<=1的顶点重复步骤2
 * 4、最后如果还存在未被删除的顶点，则表示有环；否则没有环
 * 时间复杂度为O（E+V），其中E、V分别为图中边和顶点的数目，这个算法我们稍后分析算法3的时候再分析。
 * 算法二
 * 深度优先遍历该图，如果在遍历的过程中，发现某个节点有一条边指向已经访问过的节点，
 * 并且这个已访问过的节点不是当前节点的父节点（这里的父节点表示dfs遍历顺序中的父节点），
 * 则表示存在环。但是我们不能仅仅使用一个bool数组来标志节点是否访问过。如下图
 * 1-2-3-1
 * 从节点1开始遍历-接着遍历2-接着遍历3，然后发现3有一条边指向遍历过的1，则存在环。但是回到1节点时，它的另一条边指向已访问过的3，又把这个环重复计算了一次。
 * <p>
 * 我们按照算法导论22.3节深度优先搜索中，对每个节点分为三种状态，白、灰、黑。开始时所有节点都是白色，
 * 当开始访问某个节点时该节点变为灰色，当该节点的所有邻接点都访问完，该节点颜色变为黑色。
 * 那么我们的算法则为：如果遍历的过程中发现某个节点有一条边指向颜色为灰的节点，那么存在环。
 * 则在上面的例子中，回溯到1节点时，虽然有一条边指向已经访问过的3，但是3已经是黑色，所以环不会被重复计算。
 * <p>
 * <p>
 * 【对于有向图】
 * 算法三、kahn算法 原理即如果一个点没有form的点，则表示没有环。这个是从头删到尾
 * 1、计算图中所有点的入度，把入度为0的点加入栈
 * 2、如果栈非空：
 * 3、取出栈顶顶点a，输出该顶点值，删除该顶点
 * 4、从图中删除所有以a为起始点的边，如果删除的边的另一个顶点入度为0，则把它入栈
 * 5、如果图中还存在顶点，则表示图中存在环；否则输出的顶点就是一个拓扑排序序列
 * <p>
 * 算法二
 */
public class DAGTest {

    class Tuple<K, V> {
        K k;
        V v;

        public Tuple(K k, V v) {
            this.k = k;
            this.v = v;
        }

        public K getK() {
            return k;
        }

        public Tuple<K, V> setK(K k) {
            this.k = k;
            return this;
        }

        public V getV() {
            return v;
        }

        public Tuple<K, V> setV(V v) {
            this.v = v;
            return this;
        }
    }

    private class Node<T> {
        T point;
        List<Tuple<Node<T>, Node<T>>> edges = new ArrayList<>();

        public T getPoint() {
            return point;
        }

        public Node<T> setPoint(T point) {
            this.point = point;
            return this;
        }

        public void addEdge(Node<T> from, Node<T> to) {
            edges.add(new Tuple<>(from, to));
        }
    }

    class DAG<T> {

        Node<T> root;

        Map<T, Node<T>> nodeMap = new HashMap<>();

        public Node<T> getRoot() {
            return root;
        }

        public void setRoot(T t) {
            setRoot(nodeMap.get(t));
        }

        public void setRoot(Node<T> root) {
            this.root = root;
        }

        private void addNode(Node<T> node) {
            nodeMap.put(node.point, node);
        }

        public void addEdge(T from, T to) {
            Node<T> fNode = nodeMap.get(from) == null ? new Node<T>().setPoint(from) : nodeMap.get(from);
            Node<T> tNode = nodeMap.get(to) == null ? new Node<T>().setPoint(to) : nodeMap.get(to);
            fNode.addEdge(fNode, tNode);

            addNode(fNode);
            addNode(tNode);
        }

    }

    //构建dag
    @Test
    public void testDag() {
        Map<Integer, Integer> inDegree = new HashMap<>();
        Map<Integer, Integer> outDegree = new HashMap<>();
        int[] u = {1, 2, 3, 2};
        int[] v = {2, 3, 4, 4};
        //计算入度出度
        for (int i = 0; i < u.length; i++) {
            Integer vIn = inDegree.get(v[i]) == null ? 0 : (inDegree.get(v[i]) + 1);
            inDegree.put(v[i], vIn);
        }
        //获取入度为零的点作为根节点，如果没有则不是合格的DAG
        DAG<Integer> dag = new DAG<>();
        int root = 0;
        for (int i = 0; i < u.length; i++) {
            dag.addEdge(u[i], v[i]);
            if (inDegree.get(u[i]) == null)
                root = u[i];
        }
        dag.setRoot(root);

        System.out.println(dag);

    }
}
