作者：bluesky

链接：https://zhuanlan.zhihu.com/p/145364298



### 2PC

两阶段提交（2 Phase Commit简称2PC）协议是用于在多个节点之间达成一致的通信协议，它是实现“有状态的”分布式系统所必须面对的经典问题之一。本文通过对比经典2PC协议，和Google工程实践的基础上，分析一种优化延迟的2PC协议。为了方便说明，本文主要针对分布式数据库中，跨域sharding的2PC方案的讨论。主要参考文献：Gray J, Lamport L. Consensus on transaction commit[J]. ACM Transactions on Database Systems (TODS), 2006, 31(1): 133-160.

- 经典两阶段提交概述

- - 先来回顾下经典的2PC协议，有两个角色：一个协调者（coordinator）和若干参与者（participant），协议执行可以分为如下几个阶段：

  - - 预处理阶段：严格来说，预处理阶段并不是2PC的一部分，在实际的分布式数据库中，这个阶段由协调者向若干参与者发送SQL请求或执行计划，包括获取行锁，生成redo数据等操作。

    - Prepare阶段：客户端向协调者发送事务提交请求，协调者开始执行两阶段提交，向所有的事务参与者发送prepare命令，参与者将redo数据持久化成功后，向协调者应带prepare成功。这里隐含的意思是，参与者一旦应答prepare成功，就保证后续一定能够成功执行commit命令（redolog持久化成功自然保证了后续能够成功commit）。

    - Commit阶段

    - - 执行Commit：协调者收到所有参与者应答prepare成功的消息后，执行commit，先在本地持久化事务状态，然后给所有的事务参与者发送commit命令。参与者收到commit命令后，释放事务过程中持有的锁和其他资源，将事务在本地提交（持久化一条commit日志），然后向协调者应答commit成功。协调者收到所有参与者应答commit成功的消息后，向客户端返回成功。
      - 执行Abort：prepare阶段中如果有参与者返回prepare失败或者超时未应答，那么协调者将执行abort，同样先在本地持久化事务状态，然后给所有参与者发送abort命令。参与者收到abort命令后，释放锁和其他资源，将事务回滚（有必要的情况下还要持久化一条abort日志）。



- - 经典2PC的局限

  - - 协调者宕机：2PC是一个阻塞式的协议，在所有参与者执行commit/abort之前的任何时间内协调者宕机，都将阻塞事务进程，必须等待协调者恢复后，事务才能继续执行。
    - 交互延迟：协调者要持久化事务的commit/abort状态后才能发送commit/abort命令，因此全程至少2次RPC延迟（prepare+commit），和3次持久化数据延迟（prepare写日志+协调者状态持久化+commit写日志）。

#### 2pc的缺点

二阶段提交看似能够提供原子性的操作，但它存在着严重的缺陷

- **网络抖动导致的数据不一致：** 第二阶段中`协调者`向`参与者`发送`commit`命令之后，一旦此时发生网络抖动，导致一部分`参与者`接收到了`commit`请求并执行，可其他未接到`commit`请求的`参与者`无法执行事务提交。进而导致整个分布式系统出现了数据不一致。
- **超时导致的同步阻塞问题：** `2PC`中的所有的参与者节点都为`事务阻塞型`，当某一个`参与者`节点出现通信超时，其余`参与者`都会被动阻塞占用资源不能释放。
- **单点故障的风险：** 由于严重的依赖`协调者`，一旦`协调者`发生故障，而此时`参与者`还都处于锁定资源的状态，无法完成事务`commit`操作。虽然协调者出现故障后，会重新选举一个协调者，可无法解决因前一个`协调者`宕机导致的`参与者`处于阻塞状态的问题。



#### Percolator系统的两阶段提交

- - 概述：percolator是google基于bigtable实现的分布式数据库，在bigtable单行事务的基础上，它使用全局的Timestamp Server来实现分布式的mvcc（后续专门讨论，本文不展开了）；还有2PC协议来实现多行事务。由于bigtable屏蔽了数据sharding的细节，在percolator看来事务修改的每一行记录，都被看作一个参与者，事务没有区分预处理和prepare阶段，可以认为事务开始后，即进入了2PC的prepare阶段。
    percolator的2PC协调者并不持久化状态，而是引入primary record的概念，将事务执行过程中修改的第一个record作为primary record，在这个record中记录本次事务的状态，而在事务执行过程中其他被修改的record里面记录primary record的key（这里我觉得priamry record保存单独的表中更优雅，否则priamry record被用户删除的话，并不好处理）。在commit阶段，先在primary record中记录事务状态（包括事务ID，mvcc版本号等），成功后，才将各个参与者的修改提交（包括持久化mvcc版本号，释放行锁等）。在事务执行过程中，如果协调者宕机，那么其他参与者可以通过查询primary record中保存的事务状态来决定回滚或提交本地的修改。

  - 创新与局限：在仅提供行级事务的bigtable基础上，percolator创新的实现了多行事务和mvcc，primary record的设计简化了2PC协议中对协调者状态的维护，是一套比较优雅的2PC工程实现。但是直接构建在KV基础上的数据库事务，也存在着诸多局限：

  - - 底层KV屏蔽了sharding细节，且不提供交户型的事务上下文机制，对存储引擎的读写只能在一次RPC提交，**使得加锁、修改、提交都必须是一次bigtable的提交操作**，延迟代价是巨大的。
    - 尽管primary record的设计简化了2PC的协调者状态维护，但是commit时仍然要等待primary record持久化事务状态成功后，参与者才能进行commit，这一次延迟不可避免。



