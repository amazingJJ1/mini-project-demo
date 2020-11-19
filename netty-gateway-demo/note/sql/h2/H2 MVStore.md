# H2 MVStore

本文源码分析基于h2-1.4.200版本

MVStore是H2数据库的存储系统，最底层的数据结构是K-V。

> **其实所有的数据库，无论是SQL还是NOSQL,无论是单机还是分布式数据库，都存在K-V的抽象，但对于KV这个接口的设计，还是存在差别的。**

关系数据库在Key-Value的基础上以主键为Key，以行为Value构建了表，列存储数据库如HBase将这些行打散，成为用rowkey相关联的单独列单元，文档数据库如MongoDB则是变value为文档，Redis则是干脆剥干净外衣，变成了纯粹的Key-Value系统。

同样，不同的还有怎么去写和读这些Key-Value。事实上，也就是怎么将这些数据精细地转化于各个存储介质之间，可以只存于内存，比如Redis或H2、sqlite、Mysql的内存数据库形式，也可以是单文件如H2、sqlite的文件形式，还可以是多文件而统一于表空间，如Oracle、Mysql，postgresql等，甚至可以是分布式文件系统。 

### 存储结构

H2 Store的结构设计从下到上是：

- KV data
- Page
- B- tree Page 
- Chunk

这套结构的核心是 **B-tree**和**Page**

这里的page是对应了操作系统的页缓存。操作系统里的Page(页)是固定大小的内存，一般为4k，读写数据时，哪怕你读一个字节或写一个字节，载入主存或写入辅存的也是一个页。而作为数据库，要最大限度的减少磁盘读写，就是尽量把这些页塞满，这样，读一次就有最多有用的数据，因此数据库的Page往往设置与操作系统的Page相同或者是操作系统Page的整数倍。把这些Page以类似目录的形式连接，这就有了B-tree，它在树装存储结构和顺序表之间做了一个平衡。

H2在外面还包了一层chunk,代表了一系列**变更操作的集合**，[这个也是CopyOnWrite的设计思想]



下面来重点分析下page

### Page Btree

我们从官方的demo代码入手

```java
MVStore s = MVStore.open("hehe.mv");
MVMap<Integer, String> map = s.openMap("data");
for (int i = 0; i < 100000; i++) {
    map.put(i, "Hello");
}
s.commit();
s.close();
```

分析page的三个过程

#### 1、page 写入

```java
public synchronized V put(K key, V value) {
    DataUtils.checkArgument(value != null, "The value may not be null");
    beforeWrite();
    long v = writeVersion;
    Page p = root.copy(v);
    p = splitRootIfNeeded(p, v);
    Object result = put(p, v, key, value);
    newRoot(p);
    return (V) result;
}
```

写入的过程是线程安全的，写入前先复制page root

```java
public Page copy(long version) {
    Page newPage = create(map, version,
            keys, values,
            children, totalCount,
            getMemory());
    // mark the old as deleted
    removePage();
    newPage.cachedCompare = cachedCompare;
    return newPage;
}
```

复制的过程只是获取相关的key,value,还有childrenPage的引用，还有使用的内存的数量

接着是删除原root页的引用: 主要是找到以前根节点的chunk位置，将原page页标记失效

```java
Chunk c = getChunk(pos);
long version = currentVersion;
if (map == meta && currentStoreVersion >= 0) {
    if (Thread.currentThread() == currentStoreThread) {
        // if the meta map is modified while storing,
        // then this freed page needs to be registered
        // with the stored chunk, so that the old chunk
        // can be re-used
        version = currentStoreVersion;
    }
}
registerFreePage(version, c.id,
        DataUtils.getPageMaxLength(pos), 1);
```

接着返回复制的新页，在新页中插入需要写入的KV。写入的过程就是B树写值的过程

最后将新页标记为新的root页

#### 2、page 分裂

page分裂的判断条件很简单，就是page的memory是否达到了设置的分裂值。

