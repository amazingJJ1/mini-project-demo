package com.dzz.algorithm.linklist;

/**
 * @author zoufeng
 * @date 2020-1-14
 */
public class ListNode {

    private int value;

    private ListNode next;

    public ListNode(int value, ListNode next) {
        this.value = value;
        this.next = next;
    }

    public int getValue() {
        return value;
    }

    public ListNode setValue(int value) {
        this.value = value;
        return this;
    }

    public ListNode getNext() {
        return next;
    }

    public ListNode setNext(ListNode next) {
        this.next = next;
        return this;
    }
}
