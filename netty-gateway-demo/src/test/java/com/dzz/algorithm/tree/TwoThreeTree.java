package com.dzz.algorithm.tree;

/**
 * @author zoufeng
 * @date 2019/7/10
 * <p>
 * 2-3树是一棵平衡树，但不是二叉平衡树。
 * 对于高度相同的2-3树和二叉树，2-3树的节点数要大于满二叉树，因为有些节点可能有三个子节点。
 * 2-3树可以是一棵空树。
 * 对于2节点来说，该节点保存了一个key及对应的value，除此之外还保存了指向左右两边的子节点，子节点也是一个2-3节点，左子节点所有值小于key，右子节点所有值大于key。
 * 对于3节点来说，该节点保存了两个key及对应的value，除此之外还保存了指向左中右三个方向的子节点，子节点也是一个2-3节点，左子节点的所有值小于两个key中较小的那个，中节点的所有值在两个key值之间，右子节点大于两个key中较大的那个。
 * 对2-3树进行中序遍历能得到一个排好序的序列。
 * <p>
 * 2-3树在叶子节点达到三个元素时开始分裂
 * <p>
 * 插入第一个元素
 * |A
 * 插入第二个元素
 * |    A B
 * 插入第三个元素，节点有3个元素，开始分裂中间的元素到父节点
 * |         B
 * |    A       C
 */
public class TwoThreeTree {
}