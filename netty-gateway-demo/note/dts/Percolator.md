percolator简介
Percolator 是 Google 的上一代分布式事务解决方案。
构建在 BigTable 之上，在 Google 内部 用于网页索引更新的业务，原始的论文在此。
http://research.google.com/pubs/pub36726.html
原理比较简单，总体来说就是一个经过优化的二阶段提交的实现，进行了一个二级锁的优化。

### percolator多行数据的原子性
想保证多条指令作为整体执行的原子性，有单机并发编程基础的都应该知道，那就是通过加锁(原子操作不适合)，
要么你基于内存的mutex，要么基于文件系统的file lock，甚至于可以使用分布式锁服务。。。

Percolator的做法是对数据**扩展出一个lock列**。
对于多行数据，它随机选择一行作为主，通过BigTable的事务对这个primary主row的lock列写入信息表明持有锁，
而其他的rows则随后在其lock列写入primary锁的位置信息。

这样就完成了两件事：
1. 一个是把事务中的所有rows关联起来了；
2. 一个是互斥点唯一了，都在primary，如同加了分布式锁一样。

另外还需要考虑一个问题，Percolator为什么不怕lock信息丢失呢？
因为BigTable底层是GFS，是多副本，假定其能保证对外承诺的SLA下的可靠性，真发生了丢数据的问题那就人工处理呗。。。

### percolator 数据结构
Percolator在每行数据上抽象了五个COLUMN，其中三个跟事务相关:lock,data,write

1. lock column

    事务产生的锁，未提交的事务会写本项，记录primary lock的位置。**事务成功提交后，该记录会被清理**。
    
    >记录内容格式：{key,start_ts} -> {primary_key,lock_type,...}
    
    - key: 数据的key​
    - start_ts: 事务开始时间戳​
    - primary_key: 事务primary引用
    
    在执行Percolate事务时，会从待修改的keys中选择一个(随机选择)作为 primary_key  ，其余的则作为 secondaries 
    
2. data column

    存储实际用户数据
    
    > 数据格式为: {key,start_ts} -> {value} ​        
     
3. write column

    已提交的数据信息，存储数据所对应的时间戳。
    
    > 数据格式 {key,commit_ts} -> {start_ts} ​                
    
    可根据key + start_ts在DATA COLUMN中找到数据value
    
 关键在于write column，只有该列正确写入后，事务的修改才会真正被其他事务可见。
 读请求会首先在该column中寻找最新一次提交的start_timestamp，这决定了接下来从data column的哪个key读取最新数据。
 
 
### Snapshop isolation
Percolator 使用时间戳记维度实现数据的多版本化从而达到了snapshot isolation，优点是：
- 对于读：读操作都能够从一个带时间戳的稳定快照获取
- 对于写：较好地处理写-写冲突：若事务并发更新同一个记录，最多只有一个会提交成功

以下面3个事务为例：

0----------------------------------------------------->时间线

事务1.......s1----------c1

事务2...............s2------------c2

事务3........................................s3---------c3


快照隔离的事务均携带两个时间戳：
- s:start_ts, 
- c: commit_ts 。

上面3个事务中：
- start_ts2<commit_ts1,所以事务1的更新对于事务2不可见
- 事务3可以看到事务1和事务2的所有信息
- 事务1和事务2如果并发执行同一条数据，至少有一个失败

### 事务流程 
每一个事务开始的时候都会去授时服务（保证整体的时间是一致的）获取一个start_ts。

