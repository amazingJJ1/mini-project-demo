package com.dzz.consistency.raft;

/**
 * @author zoufeng
 * @date 2020-8-21
 * 问题完善逻辑连 - -
 * <p>
 * 问题一、 魔法少女提出的问题
 * 集群具有相同term,网络隔离后产生两个leader,他们分别继续选举的更替，然后都达到了term2,
 * 他们最大的logIndex可能一样也可能不一样。这个时候网络问题修复了，之后集群是如何协调呢？
 * <p>
 * 咋看像是脑裂问题，其实仔细思考下题目描述的情况是不会发生的。
 * 首先节点发出选举是需要获得半数投票支持，那么他如何知道集群有多少个节点？
 * 一般启动的时候是会在配置中告诉集群有多少个节点。
 * 网络分隔的情况下，分成两块的区域至少一个区域根据配置是无法获得超过半数的支持，
 * 比如有6个节点，网络隔离了3个，两边都只能和3个节点通信，都无法成功选主。
 * 但是还有一种情况，比如有5个节点，分隔成了A[1个leader,1个follower]和B[3个follower]
 * 那么3个follower的隔离分区会选出一个新的leader。A,B分区有两个leader,也就是问题描述的情况。
 * 但这个其实不会造成数据覆盖的问题。
 * 因为根据raft的机制，A区的老leader是无法成功写入日志的，因为他得不到半数支持，
 * 同样的，根据preVote机制,无法获得多数节点的支持，他也无法进行新的选举，增加term
 * 网络联通后，A leader接到B leader的心跳，B leader的Term必然比A大，A 退化为follower,认 B为leader。
 * A的日志信息也从B里面同步，不会出现数据覆盖的问题。
 * 实际的脑裂情况处理其实就是低版本的term
 *
 * =========================Zookeeper的脑裂问题默认也是通过quorums+term的方式解决==================================
 * 其他解决脑裂的方式，leader lease,给leader设置过期时间，脑裂发生时牺牲一部分客户的请求，保证使用的一致性
 * <p>
 * 上面的问题其实还引出了一个问题，就是raft集群配置的问题。实际运行中，raft的节点可能会有增删节点的情况。
 * 那么如何保证增删节点情况下的一致性？
 * <p>
 * <p>
 * Raft对选举有限制条件
 * 1：Candidate在拉票时需要携带自己本地已经持久化的最新的日志信息，等待投票的节点如果发现自己本地的日志信息比竞选的Candidate更新，则拒绝给他投票。
 * 2：只允许Leader提交（commit）当前Term的日志。
 * <p>
 * 选举过程：
 * 两种情况会让 Candidate 退回 (step down) 到 Follower，放弃竞选本届 Leader：
 * 如果在 Candidate 等待 Servers 的投票结果期间收到了其他拥有更高 Term 的 Server 发来的投票请求；
 * 如果在 Candidate 等待 Servers 的投票结果期间收到了其他拥有更高 Term 的 Server 发来的心跳；
 * 同时，当一个 Leader 发现有 Term 更高的 Leader 时也会退回到 Follower 状态。
 * 当选举 Leader 选举成功后，整个 Raft 集群就可以正常地向外提供读写服务了，
 * 如上图所示，集群由一个 Leader 和两个 Follower 组成，Leader 负责处理 Client 发起的读写请求，
 * 同时还要跟 Follower 保持心跳和将日志 Log 复制给 Follower。
 * <p>
 * <p>
 * <p>
 * <p>
 * 日志同步过程：
 * Leader发生切换的时候，新Leader的日志和Follower的日志可能会存在不一致的情形。
 * 这时Follower需要对自身的日志进行截断处理，再从截断的位置重新同步日志。
 * Leader自身的日志是Append-Only的，它永远不会抹掉自身的任何日志。
 * 标准的策略是Leader当选后立即向所有节点广播AppendEntries消息，携带最后一条日志的信息。
 * Follower收到消息后和自己的日志进行比对，如果最后一条日志和自己的不匹配就回绝Leader。
 * Leader被打击后，就会开始回退一步，携带最后两条日志，重新向拒绝自己的Follower发送AppendEntries消息。
 * 如果Follower发现消息中两条日志的第一条还是和自己的不匹配，那就继续拒绝，然后Leader被打击后继续后退重试。
 * 如果匹配的话，那么就把消息中的日志项覆盖掉本地的日志，于是同步就成功了，一致性就实现了。
 * <p>
 * <p>
 * 问题二、集群配置变更。 (如何增删节点)
 * 分布式系统的一个非常头疼的问题就是同样的动作发生的时间却不一样。
 * 比如集群从3个变成5个，集群的配置从OldConfig变成NewConfig。
 * 这些节点配置转变的时间并不完全一样，存在一定的偏差，于是就形成了新旧配置的叠加态。
 * 比如：
 * 旧配置的集群下Server[1,2]可以选举Server1为Leader，Server3不同意没关系，过半就行。
 * 而同样的时间，新配置的集群下Server[3,4,5]则可以选举出Server5为另外一个Leader。
 * 这时候就存在多Leader并存问题。
 * 为了避免这个问题，Raft使用单节点变更算法。一次只允许变动一个节点，并且要按顺序变更，不允许并行交叉，否则会出现混乱。
 * 如果你想从3个节点变成5个节点，那就先变成4节点，再变成5节点。变更单节点的好处是集群不会分裂，不会同时存在两个Leader。
 * <p>
 * 集群变更操作日志不同于普通日志。普通日志要等到commit之后才可以apply到状态机，而集群变更日志在leader将日志追加持久化后，就可以立即apply。
 * 为什么要这么做，可以参考知乎孙建良的这篇文章 https://zhuanlan.zhihu.com/p/29678067，
 * 这里面精细描述了commit & reply集群变更日志带来的集群不可用的场景
 * <p>
 * <p>
 * <p>
 * <p>
 * <p>
 * 问题三、raft和事务
 * 见TiKVMulitRaft
 * <p>
 * 问题四、raft中，如果leader已经将日志发送给一半以上的节点后，没回复客户端就宕机了，该如何处理？
 * raft 会重新选leader 完成数据的提交，客户端会重试，raft要求RPC实现幂等性，会做内部去重机制
 */
