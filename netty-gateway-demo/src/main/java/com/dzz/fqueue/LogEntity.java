package com.dzz.fqueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author zoufeng
 * @date 2020-7-7
 */
public class LogEntity {

    private final Logger log = LoggerFactory.getLogger(LogEntity.class);

    public static final byte WRITESUCCESS = 1;

    public static final byte WRITEFAILURE = 2;

    public static final byte WRITEFULL = 3;

    public static final String MAGIC = "FQueuefs";

    public static int messageStartPosition = 20;

    private File file;
    private RandomAccessFile raFile;
    private FileChannel fc;

    private MappedByteBuffer mappedByteBuffer;

    //默认12M
    private int fileLimitLength = 1024 * 1024 * 12;

    private LogIndex db = null;
    /**
     * 文件操作位置信息
     */
    private String magicString = null;
    private int version = -1;
    private int readerPosition = -1;
    private int writerPosition = -1;
    private int nextFile = -1;
    private int endPosition = -1;
    private int currentFileNumber = -1;

    public LogEntity(String path, LogIndex db, int fileNumber,
                     int fileLimitLength) throws IOException, FileFormatException {
        this.currentFileNumber = fileNumber;
        this.fileLimitLength = fileLimitLength;
        this.db = db;
        file = new File(path);
        // 文件不存在，创建文件
        if (!file.exists()) {
            createLogEntity();
            FileRunner.addCreateFile(Integer.toString(fileNumber + 1));
        } else {
            raFile = new RandomAccessFile(file, "rwd");
            if (raFile.length() < LogEntity.messageStartPosition) {
                throw new FileFormatException("file format error");
            }
            fc = raFile.getChannel();
            mappedByteBuffer = fc.map(FileChannel.MapMode.READ_WRITE, 0,
                    this.fileLimitLength);
            // magicString
            byte[] b = new byte[8];
            mappedByteBuffer.get(b);
            magicString = new String(b);
            if (!magicString.equals(MAGIC)) {
                throw new FileFormatException("file format error");
            }
            // version
            version = mappedByteBuffer.getInt();
            // nextfile
            nextFile = mappedByteBuffer.getInt();
            endPosition = mappedByteBuffer.getInt();
            // 未写满
            if (endPosition == -1) {
                this.writerPosition = db.getWriterPosition();
            } else if (endPosition == -2) {// 预分配的文件
                this.writerPosition = LogEntity.messageStartPosition;
                db.putWriterPosition(this.writerPosition);
                mappedByteBuffer.position(16);
                mappedByteBuffer.putInt(-1);
                this.endPosition = -1;

            } else {
                this.writerPosition = endPosition;
            }
            if (db.getReaderIndex() == this.currentFileNumber) {
                this.readerPosition = db.getReaderPosition();
            } else {
                this.readerPosition = LogEntity.messageStartPosition;
            }
        }
        Executor executor = Executors.newFixedThreadPool(1, new NameThreadFactory("FsQueue.logEntity"));
        executor.execute(new Sync());

    }

