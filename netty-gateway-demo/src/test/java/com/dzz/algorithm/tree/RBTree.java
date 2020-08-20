package com.dzz.algorithm.tree;

import org.junit.Test;

/**
 * @author zoufeng
 * @date 2019/7/10
 * <p>
 * 它是一种特殊的二叉查找树
 * 本质红黑树就是一种以二叉树形式实现了2-3-4树的想法。但也可以完全不把他当成2-3-4树- -
 * 因此红黑树的各种操作其实是对应到2-3-4树的相应操作的。而2-3-4树是平衡树，所以红黑树的样子也就差不多算平衡树了。
 * ps：具体来说，红黑树其实就3类节点，分别是一黑，一黑一红，一黑二红。就是用来对应二，三，四节点的。其他细节的话还是看wiki吧。
 *
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
/*
 *case6:
 * N,P为红，U黑，N为P的左孩子，P为G的右孩子
 * N,P先右旋，当前节点设置为P，然后N，P再左旋并且变色
 *
 * */

/*
 * 删除分析
 * 红黑树删除操作的复杂度在于删除节点的颜色，当删除的节点是红色时，直接拿其孩子节点补空位即可。
 * 因为删除红色节点，性质5（从任一节点到其每个叶子的所有简单路径都包含相同数目的黑色节点）仍能够被满足。
 * 当删除的节点是黑色时，那么所有经过该节点的路径上的黑节点数量少了一个，破坏了性质5。
 * 如果该节点的孩子为红色，直接拿孩子节点替换被删除的节点，并将孩子节点染成黑色，即可恢复性质5。
 * 但如果孩子节点为黑色，处理起来就要复杂的多。分为6种情况，下面会展开说明。
 *
 * ①、如果待删除的节点没有子节点，那么直接删除即可。(如果是黑色节点，他的兄弟节点可能需要变色)　　
 * ②、如果待删除的节点只有一个子节点，那么直接删掉，并用其子节点去顶替它。　　
 * ③、如果待删除的节点有两个子节点，这种情况比较复杂：
 * 首先找出它的后继节点，然后处理“后继节点”和“被删除节点的父节点”之间的关系，
 * 最后处理“后继节点的子节点”和“被删除节点的子节点”之间的关系。每一步中也会有不同的情况。　　
 * 实际上，删除过程太复杂了，很多情况下会采用在节点类中添加一个删除标记，并不是真正的删除节点。详细的删除我们这里不做讨论。
 * 我们以前驱节点来举例
 * 第一种，前驱节点为黑色节点，同时有一个非空节点
 * 第二种，前驱节点为黑色节点，同时子节点都为空
 * 第三种，前驱节点为红色节点，同时子节点都为空
 *
 * case1 删除的是根节点
 * 如果左右子节点都是null最好办，根节点直接干掉，设置为null,如果不是，那么拿节点的前驱（左子树最大的节点）或后驱节点（右子树的）【且不是黑色节点】替换根节点
 *
 *
 * */

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

    public void remove(Integer t) {

    }

    private void remove(Node node) {

    }

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

    private void add(Node node) {
        Node parent = node.getParent();
        if (parent.isRoot() || !parent.isRed()) return;
        Node gParent = parent.getParent();
        Node uncle = gParent.getRight() == parent ? gParent.getLeft() : gParent.getRight();

        //case3 P红U红  设置为P,U黑
        if (parent.isRed() && uncle != null && uncle.isRed()) {
            uncle.setRed(false);
            parent.setRed(false);
            gParent.setRed(true);
            if (gParent.isRoot()) {
                gParent.setRed(false);
                return;
            } else {
                add(gParent);
                return;
            }
        }

        //case4&5  6&7、P红U黑
        if (parent.isRed() && (uncle == null || !uncle.isRed())) {
            if (node == parent.right && parent == gParent.left) {
                leftRotate(parent, node);
                add(parent);//当前节点指向父节点
                return;
            }
            if (node == parent.left && parent == gParent.left) {
                rightRotate(parent, gParent);
                parent.setRed(false);//PG换色
                gParent.setRed(true);
                if (gParent.isRoot()) {
                    gParent.setRoot(false);
                    parent.setRoot(true);
                    root = parent;
                }
                return;
            }

            if (node == parent.left && parent == gParent.right) {
                rightRotate(node, parent);
                add(parent);
                return;
            }

            if (node == parent.right && parent == gParent.right) {
                leftRotate(gParent, parent);//PG换色
                boolean temp = gParent.red;
                gParent.setRed(parent.red);
                parent.setRed(temp);
                if (gParent.isRoot()) {
                    gParent.setRoot(false);
                    parent.setRoot(true);
                    root = parent;
                }
            }
        }
    }

    //右旋
    private void rightRotate(Node left, Node right) {
        left.setParent(right.parent);
        if (right.parent != null) {
            if (right.parent.getLeft() == right)
                right.parent.setLeft(left);
            else
                right.parent.setRight(left);
        }
        right.setParent(left);
        right.setLeft(left.getRight());
        left.setRight(right);
    }


    //左旋
    private void leftRotate(Node left, Node right) {
        left.setRight(right.getLeft());
        right.setParent(left.parent);
        if (left.parent != null) {
            if (left.parent.getLeft() == left)
                left.parent.setLeft(right);
            else
                left.parent.setRight(right);
        }
        left.setParent(right);
        right.setLeft(left);
    }


    //10 40 30 60 90 70 20 50 80
    //                      30黑
    //                   /       \
    //                10黑       60红
    //                 \        /   \
    //                20红    40黑   70黑
    //                          \       \
    //                         50红     80红
    @Test
    public void testRBTreeAdd() {
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
