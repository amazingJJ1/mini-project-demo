package com.dzz.algorithm.linklist;

import org.junit.Before;
import org.junit.Test;

/**
 * 合并两个有序链表
 *
 * @author zoufeng
 * @date 2020-1-14
 */
public class MergeTwoList {

    private ListNode head1;
    private ListNode head2;

    @Before
    public void init() {
        head1 = new ListNode(1, new ListNode(2, new ListNode(3, null)));
        head2 = new ListNode(1, new ListNode(4, new ListNode(5, null)));
    }

    @Test
    public void test() {
        ListNode listNode = merge(head1, head2);
        do {
            System.out.println(listNode.getValue());
            listNode = listNode.getNext();
        } while (listNode != null);
    }

    public static ListNode merge(ListNode node1, ListNode node2) {
        if (node1 == null) return node2;
        if (node2 == null) return node1;

        ListNode head = null;
        if (node1.getValue() <= node2.getValue()) {
            head = node1;
            head.setNext(merge(node1.getNext(), node2));
        } else {
            head = node2;
            head.setNext(merge(node1, node2.getNext()));
        }
        return head;
    }


}