- 写过程

    定义一个辅助函数pre_write：某行数据的**pre_write**,其实就是**tyy lock阶段**
    1. 冲突检查阶段，检测write列和lock列与本次事务是否冲突，如果冲突则直接取消本次事务
        
        >冲突检查：
        >1. 从write 列中获取当前key的最新数据。
            若其commit_ts大于等于start_ts，说明在该事务的更新过程中其他事务提交过对该key的修改，返回WriteConflict错误
        >2. 检查key是否已被锁，如果是，返回KeyIsLock的错误
                
    2. 锁写入阶段，以start_ts对该行数据的lock列写入信息并写入data 
        
        >3. 向lock 列写入 {start_ts,key}->{primary_ref} 为当前key加锁。
        >   若当前key是primary key，primary_ref标记为primary。
        >   若当前key为secondary key，primary_ref则标记为指向primary的信息
        >4. 向data列写入数据 ​{key,start_ts}->{value}
       
    写主要分两个阶段：
    1. 一个是客户端向Percolator SDK的事务对象写入mutations（主要是update/delete）的阶段(Percolator SDK会缓存所有的mutations)
    2. 一个是客户端本身的commit阶段(即调用Percolator SDK的事务对象的commit)。
    
    Percolator SDK中事务对象的commit分为两个阶段，即分布式事务的2PC：
    1. 选出primary，对primary执行pre_write，失败返回
    2. 对所有其他rows执行pre_write，失败返回
    3. 客户端发送commit请求到primary key所在的存储节点，带上start_ts,并再获取一个commit_ts，转到步骤4
    4. primary key所在的存储节点进行commit请求处理：
        1. 检查lock的合法性。
            - lock列start_ts小于write列的start_ts。不合法的lock，处理失败。
            - 是不是有自己时间戳加的锁，如果有对primary解锁(解锁指的是删除lock列)，且write列有当前start_ts。
                表示事务其实前面已经处理完成了，不需要重复处理，直接转到步骤5；
            - 如果没有lock列没有start_ts加的锁，则事务失败。
        2. write列写入{key,commit_ts}->{start_ts},标识事务完成（后面可以通过start_ts和key找到相应版本的data列的value）
        3. 从lock列中删除key的锁记录以释放锁
    5. primary commit如果成功则并行异步对其他rows执行4的处理
   
   在某些实现中（如TiDB），Commit阶段并非并行执行，而是先向primary节点发起commit请求，成功后即可响应客户端成功且后台异步地再向 [secondaries] 发起commit。 
    
- 读过程

    读本身很简单，这里就简单说了。
    
    先获取当前的时间戳start_ts，**如果时间戳之内的数据有锁，则等一个超时时间，等到一定程度还不行就清理掉锁，执行读操作**。
    
    > 这里不可以直接返回当前小于start_ts的最新版本数据，会造成幻读。
    
    因为如果当前读不等锁，再次读的话可能锁释放数据提交了，这样就产生了不可重复读或者幻读。     
    

- FAQ
    
    1. 为什么写提交的时候primary成功了其他的row异步就可以？
    > 因为其他row的lock列都指向了primary，primary决议完怎么做之后，其他rows就都知道该怎么处理了。
    当其他rows面临访问的时候，如果当前有lock，则去看看primary怎么处理的进而对自己执行abort或commit
    (访问primary的lock和write列就知道该怎么做了)
    
    2. 为什么client作为协调者挂了无所谓？
    > client挂了其事务也无非是完成/未完成的状态，无论哪种状态都不会导致数据不一致，仔细想想Percolator的lock和commit机制就都明白了。
    
    3. **为什么读的时候如果等到一定程度还等不到无锁就可以主动清理锁以继续**？
    > 这和2其实是一个问题，只不过回答的时候额外多了一个考虑，那就是线性一致性的考虑。
    
    为什么这里不会影响到用户视角的数据的线性一致性呢？
    因为计算机的世界但凡涉及网络的请求都是3中情况：成功、失败和不知道。
    
    读等不及了要主动清理锁无非是下面2种情况:
    - a. 写的协调者挂了。那对于发起写的用户结果就是不可预知，他需要后续自己发请求确认到底执行的如何。
    - b. 写的协调者hang了。清理锁之后如果写协调者又活过来了，其执行commit的时候也必然失败，
        因为它持有的版本号的lock已经没了，只能取消，不会影响一致性。
    
    4. Percolator会有write skew问题吗？
    会。
    
    
### 异常处理-清理锁

在Prewrite阶段检测到锁冲突时会直接报错
（读时遇到锁就等直到锁超时或者被锁的持有者清除，**写时遇到锁，直接回滚然后给客户端返回失败由客户端进行重试**）。

锁清理是在读阶段执行。有以下几种情况时会产生垃圾锁：
1. Prewrite阶段：部分节点执行失败，在成功节点上会遗留锁
2. Commit阶段：Primary节点执行失败，事务提交失败，所有节点的锁都会成为垃圾锁
3. Commit阶段：Primary节点执行成功，事务提交成功，但是在secondary节点上异步commit失败导致遗留的锁
4. 客户端奔溃或者客户端与存储节点之间出现了网络分区造成无法通信

对于前三种情况，客户端出错后会主动发起Rollback请求，要求存储节点执行事务Rollback流程。这里不做描述。
对于最后一种情况，事务的发起者已经无法主动清理，只能依赖其他事务在发生锁冲突时来清理
  

#### Percolator采用延迟处理来释放锁
事务A运行时发现与事务B发生了锁冲突，A必须有能力决定B是一个正在执行中的事务还是一个失败事务。

