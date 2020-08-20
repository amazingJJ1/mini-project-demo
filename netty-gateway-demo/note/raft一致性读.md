tikv 使用raft 一致性读

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

LeaseReadLeaseRead 与 ReadIndex 类似，但更进一步，不仅省去了 Log，还省去了网络交互。
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