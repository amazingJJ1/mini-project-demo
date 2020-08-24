# Parallel Raft
作者：朱元
链接：https://www.zhihu.com/question/278984902/answer/494778859

**raft 的leader其实本来就是可以安全的、不等待前面请求结束、就发起后面的状态机日志复制请求的。**

   因为， 当这堆复制请求中某一个达成多数一致（quorum，下同）时，必有前提：前面的每一个请求都已经达成quorum了。
原因是同意了这个日志复制请求rpc的follower，必然已经同意了比这次请求更早的, leader已知的,
所有日志复制请求(这个请求和以前的请求会核对双方的prevLogIndex，那么根据数学归纳法可得出这个结论)。
所以前面的请求，同意的节点数量不可能比现在这个请求少。
 
   所以leader标记这个请求可持久化执行时，前面的其实也都可以持久化了（前面的请求一致性完备性已经达成，只是对leader的可见性发生了乱序）；
这段时间内哪怕有个follower此时跑过来回复leader的term比自己小，leader把自己的身份转成follower后依然可以安全的完成这所有的持久化并都给客户端回复成功：
新leader迟早会把自己未广播完整的历史继续广播让所有节点持久化的！！

quorum不可否定原则就是这么牛（可惜raft原始论文没提这个：在raft中，每一个quorum的达成都是（term, logindex）为元素的lamport时间戳全序链的不可逆转的延长）！！
只要有某个日志达成quorum，在它之前自己的日志都达成quorum了（quorum不可否定原则和达成quorum的前置条件的数学归纳），
哪怕自己此时已经不是leader了（新leader必然包含这段历史，迟早会有节点来要求你序列化那些数据）！！

这个答案其实暗含在Raft的commitIndex定义中： 
**index of highest log entry known to be committed (initialized to 0, increases monotonically)**
只需要管最大的就好，前面的一定是committed或者to be committed，**也暗含在论文末尾要求新任leader要先发一个空白请求那里**。

   不仅请求可以不等待前面请求结束就发起后面的，回复也可以提前甚至并行发起：
只要leader只要看到一个复制请求达成quorum，立即就可以认定前面所有自己还不知道是否达成quorum的请求已经达成quorum。
按照顺序把他们apply持久化并且回复操作成功即可。如果这些状态机变更都是写请求，此时并行回复他们写成功也可以，
**只是下次过来的读请求要等到这些写请求持久化全部结束才能发起读并回复，因为并行的写请求回复导致不能读取这些写请求之间的快照版本**。

所以这里需要你在读写效率，读写事务级别中做个抉择了（你愿意读脏吗（读不等待或者你读带cookie过来）？
你愿意写完成请求的返回顺序和执行顺序不同吗（如果并行返回写完成而不是依执行顺序返回）？）。
然而实际要实现这样的高吞吐，需要follower需要为可能的接收乱序（例如follower和leader使用udp通信，或者为每个rpc使用各自的tcp连接来通信）
做一些实现上的等待或缓冲：
    follower在接受到一个日志复制请求时如果核查prevLogIndex不通过，不要马上回绝并让leader给自己发老数据。。
    因为这可能仅仅是一个接收乱序甚至用户态里epoll处理中的乱序而已，并不一定就是前面的请求丢包或者leader变更要回溯了。
    网络中乱序毕竟是很少的；而内核缓冲区对用户态epoll_wait的可见性其实是不会乱的，只是你epoll_wait里结果中的活动fd未必按照fd的激活顺序来排列而已。
如果follower和leader仅使用2个tcp连接来做单工pipe双向通信则完全不会有这事（记得把收发缓冲区设大一些）；
否则推荐follower发现prevLogIndex不通过时，至少等这轮epoll_wait的结果处理完之后再看看是否不match prevLogIndex再请求老数据。
如果不这样做的话，leader很容易因为前面的请求follower暂未收齐而导致后面的请求被拒，还被要求重发已经发送的请求。
我不知道是否因为raft原始论文没有提**leader可并行发起他的一切请求**这个事（基于：在新leader选出来之前，达成了quorum的（term, logindex），
所构成的lamport时间戳全序链，一定是leader日志序列的子集），导致很多人认为Raft不能并行，，事实上raft不仅能并行，
而且可并行度相当的高（只要你能把raft的2个rpc都能用paxos解释清楚，你可以安全的定义各个并行实施点）。
鄙人最近可能也要开源一个network raft实现，能给leader的执行减负（因为并行度一高太容易把leader网卡给打满了）。

   对应一下我看到的这个资料提到的一些点：
   源自：面向云数据库，超低延迟文件系统PolarFS诞生了