- 2PC协议优化

- - 通过对经典2PC和percolator实现的分析，可以得到如下几个对2PC的改进思路：

  - - 底存存储需要暴露sharding细节，提供以分区为单位的事务上下文管理机制，使得在预处理过程中，行锁和数据修改为内存操作，避免持久化的代价。
    - 简化协调者为无状态逻辑
    - 减少2PC执行**关键路径**上的持久化和RPC次数



- - 优化的2PC协议：

  - - 预处理阶段：协调者向若干参与者发送SQL请求或执行计划，一个sharding即对应一个参与者，针对这个事务，在每个参与者中会维护一个通过事务ID索引的事务上下文，**用于维护行锁、redo数据等**，有必要的情况（redolog过多）下，**这个阶段可能会异步的持久化redolog。**
    - Prepare阶段：协调者收到客户端提交事务的请求，向各个参与者发送prepare命令，命令中携带了当前事务的参与者列表，参与者收到prepare命令后，**将事务的redolog、参与者列表、prepare日志持久化后**，**向协调者和其他参与者发送prepare成功的消息。**
    - Commit阶段：协调者收到所有参与者应答prepare成功的消息后，即**向客户端返回事务提交成功**；对于每个参与者，当它确认所有参与者都prepare成功后，将本地事务提交并释放行锁等资源，并异步的持久化一条commit日志，然后向其他参与者发送commit成功的消息。
    - Finish阶段：对于每个参与者，当它确认所有参与者都commit成功后，将本地事务上下文释放，并异步的持久化一条finish日志。

### 3PC

3PC(three phase commit)即三阶段提交[6][7]，既然2PC可以在异步网络+节点宕机恢复的模型下实现一致性，那还需要3PC做什么，3PC是什么鬼？

 

在2PC中一个participant的状态只有它自己和coordinator知晓，假如coordinator提议后自身宕机，在watchdog启用前一个participant又宕机，其他participant就会进入既不能回滚、又不能强制commit的阻塞状态，直到participant宕机恢复。这引出两个疑问：

1. 能不能去掉阻塞，使系统可以在commit/abort前回滚(rollback)到决议发起前的初始状态
2. 当次决议中，participant间能不能相互知道对方的状态，又或者participant间根本不依赖对方的状态

coordinator接收完participant的反馈(vote)之后，进入阶段2，给各个participant发送准备提交(prepare to commit)指令。participant接到准备提交指令后可以锁资源，但要求相关操作必须可回滚。coordinator接收完确认(ACK)后进入阶段3、进行commit/abort，3PC的阶段3与2PC的阶段2无异。协调者备份(coordinator watchdog)、状态记录(logging)同样应用在3PC。

 

participant如果在不同阶段宕机，我们来看看3PC如何应对：

- **阶段1**: coordinator或watchdog未收到宕机participant的vote，直接中止事务；宕机的participant恢复后，读取logging发现未发出赞成vote，自行中止该次事务
- **阶段2**: coordinator未收到宕机participant的precommit ACK，但因为之前已经收到了宕机participant的赞成反馈(不然也不会进入到阶段2)，coordinator进行commit；watchdog可以通过问询其他participant获得这些信息，过程同理；宕机的participant恢复后发现收到precommit或已经发出赞成vote，则自行commit该次事务
- **阶段3**: 即便coordinator或watchdog未收到宕机participant的commit ACK，也结束该次事务；宕机的participant恢复后发现收到commit或者precommit，也将自行commit该次事务

因为有了准备提交(prepare to commit)阶段，3PC的事务处理延时也增加了1个RTT，变为3个RTT(propose+precommit+commit)，但是它防止participant宕机后整个系统进入阻塞态，增强了系统的可用性，对一些现实业务场景是非常值得的。



### 2pc和3pc对比

相比较2PC而言，**3PC对于协调者和参与者都设置了超时时间**，而**2PC只有协调者才拥有超时机制**。这解决了一个什么问题呢？这个优化点主要是避免了参与者在长时间无法与协调者通讯的情况下（协调者挂掉了），无法释放资源阻塞的问题。

但是相应的，正因为引入了参与者的超时机制，也导致了3PC协议在第二三阶段很容易出现数据不一致的问题。

简单来说2PC是一个数据强一致性协议，而3PC通过弱化数据的一致性来解决阻塞的问题。



3pc通过以下手段降低了阻塞：

- 参与者返回 CanCommit 请求的响应后，等待第二阶段指令，若等待超时，则自动 abort，降低了阻塞；

- 参与者返回 PreCommit 请求的响应后，等待第三阶段指令，若等待超时，则自动 commit 事务，也降低了阻塞；

**注意， 数据不一致问题仍然可能是存在，3pc这个算法并没有完美解决数据不一致问题。**



3PC工作在同步网络模型上，它假设消息传输时间是有上界的，只存在机器失败而不存在消息失败。这个假设太强，现实的情形是，机器失败是无法完美地检测出来的，消息传输可能因为网络拥堵花费很多时间。同时, 说阻塞是相对, 存在协调者和参与者同时失败的情形下, 3PC事务依然会阻塞。

> **实际上，很少会有系统实现3PC，多数现实的系统会通过复制状态机解决2PC阻塞的问题**。

3PC并没有完美解决2PC的阻塞，也引入了新的问题。比如，如果失败模型不是失败-停止, 而是消息失败（消息延迟或网络分区），那样3PC会产生不一致的情形。

作者：邓小闲

链接：https://www.zhihu.com/question/422691164/answer/1496496450

