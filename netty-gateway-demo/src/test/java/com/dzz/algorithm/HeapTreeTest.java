package com.dzz.algorithm;

import org.junit.Test;

/**
 * @author zoufeng
 * @date 2019/7/4
 */
public class HeapTreeTest {

    private int[] arr = new int[]{10, 11, 13, 7, 30, 17, 36, 27, 4, 3, 8};
    //在arr基础上构建堆，0号索引不方便计算，舍弃
    private int[] heapArr = new int[arr.length + 1];

    /**
     * 生成堆有两种方式
     * 第一种是动态添加 数据从最下面的叶子节点上浮
     * 第二种是先构造一个完全二叉树  数据从上面往下面下沉
     * <p>
     * 这里采用第二种方式来生成最小堆
     */
    private void initMinHead() {
        System.arraycopy(arr, 0, heapArr, 1, arr.length);
        for (int i = arr.length / 2; i > 0; i--) {
            siftDownMin(i);
        }
        for (int i = 1; i < heapArr.length; i++) {
            System.out.print(heapArr[i] + " ");
        }
    }

    private void siftDownMin(int index) {
        //temp是最小节点的索引
        int temp = 0;
        while ((2 * index) < arr.length) {
            //左子节点
            temp = (heapArr[index] > heapArr[2 * index]) ? 2 * index : index;

            //右子节点
            if ((2 * index + 1) < heapArr.length && heapArr[temp] > heapArr[(2 * index) + 1]) {
                temp = (2 * index) + 1;
            }

            if (temp != index) {
                //子节点比父节点小，需要继续交换
                swap(temp, index);
                index = temp;
            } else {
                //父节点已经最小了
                break;
            }
        }

    }

    private void siftDownMax(int index, int heapLen) {
        int temp = 0;
        while ((2 * index) <= heapLen) {
            //左子节点
            if (heapArr[index] < heapArr[2 * index]) {
                temp = 2 * index;
            } else {
                temp = index;
            }
            //右子节点
            if ((2 * index + 1) <= heapLen && heapArr[temp] < heapArr[(2 * index) + 1]) {
                temp = (2 * index) + 1;
            }

            if (temp != index) {
                //子节点比父节点小，需要继续交换
                swap(temp, index);
                index = temp;
            } else {
                //父节点已经最小了
                break;
            }
        }

    }

    private void swap(int srcIndex, int destIndex) {
        int t = heapArr[srcIndex];
        heapArr[srcIndex] = heapArr[destIndex];
        heapArr[destIndex] = t;
    }

    private void initMaxHeap() {
        System.arraycopy(arr, 0, heapArr, 1, arr.length);
        for (int i = arr.length / 2; i > 0; i--) {
            siftDownMax(i, arr.length);
        }
        for (int i = 1; i < heapArr.length; i++) {
            System.out.print(heapArr[i] + " ");
        }
    }

    @Test
    public void minHeap() {
        initMinHead();
    }

    @Test
    public void maxHeap() {
        initMaxHeap();
    }

    @Test
    public void headSort() {
        initMaxHeap();
        int n = arr.length;
        //每次最大值交换到叶子节点，第二大的就可以上浮，依次交换则可以得到小-大的数组
        while (n > 1) {
            swap(1, n);
            n--;
            siftDownMax(1, n);
        }

        System.out.println();
        for (int i = 1; i < heapArr.length; i++) {
            System.out.print(heapArr[i] + " ");
        }
    }
}
