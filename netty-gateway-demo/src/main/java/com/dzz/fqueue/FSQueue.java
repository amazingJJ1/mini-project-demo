package com.dzz.fqueue;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author zoufeng
 * @date 2020-7-7
 * <p>
 * 因为是内置的文件队列，这里消费消息更新缓存的实际情况为单生产者和单消费者，
 * FsQueue实例不被多线程引用情况下，写索引和读索引互不影响，无需加锁
 * <p>
 * 写入约80m/s，读取60M/s
 */
public class FSQueue {

    private final Logger log = LoggerFactory.getLogger(FSQueue.class);

    private static final AtomicBoolean fsQueueInit = new AtomicBoolean(false);

    private static final AtomicReference<String> initSeed = new AtomicReference<>(null);

    private static final String DEFAULT_SEED = "default";

    private String dbName;

    //预估每天写入30W数据，只写入索引id，预计20字节，一天写入约为6M，默认初始化为12M
    private final static int DEFAULT_FILE_LIMIT_LENGTH = 1024 * 1024 * 12;

    private int fileLimitLength;

    private static final String fileSeparator = System.getProperty("file.separator");

    private String path = null;

    /**
     * 文件操作实例
     */
    private LogIndex db = null;
    private LogEntity writerHandle = null;
    private LogEntity readerHandle = null;
    /**
     * 文件操作位置信息
     */
    private int readerIndex;//default 0
    private int writerIndex;

    public FSQueue(String dir, String dbName) throws Exception {
        this(dir, DEFAULT_FILE_LIMIT_LENGTH, dbName);
    }

    /**
     * 在指定的目录中，以fileLimitLength为单个数据文件的最大大小限制初始化队列存储
     *
     * @param dir             队列数据存储的路径
     * @param fileLimitLength 单个数据文件的大小，不能超过2G
     * @throws Exception
     */
    public FSQueue(String dir, int fileLimitLength, String dbName) throws Exception {
        this.fileLimitLength = fileLimitLength;
        File fileDir = new File(dir);
        if (!fileDir.exists() && !fileDir.isDirectory()) {
            if (!fileDir.mkdirs()) {
                throw new IOException("create dir error");
            }
        }
        path = fileDir.getAbsolutePath();

        initSeed();

        //检验索引文件是否被其他进程锁住
        this.dbName = getDbName(path, dbName);
        //当前进程锁住文件
        if (this.dbName.equals(dbName)) {
            this.dbName = dbName + initSeed.get();
            db = new LogIndex(path + fileSeparator + this.dbName);
        } else {
            LogIndex logIndex = new LogIndex(path + fileSeparator + this.dbName);
            if (logIndex.getFileLock() == null) {
                logIndex.close();
                this.dbName = dbName + initSeed.get();
                db = new LogIndex(path + fileSeparator + this.dbName);
            } else {
                this.db = logIndex;
            }
        }

        writerIndex = db.getWriterIndex();
        readerIndex = db.getReaderIndex();
        writerHandle = createLogEntity(path + fileSeparator + this.dbName + "_data_" + writerIndex + ".idb", db,
                writerIndex);
        if (readerIndex == writerIndex) {
            readerHandle = writerHandle;
        } else {
            readerHandle = createLogEntity(path + fileSeparator + this.dbName + "_data_" + readerIndex + ".idb", db,
                    readerIndex);
        }
        FileRunner deleteFileRunner = new FileRunner(path + fileSeparator + this.dbName + "_data_", fileLimitLength);


        Executor executor = Executors.newFixedThreadPool(1, new NameThreadFactory("FsQueue"));
        executor.execute(deleteFileRunner);
    }

    private void initSeed() {
        if (!fsQueueInit.get()) {
            fsQueueInit.set(true);
            initSeed.set("_" + RandomStringUtils.randomAlphabetic(10));
        }
    }

