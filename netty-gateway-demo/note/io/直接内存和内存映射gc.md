### MappedByteBuffer
mapperByteBuffer一般是用来做文件内存映射
```java
// 通过 RandomAccessFile 创建对应的文件操作类，第二个参数 rw 代表该操作类可对其做读写操作
  RandomAccessFile raf = new RandomAccessFile("fileName", "rw");

  // 获取操作文件的通道
  FileChannel fc = raf.getChannel();

  // 也可以通过FileChannel的open来打开对应的fc
  // FileChannel fc = FileChannel.open(Paths.get("/usr/local/test.txt"),StandardOpenOption.WRITE);


  // 把文件映射到内存
  MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_WRITE, 0, (int)    fc.size());

  // 读写文件
  mbb.putInt(4);
  mbb.put("test".getBytes());
  mbb.force();

  mbb.position(0);
  mbb.getInt();
  mbb.get(new byte[test.getBytes().size()]);
```
看他的实现类只有一个DirectByteBuffer

这里体现了直接内存是怎么回收的
```java
    DirectByteBuffer(int cap) {                   // package-private

        super(-1, 0, cap, cap);
        boolean pa = VM.isDirectMemoryPageAligned();
        int ps = Bits.pageSize();
        long size = Math.max(1L, (long)cap + (pa ? ps : 0));
        Bits.reserveMemory(size, cap);

        long base = 0;
        try {
            base = unsafe.allocateMemory(size);
        } catch (OutOfMemoryError x) {
            Bits.unreserveMemory(size, cap);
            throw x;
        }
        unsafe.setMemory(base, size, (byte) 0);
        if (pa && (base % ps != 0)) {
            // Round up to page boundary
            address = base + ps - (base & (ps - 1));
        } else {
            address = base;
        }
        //本质是系统调用 unsafe.freeMemory(address);
        cleaner = Cleaner.create(this, new Deallocator(base, size, cap));
        att = null;
    }
```
内部的cleaner是个幻影引用的类


直接内存我们可以通过unsafe.allocateMemory()来进行申请分配，这个参数可以通过jvm参数 -DMaxDirectMemorySize来控制直接内存大小。
到达设置的阈值也会进行直接内存的回收。
这个unsafe分配的内存回收可以通过system.gc(不一定调用)进行回收。本质是fullG回收mapperByteBuffer对象，顺带回收该对象引用的直接内存。
但是文件直接内存映射却无法通过参数去控制。
目前文件可以通过两种方式进行内存回收：
1. 手动执行unmap方法

```java
// 在关闭资源时执行以下代码释放内存
Method m = FileChannelImpl.class.getDeclaredMethod("unmap", MappedByteBuffer.class);
m.setAccessible(true);
m.invoke(FileChannelImpl.class, buffer);
```
2. 使用内部的cleaner，不过需要代码反射获取
```java
AccessController.doPrivileged(new PrivilegedAction() {
    public Object run() {
      try {
        Method getCleanerMethod = buffer.getClass().getMethod("cleaner", new Class[0]);
        getCleanerMethod.setAccessible(true);
        sun.misc.Cleaner cleaner = (sun.misc.Cleaner)
        getCleanerMethod.invoke(byteBuffer, new Object[0]);
        cleaner.clean();
      } catch (Exception e) {
        e.printStackTrace();
      }
      return null;
    }
});

```
