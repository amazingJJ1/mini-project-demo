package com.dzz.algorithm.tree;

import org.junit.Test;

/**
 * @author zoufeng
 * @date 2019/7/10
 * <p>
 * 它是一种特殊的二叉查找树
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
 * 红黑树特性：
 * (1) 每个节点或者是黑色，或者是红色。
 * (2) 根节点是黑色。
 * (3) 每个叶子节点是黑色。 【注意：这里叶子节点，是指为空的叶子节点！】
 * (4) 如果一个节点是红色的，则它的子节点必须是黑色的。【即不能有连续两个的红色节点，但是可以有连续的黑节点】
 * (5) 从一个节点到该节点的子孙节点的所有路径上包含相同数目的黑节点。
 * <p>
 * Rotate分为left-rotate（左旋）和right-rotate（右旋）
 * 区分左旋和右旋的方法是：
 * 【待旋转的节点从左边上升到父节点就是右旋，】
 * 【待旋转的节点从右边上升到父节点就是左旋。】
 * <p>
 * 定理：
 * 一、一棵含有n个节点的红黑树的高度至多为2log(n+1).
 * 逆否命题是 "高度为h的红黑树，它的包含的内节点个数至少为 2^(h/2)-1个"。(因为算是满二叉树，高度为n的树，节点数为2^n-1)
 * <p>
 * 红黑树的插入：
 * 第一步: 将红黑树当作一颗二叉查找树，将节点插入。
 * 红黑树本身就是一颗二叉查找树，将节点插入后，该树仍然是一颗二叉查找树。
 * 也就意味着，树的键值仍然是有序的。此外，无论是左旋还是右旋，若旋转之前这棵树是二叉查找树，旋转之后它一定还是二叉查找树。
 * 这也就意味着，任何的旋转和重新着色操作，都不会改变它仍然是一颗二叉查找树的事实。
 * <p>
 * 第二步：将插入的节点着色为"红色"。
 * 为什么着色成红色，而不是黑色呢？为什么呢？在回答之前，我们需要重新温习一下红黑树的特性：
 * （5）从一个节点到该节点的子孙节点的所有路径上包含相同数目的黑节点。
 * 将插入的节点着色为红色，不会违背"特性(5)"！少违背一条特性，就意味着我们需要处理的情况越少。
 * 接下来，就要努力的让这棵树满足其它性质即可；满足了的话，它就又是一颗红黑树了
 * <p>
 * 第三步: 通过一系列的旋转或着色等操作，使之重新成为一颗红黑树。
 * 第二步中，将插入节点着色为"红色"之后，不会违背"特性(5)"。那它到底会违背哪些特性呢？
 * 对于"特性(1)"，显然不会违背了。因为我们已经将它涂成红色了。
 * 对于"特性(2)"，显然也不会违背。在第一步中，我们是将红黑树当作二叉查找树，然后执行的插入操作。而根据二叉查找数的特点，插入操作不会改变根节点。所以，根节点仍然是黑色。
 * 对于"特性(3)"，显然不会违背了。这里的叶子节点是指的空叶子节点，插入非空节点并不会对它们造成影响。
 * 对于"特性(4)"，是有可能违背的！
 * 那接下来，想办法使之"满足特性(4)"，就可以将树重新构造成红黑树了。
 * <p>
 * 那么如何执行第三步呢？
 * 插入一个红色节点后，核心思路都是：【将红色的节点移到根节点；然后，将根节点设为黑色】
 * 设插入的当前节点是N，父节点是P，叔叔节点是U，祖父节点是G
 * case1:
 * 插入的是根节点，节点直接变成黑色
 * case2:
 * 插入的节点父节点是黑色节点，节点本身是红色，什么都不用做
 * case3：
 * 如果插入的当前节点的父节点和叔节点是红色。
 * 将父节点和叔节点设置为黑色，祖父节点设置为红色，且当前节点变成祖父节点
 * 然后递归向上调整
 */
//
//              G黑                                   红G
//            /  \              变色                 /  \
//         红P     U红       ----------->         黑P  黑U
//        /  \                                  /  \
//      黑1 [N]红                           黑1   [N]红
//          /  \                                /  \
//        黑2  黑3                            黑2  黑3
/*
 * case4:
 * 如果插入的当前节点的父节点是红色，而叔节点是黑色
 * 且节点 N 是 P 的右孩子，且节点 P 是 G 的左孩子。此时先对节点 P 进行左旋，调整 N 与 P 的位置。接下来按照情况五进行处理，以恢复性质4。
 */