    public class Sync implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (mappedByteBuffer != null) {
                    try {
                        mappedByteBuffer.force();
                    } catch (Exception e) {
                        break;
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        break;
                    }
                } else {
                    break;
                }
            }

        }

    }

    public int getCurrentFileNumber() {
        return this.currentFileNumber;
    }

    public int getNextFile() {
        return this.nextFile;
    }

    private boolean createLogEntity() throws IOException {
        if (!file.createNewFile()) {
            return false;
        }
        raFile = new RandomAccessFile(file, "rwd");
        fc = raFile.getChannel();
        mappedByteBuffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, this.fileLimitLength);
        mappedByteBuffer.put(MAGIC.getBytes()); //0+8 byte
        mappedByteBuffer.putInt(version);// 8+4 version
        mappedByteBuffer.putInt(nextFile);// 12 + 4 next fileindex
        mappedByteBuffer.putInt(endPosition);// 16 + 4
        mappedByteBuffer.force();
        this.magicString = MAGIC;
        this.writerPosition = LogEntity.messageStartPosition;
        this.readerPosition = LogEntity.messageStartPosition;
        db.putWriterPosition(this.writerPosition);
        return true;
    }

    /**
     * 记录写位置
     */
    private void putWriterPosition(int pos) {
        db.putWriterPosition(pos);
    }

    private void putReaderPosition(int pos) {
        db.putReaderPosition(pos);
    }

    /**
     * write next File number id.
     */
    public void putNextFile(int number) {
        mappedByteBuffer.position(12);
        mappedByteBuffer.putInt(number);
        this.nextFile = number;
    }

    private boolean isFull(int increment) {
        // confirm if the file is full
        if (this.fileLimitLength < this.writerPosition + increment) {
            return true;
        }
        return false;
    }

    public byte write(byte[] log) {
        int increment = log.length + 4;
        if (isFull(increment)) {
            mappedByteBuffer.position(16);
            mappedByteBuffer.putInt(this.writerPosition);
            this.endPosition = this.writerPosition;
            return WRITEFULL;
        }
        mappedByteBuffer.position(this.writerPosition);
        mappedByteBuffer.putInt(log.length);
        mappedByteBuffer.put(log);
        this.writerPosition += increment;
        putWriterPosition(this.writerPosition);
        return WRITESUCCESS;
    }

    public byte[] readNextAndRemove() throws FileEOFException {
        if (this.endPosition != -1 && this.readerPosition >= this.endPosition) {
            throw new FileEOFException("file eof");
        }
        // readerPosition must be less than writerPosition
        if (this.readerPosition >= this.writerPosition) {
            return null;
        }
        mappedByteBuffer.position(this.readerPosition);
        int length = mappedByteBuffer.getInt();
        byte[] b = new byte[length];
        this.readerPosition += length + 4;
        mappedByteBuffer.get(b);
        putReaderPosition(this.readerPosition);
        return b;
    }


    public QueueResult readNextElementsAndNotAck() {
        if (this.endPosition != -1 && this.readerPosition >= this.endPosition)
            return new QueueResult().setReaderPosition(-1);//-1表示这个文件已经读取完成
        if (this.readerPosition >= this.writerPosition)
            return new QueueResult().setReaderPosition(readerPosition);

        List<byte[]> list = new ArrayList<>();
        int num = 0;
        int temp = this.readerPosition;
        while (temp < writerPosition) {
            mappedByteBuffer.position(temp);
            int length = mappedByteBuffer.getInt();
            byte[] b = new byte[length];
            mappedByteBuffer.get(b);
            temp += length + 4;
            list.add(b);
            num++;
        }

        return new QueueResult()
                .setMessage(list)
                .setNum(num)
                .setReaderPosition(temp);
    }

    public void close() {
        try {
            if (mappedByteBuffer == null) {
                return;
            }
            mappedByteBuffer.force();
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    try {
                        Method getCleanerMethod = mappedByteBuffer.getClass()
                                .getMethod("cleaner", new Class[0]);
                        getCleanerMethod.setAccessible(true);
                        sun.misc.Cleaner cleaner = (sun.misc.Cleaner) getCleanerMethod
                                .invoke(mappedByteBuffer, new Object[0]);
                        cleaner.clean();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            });
            mappedByteBuffer = null;
            fc.close();
            raFile.close();
        } catch (IOException e) {
            log.error("close logentity file error:", e);
        }
    }

    public void updateReaderPosition(int readerPosition) {
        if (readerPosition >= 20)
            this.readerPosition = readerPosition;
    }

    public String headerInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(" magicString:");
        sb.append(magicString);
        sb.append(" version:");
        sb.append(version);
        sb.append(" readerPosition:");
        sb.append(readerPosition);
        sb.append(" writerPosition:");
        sb.append(writerPosition);
        sb.append(" nextFile:");
        sb.append(nextFile);
        sb.append(" endPosition:");
        sb.append(endPosition);
        sb.append(" currentFileNumber:");
        sb.append(currentFileNumber);
        return sb.toString();
    }

}
