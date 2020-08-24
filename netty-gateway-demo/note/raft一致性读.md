# raft 一致性读

tikv举例：

Client 将请求发送到 Leader 后，Leader 将请求作为一个 Proposal 通过 Raft 复制到自身以及 Follower 的 Log 中，然后将其 commit。
TiKV 将 commit 的 Log 应用到 RocksDB 上，由于 Input（即 Log）都一样，可推出各个 TiKV 的状态机（即 RocksDB）的状态能达成一致。

但实际多个 TiKV 不能保证同时将某一个 Log 应用到 RocksDB 上，也就是说各个节点不能实时一致，
加之 Leader 会在不同节点之间切换，所以 Leader 的状态机也不总有最新的状态。
Leader 处理请求时稍有不慎，没有在最新的状态上进行，这会导致整个系统违反线性一致性。

好在有一个很简单的解决方法：
依次应用 Log，将应用后的结果返回给 Client。这方法不仅简单还通用，**读写请求都可以这样实现**。
这个方法依据 commit index 对所有请求都做了排序，
使得每个请求都能反映出状态机在执行完前一请求后的状态，可以认为 commit 决定了 R/W 事件发生的顺序。
Log 是严格全序的（total order），那么自然所有 R/W 也是全序的，将这些 R/W 操作一个一个应用到状态机，
所得的结果必定符合线性一致性。

这个方法的缺点很明显，性能差，因为所有请求在 Log 那边就被序列化了，无法并发的操作状态机。
这样的读简称 LogRead。由于读请求不改变状态机，这个实现就显得有些“重“，不仅有 RPC 开销，还有写 Log 开销。
优化的方法大致有两种：
- ReadReadIndex
- ReadIndexLease

相比于 LogRead，ReadIndex 跳过了 Log，节省了磁盘开销，它能大幅提升读的吞吐，减小延时（但不显著）。

Leader 执行 ReadIndex 大致的流程如下：
1. 记录当前的 commit index，称为 ReadIndex
2. 向 Follower 发起一次心跳，如果大多数节点回复了，那就能确定现在仍然是 Leader
3. 等待状态机**至少应用到 ReadIndex 记录的 Log**
4. 执行读请求，将结果返回给 Client

第 3 点中的“至少”是关键要求，它表明状态机应用到 ReadIndex 之后的状态都能使这个请求满足线性一致，
不管过了多久，也不管 Leader 有没有飘走。为什么在 ReadIndex 只有就满足了线性一致性呢？
之前 LogRead 的读发生点是 commit index，这个点能使 LogRead 满足线性一致，那显然发生这个点之后的 ReadIndex 也能满足。

**LeaseReadLeaseRead** 与 ReadIndex 类似，但更进一步，不仅省去了 Log，还省去了网络交互。
它可以大幅提升读的吞吐也能显著降低延时。
基本的思路是：
 >Leader 取一个比 Election Timeout 小的租期，在租期不会发生选举，确保 Leader 不会变，
  所以可以跳过 ReadIndex 的第二步，也就降低了延时。

LeaseRead 的正确性和时间挂钩，因此时间的实现至关重要，如果漂移严重，这套机制就会有问题。

**Wait Free**

 Lease 省去了 ReadIndex 的第二步，实际能再进一步，省去第 3 步。
 
这样的 LeaseRead 在收到请求后会立刻进行读请求，不取 commit index 也不等状态机。
由于 Raft 的强 Leader 特性，在租期内的 Client 收到的 Resp 由 Leader 的状态机产生，
所以只要状态机满足线性一致，那么在 Lease 内，不管何时发生读都能满足线性一致性。

有一点需要注意:
**只有在 Leader 的状态机应用了当前 term 的第一个 Log 后才能进行 LeaseRead。**
>因为新选举产生的 Leader，它虽然有全部 committed Log，但它的状态机可能落后于之前的 Leader，
状态机应用到当前 term 的 Log 就保证了新 Leader 的状态机一定新于旧 Leader，之后肯定不会出现 stale read。


----------------------------------------------------------------------------
其他思路：
Tikv **Follower Read**
本质和wait Free差不多，tikv实现思路是：
如何保证 Follower 上读到最新的数据呢？最土的办法就是将请求转发给 Leader，然后 Leader 返回最新的 Committed 的数据就好了嘛，Follower 当做 Proxy 来用。
这个思路没有任何问题，而且实现起来也很简单还安全。但是，很明显这个地方可以优化成：Leader 只要告诉 Follower 当前最新的 Commit Index 就够了，
因为无论如何，即使这个 Follower 本地没有这条日志，最终这条日志迟早都会在本地 Apply。

TiDB 目前的 Follower Read 正是如此实现的，当客户端对一个 Follower 发起读请求的时候，这个 Follower 会请求此时 Leader 的 Commit Index，
拿到 Leader 的最新的 Commit Index 后，等本地 Apply 到 Leader 最新的 Commit Index 后，然后将这条数据返回给客户端，非常简洁。

Follower Read方案可能会引入两个问题：

1.  因为 TiKV 的异步 Apply 机制，可能会出现一个比较诡异的情况：破坏线性一致性，
    本质原因是由于 Leader 虽然告诉了 Follower 最新的 Commit Index，但是 Leader 对这条 Log 的 Apply 是异步进行的，
    在 Follower 那边可能在 Leader Apply 前已经将这条记录 Apply 了，这样在 Follower 上就能读到这条记录，但是在 Leader 上可能过一会才能读取到。
    
2.  这种 Follower Read 的实现方式仍然会有一次到 Leader 请求 Commit Index 的 RPC，所以目前的 Follower read 实现在降低延迟上不会有太多的效果。

对于第一点，虽然确实不满足线性一致性了，但是好在是永远返回最新的数据，另外我们也证明了这种情况并不会破坏我们的事务隔离级别（Snapshot Isolation），
证明的过程在这里就不展开了，有兴趣的读者可以自己想想。对于第二个问题，虽然对于延迟来说，不会有太多的提升，但是对于提升读的吞吐，减轻 Leader 的负担还是很有帮助的。总体来说是一个很好的优化。



### 一致性读的其他问题
Raft 的目标之一是为上层状态机提供日志记录的 exactly-once 语义，但如果 Leader 在完成日志提交后、向客户端返回响应之前崩溃，客户端就会重试发送该日志记录。
为此，客户端需要为自己的每一次通信赋予独有的序列号，而上层状态机则需要为每个客户端记录其上一次通信所携带的序列号以及对应的响应内容，
如此一来当收到重复的调用时状态机便可在不执行该命令的情况下返回响应。

对于客户端的只读请求，Raft 集群可以在不对日志进行任何写入的情况下返回响应。
然而，这有可能让客户端读取到过时的数据，源于当前与客户端通信的 “Leader” 可能已经不是集群的实际 Leader，而它自己并不知情。
为了解决此问题，Raft 必须提供两个保证。

首先，Leader 持有关于哪个日志记录已经成功提交的最新信息。基于前面提到的 Leader Completeness (完整性)性质，
**节点在成为 Leader 后会立刻添加一个空白的 no-op 日志记录**（表示前面的都已经提交，保证前面的commit被follower提交）；
此外，Leader 还需要知道自己是否已经需要降级，为此 Leader 在处理只读请求前需要先与集群大多数节点完成心跳通信，以确保自己仍是集群的实际 Leader。