- 乱序确认（ack）：
    当收到来自leader的一个log项后，Raft follower会在它及其所有之前的log项都持久化后，才发送ack。
    ParallelRaft则不同，任何log entry成功持久化后均能立即返回，这样就优化了系统的平均延迟。
    （raft原始论文要求log持久化其实是为了提升容灾性，让进程重启之后追日志有一个类似innodb的checkpoint,  
    这个checkpoint当然可以不用那么密集，，其实内存里达成quorum依然能对抗少于1/2节点的故障，只要follower内存存储log之前是检查过prevLogIndex的。
    所以其实原始Raft论文应当被理解为只要确认该log项是可以被持久化的就可以给leader回复的，因为即使该follower后面挂了再重启，
    也只会造成实际日志序列落后于leader所记录的你的日志序列，需要重新让leader给你补发一下丢失的日志序列而已）
    
- 乱序提交（commit）：
    Raft leader串行提交log项，一个log项只有之前的所有项提交之后才能提交。而ParallelRaft的leader在一个log项的多数副本已经确认之后即可提交。
    这符合存储系统的语义，例如，NVMe SSD驱动并不检查读写命令的LBA来保证并行命令的次序，对命令的完成次序也没有任何保证。
    （这一点我有些不太明白，根据Raft原文的术语定义：
    An entry is considered committed if it is safe for that entry to be applied to state machines（Figure-6） 
    The leader decides when it is safe to apply a log entry to the state machines; such an entry is called committed. (第7页第2行) 
     可以看到commit是一种状态，在Raft中等价于quorum，是被followers赞同而某个日志项得到的状态
     (A log entry is committed once the leader that created the entry has replicated it on a majority of the servers)，
     不应该是leader发起的对日志项的某种“操作”（不管leader是否“操作”，失活，都无法否认这个状态的存在）。
     从polardb的内容来看http://www.vldb.org/pvldb/vol11/p1849-cao.pdf，应该在文中被定义为leader持久化的标记某个log达成了quorum的意思。
     这个当然可以乱序，Raft原始论文的commitIndex定义：
      index of highest log entry known to be committed (initialized to 0, increases monotonically) 也说明了，
      只需要记录最大的committedIndex就好了，不需要每个日志都去记录，除非Polardb论文作者不认同本文第二段的结论 ）
      
- 乱序应用（apply）：
    对于Raft，所有log项都按严格的次序apply，因此所有副本的数据文件都是一致的。但是，ParallelRaft由于乱序的确认和提交，
    各副本的log都可能在不同位置出现空洞，这里的挑战是，如何保证前面log项有缺失时，安全地apply一个log项？
    （注意，"...各副本的log都可能在不同位置出现空洞" 他这里就在暗示我前面提到的前提“...只要follower内存存储log之前是检查过prevLogIndex的”并没有得到遵守，
    相反必然使用了别的数据结构去组织lamport时间戳而不是(term, logIndex)。此时Polardb论文作者不认同本文第二段的结论也就可以理解了）
    

果然 引文中下面就是ParallelRaft引入了一种新型的数据结构look behind buffer来解决apply中的问题。

1. ParallelRaft的每个log项都附带有一个look behind buffer。look behind buffer存放了前N个log项修改的LBA摘要信息。

2. look behind buffer的作用就像log空洞上架设的桥梁，N表示桥梁的宽度，也就是允许单个空洞的最大长度，
N的具体取值可根据网络连续缺失log项的概率大小，静态地调整为合适的值，以保证log桥梁的连续性。

3. 通过look behind buffer，follower能够知道一个log项是否冲突，也就是说是否有缺失的前序log项修改了范围重叠的LBAs。
没有冲突的log项能被安全apply。如有冲突，它们会被加到一个pending list，待之前缺失的冲突log项apply之后，才会接着apply。

    通过上述的异步ack、异步commit和异步apply，PolarFS的chunk log entry的写入和提交避免了次序造成的额外等待时间，从而有效缩减了高并发3副本写的平均时延。
    
