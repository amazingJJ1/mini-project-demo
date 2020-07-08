package com.dzz.algorithm.linklist;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * @author zoufeng
 * @date 2020-1-14
 */
public class ReverseLinkList {

    private ListNode head;

    @Before
    public void init() {
        head = new ListNode(1, new ListNode(2, new ListNode(3, new ListNode(4, new ListNode(5, null)))));
    }

    @Test
    public void simple1() {
//        ListNode listNode = reverse1(head);
        ListNode listNode = reverse(head);
        do {
            System.out.println(listNode.getValue());
            listNode = listNode.getNext();
        } while (listNode != null);
    }

    /*
     *  head--node--node--tail
     * */
    public ListNode reverse(ListNode head) {
        ListNode pre = head;
        ListNode now = head.getNext();
        pre.setNext(null);
        while (now != null) {
            ListNode next = now.getNext();
            now.setNext(pre);
            if (next == null) {
                break;
            }
            pre = now;
            now = next;
        }
        return now;
    }
}
