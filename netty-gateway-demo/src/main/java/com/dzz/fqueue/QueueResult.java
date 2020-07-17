package com.dzz.fqueue;

import java.util.List;

/**
 * @author zoufeng
 * @date 2020-7-7
 */
public class QueueResult {

    private List<byte[]> message;

    private int num;

    private int readerPosition;

    public List<byte[]> getMessage() {
        return message;
    }

    public QueueResult setMessage(List<byte[]> message) {
        this.message = message;
        return this;
    }

    public int getNum() {
        return num;
    }

    public QueueResult setNum(int num) {
        this.num = num;
        return this;
    }

    public int getReaderPosition() {
        return readerPosition;
    }

    public QueueResult setReaderPosition(int readerPosition) {
        this.readerPosition = readerPosition;
        return this;
    }
}