因此，问题的关键在于
> 如何正确地判断出lock column中的锁记录是属于当前正在处于活跃状态的事务还是其他失败事务遗留在系统中的垃圾记录 ？

梳理事务的Commit流程一个关键的顺序是：
事务Commit时：
1. 检查其锁是否还存在；
2. 先向write column写入记录再删除lock column中的记录。

假如事务A在事务B的primary节点上执行，它在清理事务B的锁之前需要先进行锁判断：
- 如果事务B的锁已经不存在（事实上，如果事务B的锁不存在，事务A也不会产生锁冲突了），那说明事务B已经成功提交。
- 如果事务B的primary lock还存在，说明事务没有成功提交，此时清理B的primary lock。

假如事务A在事务B的secondary 节点上执行，如果发现与事务B存在锁冲突，那么它需要判断到底是执行Roll Forward还是Roll Back动作。
判断的方法是去Primary上查找primary lock是否存在：
- 如果存在，说明事务B没有成功提交，需要执行Roll Back：清理lock column中的锁记录；
- 如果不存在，说明事务已经被成功提交，此时执行Roll Forward：在该secondary节点上的write column写入内容并清理lock column中的锁记录。

几种情形分析：
1. 节点作为Primary在事务B的commit阶段写write column成功，但是删除lock column中的锁记录失败。
如果是由于在写入过程中出现了进程退出，那么节点在重启后可以恢复出该事务并删除lock column

2. 节点作为Primary在事务B的commit阶段写write column失败：意味事务B提交失败，那么事务A可以直接删除事务B在lock column中的锁记录

3. 节点作为Secondary在事务B的commit阶段写write column成功，但是清理lock column锁失败，因为在事务commit的时候先向primary节点发起commit，
因此，进入这里必然意味着primary节点上commit成功，即primary lock肯定已经不存在，因此，直接执行Roll Forward即可。 

    
###  tidb实现
TiDB 的事务模型沿用了 Percolator 的事务模型。 总体的流程如下：