（看起来是用跳表来代替了prevLogIndex 这个仅有的前向索引，组成的lamport时间戳序列有可能变成偏序分叉而不是全序时立即阻止。
可以看到我前文中提到的在追加日志过程中容忍乱序的过程被ParallelRaft放日志应用这里来解决了，
也就是说贪心的加日志（prevLogIndex并不当场做严格检查，这样quorum的速度可以不用依赖前面的请求的quorum，但是日志真的得次次都持久化了。。也许NVME能把这个赚回来），
但谨慎的应用日志，把可以在日志追加阶段回绝的请求延后以冲突解决。
    当然因为这个延后，给leader选举也带来了一些问题，文中进一步提到了我们在ParallelRaft的设计中，确保了Raft协议关键特性不丢失，从而保障了新协议的正确性。
    
1. ParallelRaft协议的设计继承了原有Raft协议的Election Safety、Leader Append-Only及Log Matching特性。
2. 冲突log会以严格的次序提交，因此协议的State Machine Safety特性能够最终得以保证。
3. 我们在Leader选举阶段额外引入了一个Merge阶段，填补Leader中log的空洞，能够有效保障协议的Leader Completeness特性。）

虽然看不到代码，但是我们可以看到设计的原则是把一些容易造成等待（如果真的发生了乱序），耗时的操作放到了RAFT 2阶段提交的最后一步（这一步毕竟是异步的），
并把负担加到leader选举这种后台的事情上去。相信可以在单次quorum耗时上获得一定的提升，如果follower响应追加日志请求既不等待前面的结果也不校验前面的结果
（无条件落地日志拷贝，强调quorum而忽略了follower上的logindex 的lamport时间戳连续性要求，所以新leader选举和上任必须要花点功夫来补课了，
没有看到论文中提到这样改造是否有加剧原始raft选举中可能发生的活锁的可能性）。

不过个人还是比较好奇，RDMA协议用户态驱动实现导致follower收到日志复制请求乱序的结果很频繁吗
（假如排除我上文中提到的类似epoll_wait使用中造成的处理乱序）？该论文中没有看到这个数据，
如果这个数据太低的话这个优化就恐怕不能达到论文中写耗时下降超过1半多那么大的幅度吧。

评论：polarfs能放松约束的原因是数据库的mtr会根据存储层的log水位保证事务正确性，相当于把一部分Sequential Consistency约束上移了。


------------------------

> 减少状态的改变是优化性能的关键，状态的改变恰恰是热力学熵增的过程。 
我们学习技术的目的也许是为了理解事物的规律，从而避免使用这些技术，例如如果使用好的设计从而避免产生同步，不使用spinlock的吞吐量一定是最大的，对么？

----------------------------

作者：AIDBA  
链接：https://zhuanlan.zhihu.com/p/56768089

PolarFS采用的是Raft协议，Raft是分布式强一致协议Paxos的一种简化实现。
Raft协议虽然容易理解与实现，不过它高度串行化的设计在高并发环境下的性能经常被人诟病，这个PolarFS论文中有论述，之前文章里面也有提到。
PolarFS在原有Raft协议上提出了一种改进型的一致性协议ParallelRaft。

先来回顾下Raft协议保证强一致的**关键特性**：
1. Leader选举安全特性：
    Raft 协议保证了在任意一个term内，最多只有一个 leader，新leader节点拥有以前term的所有已提交的日志。
    
2. 日志匹配原则：
    Raft保证每个副本日志append的连续性；当leader发送一个log entry给follower，follower需要返回ack来确认该log项已经被收到且记录，
    同时也隐式地表明所有之前的log项均已收到且保存完毕。
    
3. Leader只附加原则：
    Raft中leader日志的commit也是连续的；当leader commit一个log entry并广播至所有follower，它也同时确认了所有之前的log项都已被提交了。
    
Raft的性能瓶颈主要在于后两个约束，而ParallelRaft打破后两个限制的同时保证了第一点。

因此，ParallelRaft与Raft最根本的不同在于，**当某个log entry提交成功时，并不意味着之前的所有log entry都已成功提交**。(可能有空洞)

因此需要额外保证：

- 在这种情况下，单个存储的状态不会违反存储语义的正确性；（log的顺序性）
- 所有已提交的log entry在各种边界情况下均不会丢失；（log不丢）

ParallelRaft乱序日志复制和ParallelRaft的乱序执行遵循如下原则：
当写入的Log entry彼此的存储范围没有交叠，那么就认为Log entry无冲突可以乱序执行；
否则，冲突的Log entry将按照写入次序依次完成。（根据logIndex顺序）

依照此原则完成的I／O不会违反传统存储语义的正确性，而log的ack-commit流程因此也得到优化。

