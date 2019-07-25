package com.dzz.algorithm.tree;

/**
 * @author zoufeng
 * @date 2019/7/10
 * <p>
 * 本质是一颗2-3-4树的另一种表现形式
 * <p>
 * 红黑树的染色和左旋右旋相当于2-3-4树节点的裂变
 * 关于2-3-4树
 * 2-节点，就是说，它包含 1 个元素和 2 个儿子，
 * 3-节点，就是说，它包含 2 个元素和 3 个儿子，
 * 4-节点，就是说，它包含 3 个元素和 4 个儿子
 * <p>
 * 【红色的节点相当于2-3-4树的连接符号，标识和他的父节点是在一起的2-3-4节点】
 * <p>
 * <p>
 * 1. 任何一个节点都有颜色，黑色或者红色
 * 2. 根节点是黑色的
 * 3. 父子节点之间不能出现两个连续的红节点
 * 4. 任何一个节点向下遍历到其子孙的叶子节点，所经过的黑节点个数必须相等
 * 5. 空节点被认为是黑色的
 * <p>
 * Rotate分为left-rotate（左旋）和right-rotate（右旋）
 * 区分左旋和右旋的方法是：
 * 【待旋转的节点从左边上升到父节点就是右旋，】
 * 【待旋转的节点从右边上升到父节点就是左旋。】
 */
public class RBTree {

    public class Node<T> {
        public T value;
        public Node<T> parent;
        public boolean isRead;
        public Node<T> left;
        public Node<T> right;
    }

}