```java
private Page splitLeaf(int at) {
    int a = at, b = keys.length - a;
    Object[] aKeys = new Object[a];
    Object[] bKeys = new Object[b];
    System.arraycopy(keys, 0, aKeys, 0, a);
    System.arraycopy(keys, a, bKeys, 0, b);
    keys = aKeys;
    Object[] aValues = new Object[a];
    Object[] bValues = new Object[b];
    bValues = new Object[b];
    System.arraycopy(values, 0, aValues, 0, a);
    System.arraycopy(values, a, bValues, 0, b);
    values = aValues;
    totalCount = a;
    Page newPage = create(map, version,
            bKeys, bValues,
            null,
            bKeys.length, 0);
    recalculateMemory();
    newPage.recalculateMemory();
    return newPage;
}
```

分裂的过程就是简单将原来的page数据分半，返回新生成的页，新的页在父节点的引用里面按顺序添加

叶子节点，内部节点和根节点流程差不多，只不过叶子节点和内部节点分裂一般是放入数据的过程中触发。

#### 3、page 持久化

持久化是page的重点，这里要结合Store的文件格式来分析

首先是Store的整体结构

```txt
[ file header 1 ] 
[ file header 2 ] 
[ chunk ] [ chunk ] ... [ chunk ]
```

持久化的文件包含两个部分：

- 文件头 
- chunk 数据块。每个chunk是一次变更的集合

可以看到文件头有两个，这个是出于数据安全的考虑（暂时不是很清晰的解释）。

然后是大量的chunk. 每个文件头是一个4096 bytes的block.每个chunk至少一个block，但是通常是 200个或者更多个block. 数据已日志结构存储的形式存储在chunk中. 每个版本都有一个chunk。

查看写入文件后的格式

```txt
H:2,block:4,blockSize:1000,chunk:5,created:171ca8c122a,format:1,version:5,fletcher:eec210ce
H:2,block:4,blockSize:1000,chunk:5,created:171ca8c122a,format:1,version:5,fletcher:eec210ce
```

- H: 这个暂时不知道啥意思，默认2
- blocksize 块大小，默认是4096也就是4K，16进制表示是1000
- block 表示是最新的chunk的所在的块位置
- chunk 表示最新的chunk的id,
- created 文件创建的时间
- format 写入格式化，默认1
- version 目前的文件写入版本
- fletcher 使用Fletcher32算法得到的checksum，是对前面7个属性求checksum



接下来分析chunk的格式

##### chunk格式

写入过程中是以chunk为单位，每个版本有一个chunk，**chunk相当于当前版本变更操作的集合**。每个chunk由chunk header, 这个版本中发生修改的pages , 和一个chunk footer组成。page包含map中实际的数据。chunk里的page被存储在header的后面的，彼此相邻（未对齐）。chunk的大小是block大小的倍数， footer被存储在chunk最后面的128字节中。

```no
[ header ] [ page ] [ page ] ... [ page ] [ footer ]
```

footer是用来验证数据的完整性的，因为一个chunk对应一次操作。也可以用来找到文件中最后一个chunk的开始位置。**最后这个page页面存储的meta page**.

chunk的写入文件后的格式示例：

```bash
chunk:1,block:2,len:1,map:6,max:1c0,next:3,pages:2,root:4000004f8c,time:1fc,version:1
chunk:1,block:2,version:1,fletcher:aed9a4f6
```

这些字段解析如下：

- **chunk**: chunk id
- **block**: chunk的第一个block (乘以块大小以获取文件中的位置).
- **len**: chunk的大小（以block数量为单位）
- **map**: 最新map的id; 当新map创建时会增加.
- **max**: 所有的最大的page size的大小总和
- **next**: 为下一个chunk预估的开始位置.（block位置）
- **pages**: 一个chunk中page的个数
- **root**: metadata根page的位置 (see page format).
- **time**: 写chunk的时间, 从文件创建到写chunk之间的隔的毫秒数.
- **version**: chunk表示的版本
- **fletcher**: footer的check sum.

