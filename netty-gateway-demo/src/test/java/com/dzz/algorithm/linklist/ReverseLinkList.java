package com.dzz.algorithm.linklist;

import org.junit.Before;
import org.junit.Test;

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
        ListNode listNode = reverse1(head);
        do {
            System.out.println(listNode.getValue());
            listNode = listNode.getNext();
        } while (listNode != null);
    }

    private ListNode reverse1(ListNode head) {
        ListNode pre = head;
        ListNode next = head.getNext();
        pre.setNext(null);

        ListNode end = null;
        while (next != null) {
            //5.获取尾节点指针
            end = next;
            //1.设置当前节点指针
            ListNode temp = next;
            //2.获取下一个节点指针
            next = next.getNext();
            //3.当前节点指向上一个节点
            temp.setNext(pre);
            //4.上一个节点指针变为当前节点
            pre = temp;
        }
        return end;
    }
}