    private String getDbName(String path, String dbName) {
        File file = new File(path);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File file2 : files) {
                    String absolutePath = file2.getAbsolutePath();
                    if (!absolutePath.contains(dbName) || absolutePath.endsWith(".idb"))
                        continue;
                    try {
                        RandomAccessFile fis = new RandomAccessFile(file2, "rw");
                        FileChannel channel = fis.getChannel();
                        FileLock lock = channel.tryLock();
                        if (lock != null) {
                            dbName = absolutePath.substring(absolutePath.lastIndexOf(fileSeparator) + 1);
                            lock.release();
                            channel.close();
                            fis.close();
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return dbName;
    }

    /**
     * 创建或者获取一个数据读写实例
     */
    private LogEntity createLogEntity(String dbpath, LogIndex db, int fileNumber) throws IOException,
            FileFormatException {
        return new LogEntity(dbpath, db, fileNumber, this.fileLimitLength);
    }

    /**
     * 一个文件的数据写入达到fileLimitLength的时候，滚动到下一个文件实例
     */
    private void rotateNextLogWriter() throws IOException, FileFormatException {
        writerIndex = writerIndex + 1;
        writerHandle.putNextFile(writerIndex);
        if (readerHandle != writerHandle) {
            writerHandle.close();
        }
        db.putWriterIndex(writerIndex);
        writerHandle = createLogEntity(path + fileSeparator + dbName + "_data_" + writerIndex + ".idb", db,
                writerIndex);
    }


    public void add(String message) throws IOException, FileFormatException {
        add(message.getBytes());
    }

    public void offer(String message) throws IOException, FileFormatException {
        offer(message.getBytes());
    }

    public void offer(byte[] bytes) throws IOException, FileFormatException {
        add(bytes);
    }

    public byte[] poll() throws IOException, FileFormatException {
        return readNextAndRemove();
    }

    public void add(byte[] message) throws IOException, FileFormatException {
        short status = writerHandle.write(message);
        if (status == LogEntity.WRITEFULL) {
            rotateNextLogWriter();
            status = writerHandle.write(message);
        }
        if (status == LogEntity.WRITESUCCESS) {
            db.incrementSize();
        }

    }

    public void pollWithConsumer(QueueConsumer queueConsumer) {
        QueueResult queueResult = readAndNeedAck();
        if (queueResult.getNum() > 0) {
            queueConsumer.accept(queueResult.getMessage());
        }
        ackRead(queueResult);
    }


    @FunctionalInterface
    public interface QueueConsumer extends Consumer<List<byte[]>> {
    }

    /*
     * 读取元素但不更新读取游标，等待提交ack时更新
     * 一次性读取可以读取的元素 readerPosition->writerPosition
     * 但不跨文件读
     * */
    public QueueResult readAndNeedAck() {
        return readerHandle.readNextElementsAndNotAck();
    }

    /*
     * 更新游标，如果读到文件末尾需要更新readHandler
     * */
    public void ackRead(QueueResult queueResult) {
        if (queueResult.getReaderPosition() == -1) {
            try {
                readNextFile();
            } catch (IOException | FileFormatException e) {
                log.error(e.getMessage());
            }
        }

        if (queueResult.getNum() > 0) {
            db.decrementSize(queueResult.getNum());
            readerHandle.updateReaderPosition(queueResult.getReaderPosition());
            db.putReaderPosition(queueResult.getReaderPosition());
        }
    }

    /**
     * 从队列存储中取出最先入队的数据，并移除它
     */
    private byte[] readNextAndRemove() throws IOException, FileFormatException {
        byte[] b = null;
        try {
            b = readerHandle.readNextAndRemove();
        } catch (FileEOFException e) {
            readNextFile();
            try {
                b = this.readerHandle.readNextAndRemove();
            } catch (FileEOFException e1) {
                log.error("read new log file FileEOFException error occurred", e1);
            }
        }
        if (b != null) {
            db.decrementSize();
        }
        return b;
    }

    private void readNextFile() throws IOException, FileFormatException {
        int deleteNum = readerHandle.getCurrentFileNumber();
        int nextFile = readerHandle.getNextFile();
        //读完了直接关闭并放入删除队列
        readerHandle.close();
        FileRunner.addDeleteFile(path + fileSeparator + dbName + "_data_" + deleteNum + ".idb");
        // 更新下一次读取的位置和索引
        db.putReaderPosition(LogEntity.messageStartPosition);
        db.putReaderIndex(nextFile);
        if (writerHandle.getCurrentFileNumber() == nextFile) {
            readerHandle = writerHandle;
        } else {
            readerHandle = createLogEntity(path + fileSeparator + dbName + "_data_" + nextFile + ".idb", db,
                    nextFile);
        }
    }

    public void clear() throws IOException, FileFormatException {
        while (poll() != null) ;
    }

    public void close() {
        readerHandle.close();
        writerHandle.close();
    }

    public int size() {
        return getQueueSize();
    }

    private int getQueueSize() {
        return db.getSize();
    }

    public static boolean isInit() {
        return fsQueueInit.get();
    }

    public static String getSeed() {
        return initSeed.get();
    }
}
