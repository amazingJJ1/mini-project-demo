package com.dzz.algorithm;

import org.junit.Test;

/**
 * @author zoufeng
 * @date 2019/7/9
 */
public class LinkedListTest {

    public class ListNode {
        int value;
        ListNode next;
    }

    /**
     * 给定一个乱序的单链表的头节点，对该链表中的节点进行排序
     * 要求时间复杂度为O(nlgn），空间复杂度为O(1)
     * <p>
     * 有交换节点和交换数据的方式，采用交换数据的方式比较爽
     * <p>
     * 由于题目要求时间复杂度我O(nlgn），因此选择排序和插入排序可以排除。
     * 　　在排序算法中，时间复杂度为O(nlgn）的主要有：归并排序、快速排序、堆排序。
     * 　　其中堆排序的空间复杂度为（n），也不符合要求，因此也可以排除。
     * 　　归并排序在对数组进行排序时，需要一个临时数组来存储所有元素，空间复杂度为O(n）。但是利用归并算法对单链表进行排序时，可以通过next指针来记录元素的相对位置，因此时间复杂度也为O(1）。
     * 　　因此可以考虑用快排和归并来实现单链表的排序。
     * ---------------------
     */
    @Test
    public void sortLinkedList() {

    }

    @Test
    public void reversalLinkedList() {

    }

    private void res(ListNode node) {

    }
}