- 读写事务

    事务提交前，在客户端 buffer 所有的 update/delete 操作。
    - Prewrite 阶段:
        1. 首先在所有行的写操作中选出一个作为 primary，其他的为 secondaries。

            Prewrite: 对 Row 写入lock列(上锁)，Lock 列中记录本次事务的开始时间戳。写入 Lock 列前会检查:
            是否已经有别的客户端已经上锁 (Locking)。
            是否在本次事务开始时间之后，检查 write 列，是否有更新 [startTs, +Inf(超时时间) 的写操作已经提交 (Conflict)。
            在这两种种情况下会返回事务冲突。否则，就成功上锁。将行的内容写入 row 中，时间戳设置为 startTs。

        2. 将 primaryRow 的锁上好了以后，进行 secondaries 的 prewrite 流程:
            1. 类似 primaryRow 的上锁流程，只不过lock的内容为事务开始时间start_ts及 primaryRow 的 Lock 的信息(主要是位置信息)。
            2. 检查的事项同 primaryRow 的一致。
            3. 当锁成功写入后，写入 row，时间戳设置为 startTs。

        以上 Prewrite 流程任何一步发生错误，都会进行回滚：删除 Lock，删除版本为 startTs 的数据。

    - 当 Prewrite 完成以后，进入 Commit 阶段，当前时间戳为 commitTs，且 commitTs> startTs :

        1. commit primary： write 列写入新数据，时间戳为 commitTs，内容为 startTs，表明数据的最新版本是 startTs 对应的数据。
        2. 删除lock列。
        
        如果 primary row 提交失败的话，全事务回滚，回滚逻辑同 prewrite。如果 commit primary 成功，
        则可以异步的 commit secondaries, 流程和 commit primary 一致， 失败了也无所谓。

- 事务中的读操作

    检查该行是否有 lock 列，时间戳为 [0, startTs]，如果有，表示目前有其他事务正占用此行，
    如果这个锁已经超时则尝试清除，否则等待超时或者其他事务主动解锁。
    
    注意此时不能直接返回老版本的数据，否则会发生幻读的问题。
    读取至 startTs 时该行最新的数据，方法是：读取 W 列，时间戳为 [0, startTs], 获取这一列的值，
    转化成时间戳 t, 然后读取此列于 t 版本的数据内容。
    
    由于锁是分两级的，primary 和 seconary，只要 primary 的行锁去掉，就表示该事务已经成功 提交，
    这样的好处是 secondary 的 commit 是可以异步进行的，只是在异步提交进行的过程中 ，
    如果此时有读请求，可能会需要做一下锁的清理工作。
    
    
### tidb 锁冲突
在两阶段提交的 Prewrite 阶段，TiDB 会对目标 key 分别上 primary lock 和 secondary lock。
在冲突严重的场景中，会出现写冲突 (write conflict)、keyislocked 等报错。具体而言，这个阶段可能会遇到的锁相关的报错信息如下。

#### 读写冲突
在 TiDB 中，读取数据时，会获取一个包含当前物理时间且全局唯一递增的时间戳作为当前事务的 start_ts。
事务在读取时，需要读到目标 key 的 commit_ts 小于这个事务的 start_ts 的最新的数据版本。

当读取时发现目标 key 上存在 lock 时，因为无法知道上锁的那个事务是在 Commit 阶段还是 Prewrite 阶段，
所以就会出现读写冲突的情况，如下图：
![](read-write-conflict1.png)

分析：

Txn0 完成了 Prewrite，在 Commit 的过程中 Txn1 对该 key 发起了读请求，
Txn1 需要读取 start_ts > commit_ts 最近的 key 的版本。此时，Txn1 的 start_ts > Txn0 的 lock_ts，
需要读取的 key 上的锁信息仍未清理，故无法判断 Txn0 是否提交成功，因此 Txn1 与 Txn0 出现读写冲突。

你可以通过如下两种途径来检测当前环境中是否存在读写冲突：

处理建议：
- 在遇到读写冲突时会有 backoff 自动重试机制，如上述示例中 Txn1 会进行 backoff 重试，单次初始 100 ms，单次最大 3000 ms，总共最大 20000 ms
- 可以使用 TiDB Control 的子命令 decoder 来查看指定 key 对应的行的 table id 以及 rowid：


悲观锁
在 v3.0.8 之前，TiDB 默认使用的乐观事务模式会导致事务提交时因为冲突而失败。
为了保证事务的成功率，需要修改应用程序，加上重试的逻辑。
悲观事务模式可以避免这个问题，应用程序无需添加重试逻辑，就可以正常执行。

TiDB 悲观锁复用了乐观锁的两阶段提交逻辑，重点在 DML 执行时做了改造。

在两阶段提交之前增加了 Acquire Pessimistic Lock 阶段，简要步骤如下。

1. （同乐观锁）TiDB 收到来自客户端的 begin 请求，获取当前版本号作为本事务的 StartTS。
2. TiDB 收到来自客户端的更新数据的请求：TiDB 向 TiKV 发起加悲观锁请求，该锁持久化到 TiKV。
3. （同乐观锁）客户端发起 commit，TiDB 开始执行与乐观锁一样的两阶段提交。

**如果出现非常频繁，需要调整业务代码来降低死锁发生概率。**


### 优缺点分析
- 优点

- 缺点

### percolator 时钟支持
tidb时钟支持

tidb Placement Driver (简称 PD) 是整个集群的管理模块，其主要工作有三个：
1. 存储集群的元信息（某个 Key 存储在哪个 TiKV 节点）；
2. 对 TiKV 集群进行调度和负载均衡（如数据的迁移、Raft group leader 的迁移等）；
3. 提供时间服务。

TiDB的时间服务是由PD提供的，使用的是中心授时服务。

PD的TSO使用的是中心式的混合逻辑时钟。其使用64位表示一个时间，其中低18位代表逻辑时钟部分，
剩余部分代表物理时钟部分，其结构如下图所示。

|<----------- 48位 ------------->|<--------18位---------->|

|---------物理时钟部分----------|------逻辑时钟部分---|

由于其逻辑部分为18位，因此理论上每秒可以分配时间戳为 2^18 * 1000 = 262144000个，即每秒可以产生2.6亿个时间戳。

PD采用了中心式的时钟解决方案，本质上还是混合逻辑时钟。但是由于其是单点授时，所以是全序的。
中心式的解决方案实现简单，但是跨区域的性能损耗大，因此实际部署时，会将PD集群部署在同一个区域，避免跨区域的性能损耗；

PD通过引入etcd解决了单点问题，一旦Leader节点故障，会立刻选举新的Leader继续提供服务；
而由于TSO服务只通过PD的Leader提供，所以可能会出现性能瓶颈，但是理论上PD每秒可以产生2.6亿个时间戳，
并且经过了很多优化，从目前使用情况看，TSO并没有出现性能瓶颈。