乱序确认（ack）：
当收到来自leader的一个log entry后，Raft follower会在它及其所有之前的log entry都持久化后，才发送ack。
ParallelRaft则不同，任何log entry成功持久化后均能立即返回，这样就优化了系统的平均延迟。

乱序提交（commit）：
Raft leader串行提交log entry，一个log entry只有之前的所有entry提交之后才能提交。
而ParallelRaft的leader在一个log entry的被多数副本确认之后即可提交。
对于Raft，所有log entry都按严格的次序apply，因此所有副本的数据文件都是一致的。

但是ParallelRaft由于乱序的确认和提交，各副本的log都可能在不同位置出现空洞，因此这会带来两个问题:
1. log entry存在空洞下状态机如何乱序应用（apply）？
    - ParallelRaft引入了一种新型的数据结构look behind buffer来解决乱序apply中的问题。ParallelRaft的每个log entry都附带有一个look behind buffer。
    - look behind buffer存放了前N个log entry修改的LBA(逻辑块地址)信息。look behind buffer的作用就像log空洞上架设的桥梁，N表示桥梁的宽度，
        也就是允许单个空洞的最大长度，N的具体取值可根据网络连续缺失log entry的概率大小，静态地调整为合适的值，以保证log桥梁的连续性。
    - 通过look behind buffer，follower能够知道一个log entry是否冲突，也就是说是否有缺失的前序log entry修改了范围重叠的LBAs。
        没有冲突的log entry才能被安全apply。如有冲突，它们会被加到一个pending list，待之前缺失的冲突log entry apply之后，才会接着apply。
        
 通过上述的异步ack、异步commit和异步apply，PolarFS的chunk log entry的写入和提交避免了顺序造成的额外等待时间，从而有效缩减了高并发3副本写的平均时延。
 
2. log entry空洞下如何保证Leader选举安全特性？
    虽然允许日志空洞带来性能的提升，但一旦 leader 挂了，集群重新出选主，就需要解决这些日志空洞的问题，保证leader日志完整性原则不被破坏。
    ParallelRaft在Leader选举阶段额外引入了一个Merge阶段来填补Leader中log的空洞。
    RarallelRaft采用与Raft类似的机制，具有相同term和index的log entry是相同的log entry。
    - 如果leader空洞缺的是已提交的log entry，那么一定可以从当前的Follower中找回来，因为已提交的log entry总是已经被集群中多数派确认接受。
      而这在ParallelRaft中也是如此，而多数派与Follower一定存在交集。
    - 如果缺的是未提交的log entry，可能这个未提交的log entry已经丢了，在Follower也找不到，那么这时候就直接填补一个空日志。
    - 也可能这个未提交的log entry 被多数派接受，那么这时候就要提交这个log entry 了，需要找到当前index下最大的term的log entry 填补空洞。
        也可能这个未提交的log entry未被多数派接受，那么也需要找到最大的term 的log entry 填补空洞。可以看出来这个过程其实就是一个basic paxos。
        
 Merge阶段完成之后，新leader节点拥有以前term的所有已提交的日志条目，成为真正的leader。
 在此之后，新leader 通过提交自己当前term的日志（推进 commit index）来实现隐式提交非当前term的日志，这一点与Raft一致。
 读过Raft paper的都应该对文中这段话有印象
 "In any leader-based consensus algorithm, the leader must eventually store all of the committed log entries.
  In some consensus algorithms, such as Viewstamped Replication , a leader can be elected even if it doesn’t initially contain all of the committed entries. 
  These algorithms contain additional mechanisms to identify the missing entries and transmit them to the new leader, 
  either during the election process or shortly afterwards.Unfortunately, this results in considerable additional mechanism and complexity." 
  
  Raft的作者设计Raft的初衷就是利用日志的连续性简化leader的选举过程。牺牲了一定的性能换来协议的简洁性，看来鱼和熊掌不能兼得！
  
  ParallelRaft协议正确性可以看到ParallelRaft的本身未做理论上的突破，通过改进并确保了Raft协议的**Election Safety**、**Leader Append-Only**
及**Log Matching**关键特性不丢失，保障了新协议的正确性。**不冲突的log可以乱序提交，冲突log会以严格的顺序提交**，
因此协议的State Machine Safety特性能够最终得以保证。
在Leader选举阶段额外引入了一个Merge阶段，填补Leader中log的空洞，能够有效保障协议的Leader Completeness特性。
有了Leader Completeness特性，那么相关的成员变更都可以按照Raft paper中的方式进行实现。