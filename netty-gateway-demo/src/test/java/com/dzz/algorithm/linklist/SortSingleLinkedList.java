package com.dzz.algorithm.linklist;

import org.junit.Before;
import org.junit.Test;

/**
 * @author zoufeng
 * @date 2020-1-14
 * <p>
 * 单链表排序
 */
public class SortSingleLinkedList {

    private ListNode head;

    //3,2,6,4,5,1,8
    @Before
    public void init() {
        head = new ListNode(3, new ListNode(2, new ListNode(6, new ListNode(4, new ListNode(5, new ListNode(1, new ListNode(8, null)))))));
    }

    /**
     * 在 O(n log n) 时间复杂度和常数级空间复杂度下，对链表进行排序。
     * 快排，归并排序时间复杂读O（nlogn）,空间复杂度O（n），但是快速排序不能保证算法复杂度一定是O(nlogn)，当数据比较集中时，即使做随机选取key值，算法的复杂度也非常接近O(N^2)
     * <p>
     * 知识点1：归并排序的整体思想
     * 知识点2：找到一个链表的中间节点的方法
     * 知识点3：合并两个已排好序的链表为一个新的有序链表
     */
    @Test
    public void sort() {
        ListNode listNode = mergeSort(head);
        do {
            System.out.println(listNode.getValue());
            listNode = listNode.getNext();
        } while (listNode != null);
    }

    public ListNode mergeSort(ListNode head) {
        if (head.getNext() == null) {
            return head;
        }
        //快慢指针找出中间结点，这块需要注意一点就是
        //我们需要一个标记sign跟踪慢结点，当找出中间结点时，
        //让中间结点前一结点即sign的下一个结点指向空
        //这样做的目的是为了使前半部分链表和后半部分链表进行合并排序

        //获取中间节点
        //慢结点
        ListNode s = head;
        //快结点
        ListNode f = head;
        //标记结点
        ListNode sign = null;
        while (f != null && f.getNext() != null) {
            sign = s;
            s = s.getNext();
            f = f.getNext().getNext();
        }
        //标记结点下一个结点为空
        sign.setNext(null);
        ListNode left = mergeSort(head);
        ListNode right = mergeSort(s);
        return MergeTwoList.merge(left, right);
    }


}