//
//              G黑                                    G黑
//            /  \              左旋                 /  \
//         红P   黑U       ----------->          红N   黑U
//        /  \                                  /  \
//      黑1  红N                            红P  黑3
//           /  \                           /  \
//         黑2  黑3                      黑1   黑2
/*
 * case5:
 * N 的父节点为红色，叔叔节点为黑色。
 * 且N 是 P 的左孩子，且节点 P 是 G 的左孩子。此时对 G 进行右旋，调整 P 和 G 的位置，并互换颜色。经过这样的调整后，性质4被恢复，同时也未破坏性质5。
 */
//
//              G黑                                    红P                                  黑P
//            /  \              右旋                 /  \           P和G换色               /  \
//        红P     U黑       ----------->         红N     G黑     ----------->           红N    红G
//        /  \                                  /  \    /   \                          /  \    /  \
//     红N   黑1                             黑2  黑3  黑1  U黑                     黑2  黑3  黑1  U黑
//    /  \
// 黑2  黑3

public class RBTree {

    public class Node {
        private Integer value;
        private Node parent;
        private boolean red;
        private Node left;
        private Node right;
        private boolean root;

        public Integer getValue() {
            return value;
        }

        public Node setValue(Integer value) {
            this.value = value;
            return this;
        }

        public Node getParent() {
            return parent;
        }

        public Node setParent(Node parent) {
            this.parent = parent;
            return this;
        }

        public boolean isRed() {
            return red;
        }

        public Node setRed(boolean red) {
            this.red = red;
            return this;
        }

        public Node getLeft() {
            return left;
        }

        public Node setLeft(Node left) {
            this.left = left;
            return this;
        }

        public Node getRight() {
            return right;
        }

        public Node setRight(Node right) {
            this.right = right;
            return this;
        }

        public boolean isRoot() {
            return root;
        }

        public Node setRoot(boolean root) {
            this.root = root;
            return this;
        }
    }

    private Node root;

    public void add(Integer t) {
        if (root == null) {
            root = new Node().setValue(t).setRed(false).setRoot(true);
            return;
        }
        Node node = findAndSetParent(t);
        if (node == null) return;
        add(node);
    }

    private Node findAndSetParent(Integer t) {
        Node parent = root;
        Node node = new Node().setValue(t).setRed(true).setRoot(false);
        while (parent != null) {
            if (t.equals(parent.value)) return null;//已存在
            if (t > parent.getValue()) {
                if (parent.getRight() == null) {
                    node.setParent(parent);
                    parent.setRight(node);
                    break;
                } else
                    parent = parent.getRight();
            } else if (t < parent.getValue()) {
                if (parent.getLeft() == null) {
                    node.setParent(parent);
                    parent.setLeft(node);
                    break;
                } else
                    parent = parent.getLeft();
            }
        }
        return node;
    }

    public void add(Node node) {
        Node parent = node.getParent();
        if (parent.isRoot() || !parent.isRed()) return;
        Node gParent = parent.getParent();
        Node uncle = gParent.getRight() == parent ? gParent.getLeft() : gParent.getRight();

        //case3 P红U红  设置为P,U黑
        if (uncle != null && uncle.isRed()) {
            if (gParent.isRoot()) {
                if (node == parent.getLeft() && parent == gParent.getLeft())
                    rightRotate(parent, gParent);
                if (node == parent.getRight() && parent == gParent.getRight())
                    leftRotate(gParent, parent);
                parent.setRoot(true);
                gParent.setRoot(false);
            } else {
                uncle.setRed(false);
                parent.setRed(false);
                gParent.setRed(true);
                add(gParent);
            }
        }

        //case4&5 P红U黑
        if (uncle == null || !uncle.isRed()) {
            if (node == parent.right && parent == gParent.left) {
                leftRotate(parent, node);
                add(parent);//当前节点替换为父节点
            }
            if (node == parent.left && parent == gParent.left) {
                rightRotate(parent,gParent);
                parent.setRed(false);
                gParent.setRed(true);
            }
        }
    }

    //右旋
    private void rightRotate(Node left, Node right) {
        left.setParent(right.parent);
        right.setParent(left);
        left.setRight(right);
        right.setLeft(left.getRight());
    }


    //左旋
    private void leftRotate(Node left, Node right) {
        left.setRight(right.getLeft());
        right.setParent(left.parent);
        left.setParent(right);
        right.setLeft(left);
    }


    //10 40 30 60 90 70 20 50 80
    @Test
    public void testRBTreeAdd(){
        RBTree rbTree = new RBTree();
        rbTree.add(10);
        rbTree.add(40);
        rbTree.add(30);
        rbTree.add(60);
        rbTree.add(70);
        rbTree.add(20);
        rbTree.add(50);
        rbTree.add(80);
        System.out.println(rbTree);
    }

}