public class RaftQuestion {

    /*
    * 完全模拟一遍raft交互流程
    *
    * 假设我们有5个初始节点
    *
    * 初始化配置：
    * 1、节点ip:port，节点数量
    * 2、选举超时时间，最大选举超时延时。选举定计时器随机数区间=[electionTimeoutMs，electionTimeoutMs + maxElectionDelayMs)
    *
    * 启动：
    * 1、节点以follower角色启动，等待leader的心跳，因为没有leader，节点会一直等到选举定计时器的时间超时。
    * >   这个时候第一个超时的follower1会将自己变成candidate1，准备为自己拉票，拉票前启动preVote流程。
    * preVote：
    * >  preVote的目的是让Candidate首先要确认自己能赢得集群中【大多数节点】的投票，这样才会把自己的term增加。而其他节点同意投票需要满足2个条件
    * >   ①、没有收到有效领导的心跳，至少有一次选举超时
    * >   ②、Candidate的日志足够新（Term更大，或者Term相同raft index更大）。
    * 因为初始化的时候其他节点的term都是0，也没有commit index。其他节点回复candidate的请求，会把票投给他。candidate1收到超过半数的同意，
    * 于是将term+1，开始发起新一轮选举投票请求[如果没有收到半数的同意，则将自己退化成follower,重置计时器等待]。
    * 选举投票过程：
    * 2、如果candidate1获得多数节点的票数，则将自己角色变成leader，并进行周期性的心跳广播，其他节点收到心跳后将自己的角色置为follower。
    * >  但是如果candidate1和candidate2获得相同的票数（瓜分了票池），即两个candidate都没有超过半数投票，那么他们继续等待自己的计数器超时，
    * >     这里我是这么设计的，设选举的轮次是term0,选举的时间周期为et,选举delay时间为dt,那么term0轮所有的candidate的进行投票开始时间
    * >     一定是在(et,et+dt)的区间，收集的截止时间一定是小于et+dt+et,那么我们将这轮的candidate的投票收集截止时间都设为et+dt+et。如果
    * >     candidate在这段时间既没有收到多数投票，也没有收到leader心跳，那么会在(et+dt+et,et+dt+et+dt)时间段内随机一个时间，重新开启新
    * >     一轮投票,term0+1。
    * 投票请求：
    * >   节点接收candidate的投票请求时是以下处理：
    * >    ①在一个term里，同一个节点只会给出一次投票，先到先得
    * >    ②Candidate在拉票时需要携带自己本地已经持久化的最新的日志信息。
    * >      等待投票的节点如果发现自己本地的日志信息比竞选的Candidate更新，则拒绝给他投票。
    * >      如果接收的term低，也一样拒绝投票
    * 放弃竞选：
    * >   两种情况会让 Candidate 退回 (step down) 到 Follower，放弃竞选本届 Leader：
    * >     ①如果在 Candidate 等待 Servers 的投票结果期间收到了其他拥有更高 Term 的 Server 发来的投票请求；
    * >     ②如果在 Candidate 等待 Servers 的投票结果期间收到了其他拥有更高 Term 的 Server 发来的心跳；
    * >   同时，当一个 Leader 发现有 Term 更高的 Leader 时也会退回到 Follower 状态。
    *
    * 3、数据写入过程
    * >数据写入都是通过leader进行写入
    * >     ①Leader负责接收来自Client的提案请求，写入本地log,之后并行地向所有Follower通过AppendEntry请求发送该Log Entry。
    * >        这些内容将包含在Leader发出的下一个心跳中。
    * >     ②Follower接收到心跳以后，Follower对收到的Entry进行验证，包括验证其之前的一条Log Entry项是不是和Leader相同，
    * >        如果最后一条日志和自己的不匹配就回绝Leader。Leader被打击后，就会开始回退一步，携带最后两条日志，
    * >        重新向拒绝自己的Follower发送AppendEntries消息。如果Follower发现消息中两条日志的第一条还是和自己的不匹配，
    * >        那就继续拒绝，然后Leader被打击后继续后退重试。如果匹配的话，那么就把消息中的日志项写入本地的日志.
    * >        于是日志同步就成功了，一致性就实现了。（实际实现应该只有两次网络请求，直接到日志匹配的位置复制日志）
    * >     ③Leader接收到多数派Follower的回复以后，将当前Log Commit（如写入状态机）。然后回复Client成功
    * >     ④Leader后续的AppendEntry及HeartBeat都会携带leader的Commit位置，Follower会提交该位置之前的所有Log Entry，
    * >       随后所有的节点都拥有相同的数据。
    * >数据写入的过程保证了两件事
    * >>>1、Follower以与Leader节点相同的顺序依次执行每个成功提案;
    * >>>2、每个成功提交的提案有足够多的成功副本，来保证后续的访问一致
    * > 数据写入的安全性
    *
    *
    *
    *
    *
    *
    *
    *
    * */

}