Chunk永远不会取代更新。每个块包含该版本中已更改的页面（每个版本有一个块，请参见上文），以及这些页面的所有父节点（递归地直到根页面）。如果更改(删除或添加了map中的数据)则将相应的page复制，修改并存储在下一个chunk中，并且旧块中活动page的数量将减少。该机制称为写时复制(copy-on-write)，与[Btrfs](https://en.wikipedia.org/wiki/Btrfs)文件系统的工作方式类似 。没有活动page的块被标记为空闲，因此该空间可被更多的最新chunk重用。因为并非所有chunk都具有相同的大小，所以一段时间内，chunk之前可能会有许多空闲chunk（直到写入一个小的chunk或将这些chunk压缩为止）。有一个[ 45秒的延迟](http://stackoverflow.com/questions/13650134/after-how-many-seconds-are-file-system-write-buffers-typically-flushed)（默认情况下），然后覆盖可用chunk，以确保首先保留新版本。

**打开FileStore时如何定位最新Chunk?**

**StoreHeader**文件头包含最近Chunk的位置，但并不总是最新chunk的位置。这是为了减少文件头更新的次数。打开文件后，将读取文件头和最后一块的块脚注（在文件末尾）。从这些候选中，读取最近chunk的header。如果它包含“下一个”指针（请参见上文），则还将读取这些chunk的head和footer。如果结果是一个较新的有效chunk，则重复此过程，直到找到最新的chunk为止。

**StoreHeader(文件头)更新**

在写入一个chunk之前，根据下一个chunk与当前chunk具有相同大小的假设来预测下一个块的位置。当写入下一个块时，先前的预测被证明是不正确的，文件头也将更新。在任何情况下，如果下一链长于20跳，文件头就会更新

##### page格式

- length (int): page的长度(以bytes为单位)。
- checksum (short): Checksum 值(块ID或块长度内的偏移量)。
- mapId (variable size int): 这页所属map的id。
- len (variable size int): 这个页中key的数量。
- type (byte): 页的类型。0 表示左page, 1 表示内部节点; 加2代表键值对采用了LZF算法压缩, 加6代表键值对采用了Deflate 算法压缩。
- children (array of long; internal nodes only): 子节点位置。
- childCounts (array of variable size long; internal nodes only): 已知子页的实体总数。
- keys (byte array): 所有的键， 根据数据类型存储.
- values (byte array; leaf pages only): 所有值，根据数据类型存储。

**页存储顺序**

即使这不是文件格式所要求的，页仍以如下顺序存储：

官方文档解释

> 针对每一个map，先存储根页面，然后存储内部节点（如果有），然后存储叶子页面，因为顺序读的速度高于随机读。元数据的map被存储在一个chunk的尾端。

但持久化的过程看代码是先存储根页，然后递归存储他的子页，最后的**叶子页面存储其实是不连续的**

指向页的指针被当做一个64位的long型存储，使用了一个特殊的格式：26位用于chunk id，32位用于在chunk内的位移，5位用于长度码，1位用于页类型(叶子还是内部节点)。

这种编码的优势：

1. 页类型经过编码，当清除或移除一个map时，叶子节点不必被读取（内部节点需要被读取以使得程序知道所有的页在哪里，而且在一个典型的B-tree结构中，绝大多数page是叶子页）。
2. 绝对文件位置没有被包含以至于在不必改变页指针的情况下chunk能在文件里被移除，仅有chunk的元数据需要被修改。
3. 长度码是一个从0到31的数字，0表示这个页的最大长度是32bytes,1代表48bytes 2: 64, 3: 96, 4: 128, 5: 192, 以此类推，直至31代表1MB 。如此一来，读取一个页仅仅需要一个读操作(除非是很大的页)。
4. 所有页的最大长度的和被存储在chunk元数据的max字段，并且当一个页被标记成“移除了”，活动页最大长度将被调整。这样不仅可以估算空闲页数的个数，还允许估算一个block内的剩余空间。

保留子页面中条目的总数，以实现有效的范围计数，按索引查找和跳过操作。这些页面形成一个[计数的B树](https://www.chiark.greenend.org.uk/~sgtatham/algorithms/cbtree.html)。

数据压缩：page类型后的数据可以选择LZF压缩算法进行压缩。



##### meta map

除用户map之外，还有个元数据map，它含有用户map的名字、位置及其chunk元数据。 chunk的最后一页含有元数据map的root page。 root page的精确位置被存储在chunk的header里。**这个page(直接地或间接地)指向所有其他map的root page**。

一个store的元数据map名字叫data，所以一般也叫metadata map。持久化前debug可以看到meta Page中包含如下数据：

```txt
"chunk.1" -> "chunk:1,block:2,len:2,liveMax:0,livePages:0,map:2,max:18a0,next:4,pages:7,root:400004f206,time:14,version:1"
"map.1" -> "name:dd"
"map.2" -> "name:ee"
"name.dd" -> "1"
"name.ee" -> "2"
"root.1" -> "8000002581"
"root.2" -> "800002b181"
```

- chunk  比如chunk.1,代表chunk 1的元数据. 这是和chunk header相同的数据，活动的page的数量, 和最大的活动长度。
- map.x  比如map.1,map.2,表示map 1和map2的元数据。这个实体是：名字、创建版本和类型。
- name.x 比如name.dd , 表示名字为dd的map id。他的值是1
- root.x  表示 map1的root位置.
- setting.storeVersion: store的版本(一个用户定义的值).

写入文件后的查看如下。

```txt
map.1 map.2 name.dd name.ee root.1 root.2 name:dd name:ee 1 2 4000002501 400002b101
```



其实根据上面的StoreHead,Chunk,Page结构设计，已经可以很清晰的理出H2的寻址过程：

首先读取存储文件的第一个块，也就是StoreHead,可以在里面找到最近（多版本下可能不是最新）的chunk,

page持久化是在store.commit()的时候进行的

持久化的顺序

- 持久化前新建一个chunk，以记录本次持久化的操作
- 构建chunk head






  H2提供了几种方式，内存方式、单文件方式、寄存方式（存储于postsql），貌似还有集群形式（还待验证是什么样的集群）。其中，单文件的方式是用的最多的一种方式，也是我们关注的重点。 

数据库通常会有这么几个模块，KV存储、事务、索引，这三者之间的关系看起来泾渭分明，但实际上交织耦合，其中存在很多设计点。

关于kv store的讨论可以参考https://www.zhihu.com/question/345222300 为什么分布式数据库喜欢用kv store?





官方文档说明，MVStore是一个持久的，日志结构化的键值存储。它用作H2的默认存储子系统，但也可以直接在应用程序中使用，而无需使用JDBC或SQL.

先查看下类结构，包含了这些字段：

- BackgroundWriterThread 后台写入线程
- fileStore 文件存储的操作类，主要是FileChannel Nio写入文件
- pageSplitSize 定义了分页的page大小，默认16K
- cache CacheLongKeyLIR 缓存的替代算法 分两种，一个是Page为单位，记录缓存，默认16M，一个是PageChildren,主要是page chunk references
- lastChunk 最新的Chunk引用
- chunks 所有的chunks，记录所有的操作和变更
- freePageSpace  ConcurrentHashMap<Long,HashMap<Integer, Chunk>>
- meta 元信息map，记录键值类型，root page的位置
- maps 实际的数据
- storeHeader 文件头
- writeBuffer
- versionToKeep 没看懂
- compression 压缩相关的大小，压缩策略、等级等
- backgroundExceptionHandler
- currentVersion 当前版本
- lastStoredVersion 最终的Chunk版本
- retainChunk 最早版本的Chunk
- autoCommitMemory 自动提交的内存，默认19M
- autoCommitBufferSize 写buffer的大小
- autoCommitDisable 禁用自动提交



先抛开日志机构化存储的设计，MVStore本质是K-V的map存储，也就是MVMap。在持久化的过程中，数据是按页存储的，对应操作系统的page。H2在这里也将map构建成一颗Btree,这个Btree的节点是Page,这个时候就有了一些问题：

1. map是如何分页的？
2. 这些页面及数据是如何构建成Btree被快速寻址？
3. KV修改时，page是如何保证引用的正确性？



现在从源码开始

```java
MVStore s = MVStore.open("hehe.mv");
MVMap<Integer, String> map = s.openMap("data");
for (int i = 0; i < 400; i++) {
    map.put(i, "Hello");
}
s.commit();
s.close();
```

创建mvstore,从open（）进入

```java
public static MVStore open(String fileName) {
    HashMap<String, Object> config = New.hashMap();
    config.put("fileName", fileName);
    return new MVStore(config);
}
```

很简单，只是根据配置新建一个mvstore

### StroeHead

新建mvstore会初始化上面列举的字段参数，fileStore会开始新建文件并写如StoreHead,默认是一个block块大小，占据4096字节。有8个属性

- H:默认是2
- blockSize:4096
- created 创建时间
- format:1 默认是1，format_write
- block:0 block块的位置
- chunk:0 chunk块的位置
- version:0 
- fletcher:3bde3c9a (使用Fletcher32算法得到的checksum，是对前面7个属性求checksum)



### MVStore打开MVMap的过程

```java
public <K, V> MVMap<K, V> openMap(String name) {
    return openMap(name, new MVMap.Builder<K, V>());
}
```

这里分析新建的流程

新建很简单，只是Store的meta记录下新建的map信息，比如mapid，map name

而map初始化过程也是新建个空page，标记为root，起始的pos为0。如果是文件中读取则是从store读取该map的root的位置

## MVMap

mvmap是kv数据的容器，他本身非常简单

```
public class MVMap<K, V> extends AbstractMap<K, V>
        implements ConcurrentMap<K, V> {
        
protected MVStore store;

/**
 * The current root page (may not be null).
 */
protected volatile Page root;

/**
 * The version used for writing.
 */
protected volatile long writeVersion;

private int id;
private long createVersion;
private final DataType keyType;
private final DataType valueType;

private ConcurrentArrayList<Page> oldRoots =
        new ConcurrentArrayList<Page>();

private boolean closed;
private boolean readOnly;
private boolean isVolatile;
```

首先本身是一个concurrentHashMap，保证了操作时并发安全

额外属性：

- root b树的Page根节点引用
- keyType valueType 键值类型
- store 所属的MVStore
- readonly 是否只读



### MVMap存储数据的过程

MVMap.put

```java
public synchronized V put(K key, V value) {
    DataUtils.checkArgument(value != null, "The value may not be null");
    beforeWrite();
    long v = writeVersion;
    Page p = root.copy(v);
    p = splitRootIfNeeded(p, v);
    Object result = put(p, v, key, value);
    newRoot(p);
    return (V) result;
}
```

每次写入的时候重新更新根节点page页，并将数据放入page中

```java
public Page copy(long version) {
    Page newPage = create(map, version,
            keys, values,
            children, totalCount,
            getMemory());
    // mark the old as deleted
    removePage();
    newPage.cachedCompare = cachedCompare;
    return newPage;
}
```

更新的过程简单粗暴，直接将原来的key,value还有子page节点的引用,交给新建的页，然后删除老页的信息。





这类似CopyOnWriteList的思想



上面有两个注意的点，就是writeversion这个字段，这个字段是每写一次新增一个版本，debug的时候经常莫名其妙的看到被新加版本了，其实是被backgroundWriterThread线程搞鬼







-----------------------------------------------------

但是，B树里面存的数据是什么是不一定的，在课本里经常提到的是B树的阶数，一般用m代替。并且说这个m一般由页的大小确定，比如页大小为4096 Byte，里面存的是int一个是4 Byte，那么就用1024阶，即一个节点存1024个索引项或数据项。但是如果索引项和数据项不固定的呢？H2在这里对Page做了一定修改，它里面的性质如下:

一个Page至少有一个数据或索引项
Page里的数据项数量不定
  这样Page的大小是一个参考值而不是限定值。也就是说，如果一个Page里只有一条数据，而那个数据特别大，这条数据不能分在两个Page，这时Page只能适应数据的大小。而Page里面的数据数量是不一定的，只要它尽量填满页的空间。而在H2中，每个Page就是一个节点，因此，H2中的B树实际上是只能说是一个非典型的B树。与此相对的是Sqlite，Sqlite的页大小是一致的， 因此，它的叶子节点也分为空闲页、普通页和溢出页，页内还有碎片、自由块。这无疑是一种更精细的空间控制，也更加复杂，这将在以后分析。 
   Chunk则是MvStore最大的特点，这也可以从它的名字看出来。MvStore全称是Muti-Version Store。它的存储是这样工作的。

```java
MVStore s = MVStore.open("haha.mv");
MVMap<Integer, String> map = s.openMap("data");
for (int i = 0; i < 400; i++) {
    map.put(i, "Hello");
}
s.commit();
for (int i = 0; i < 100; i++) {
    map.put(i, "Hi");
}
s.commit();
s.close();
```

这样Commit了两次，会出现两个Chunk 
Chunk 1: 

- Page 1: (root) 两个指针指向 page 2 和 3 
- Page 2: leaf 有140个数据 (keys 0 - 139) 
- Page 3: leaf 有260个数据 (keys 140 - 399) 
Chunk 2: 
- Page 4: (root) 两个指针指向 page 3和 5 
- Page 5: leaf 有140个数据 (keys 0 - 139) 

第二次修改，Upate了Page2之中的前100个数据，它不是直接修改Page1，而是copy了Page1产生了Page5。这样的好处是什么呢？官网的解释是可以增加并发读写，但是没有给出详细理由。于是这只能自己琢磨，假设有多人同时修改数据，如果不用多版本，那就必须使用锁，有锁就有先来后到，这无疑是降低并发性的。而用多版本，则在很大程度上解决了这个问题。并发的读写可能会在不同版本进行，由此可以进行无锁读写。 
   到这里，B-tree，Page，Chunk已经大致说清楚了，剩下最后一个问题，这里的Key和Value是啥。H2里的Key和Value都是Value，存在org.h2.value 里。每一种Value继承自Value抽象类，比如Long类型，就是为ValueLong等等。总的包括int，short，long，double，float，string，decimal，blob,datetime多种类型。基本类型的组合是ValueArray来统一各种Value。ValueArray是row的实际值。另外，由于java没有sizeof运算符，对每种类型还得分别把其实际大小写入Value类中，从而使得数据库使用的内存可以计算和控制。

————————————————
版权声明：本文为CSDN博主「HappySkaikai」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
原文链接：https://blog.csdn.net/kaixin89/java/article/details/50738486





之前，研究H2源码的时候，重点了解了一下MvStore。结合它的文档和Inoodb的实现，发现它叫MVCC(Multi-Version Concurrency Control)，即多版本控制，同时它也叫乐观锁。后面发现它的理念在Java的CopyOnWriteList，Clojure的标识与状态分离，惊奇地发现它似乎无处不在，于是，试着对此进行总结。

相对这些名词“乐观锁”比较合其神，所谓乐观，与“悲观锁”对应。它们都是针对并发问题提出的。试想如果小D有一份文件，同时有任意个人同时去读它是没有问题的。他们可以围着一起看。但是要同时写呢？那就只能一个一个来写，于是就有了锁。普通的锁也叫“悲观锁”，大意是当需要去写这个文件时，其他人只能候着，也就是连看都不能看。这很讨厌，其他的人可能只想同时瞅一眼，但要采取这种策略只能候着。那能不能改呢？小D想了一种方法，就是给一份大家看，给另一份供人写，写的人也只能一个一个写，但是大家同时是可以看了。当有人去写好时，就把这份文件复制一份替换原来那份给人看的文件。这就是所谓的CopyOnWrite，也就是写了之后就复制。Java的CopyOnWriteList就是这样的思想。那么是不是还可以改进，可以更乐观点呢，于是小D想，干脆谁想用每人打印一份算了。这样你写自己在自己文件上写，这样每个文件就是一个版本，出现了多个版本，但这时又有问题了，文件只允许一个版本是有效的，于是，小D想了个办法，设置了一个东西叫版本号。开始是0，大家打印的都是0。有人修改之后，提交成功就变成了1。好多人同时修改，一拥而上。总有人是先修改的，于是那个人就提交成功了，剩下的人惊奇的发现他们的版本小于现有版本，数据作废，于是只能把新版数据拿回去重改。这样有了多个版本，这种策略就是MVCC。因为它对数据是乐观的，大家都可以同时改嘛，最后一致就可以了。所以也叫乐观锁。

这种乐观如果遇上“读多于写”，这是非常合适的。但如果“写”比较多，那可能出现不断有事务重复执行。数据库的很多应用中读都是远多于写。H2的MvStore，MySQL的InnoDB都实现了这种策略。而作为支持并发良好的Clojure，为了实现并发的控制，实现了Ref变量，也使用类似的策略，实现了除了D之外所有数据库ACI特性。各种MVCC的实现是不同的，下一步需要继续研究细节和应用
————————————————
版权声明：本文为CSDN博主「HappySkaikai」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
原文链接：https://blog.csdn.net/kaixin89/java/article/details/50951143