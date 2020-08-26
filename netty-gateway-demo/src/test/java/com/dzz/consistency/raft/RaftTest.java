package com.dzz.consistency.raft;

/**
 * @author zoufeng
 * @date 2020-8-20
 * <p>
 * raft
 * 要回答这个问题首先需要说明Raft是做什么的，Paxos、Raft被称为一致性协议，顾名思义它们要解决的是多节点的一致性问题，
 * 需要注意的是这里所说的一致性并不是要求所有节点在任何时刻状态完全一致。而是要保证：
 * >    即使发生网络分区或机器节点异常，整个集群依然能够像单机一样提供一致的服务，即在每次操作时都可以看到其之前的所有成功操作按顺序完成。
 * <p>
 * <p>
 * raft是简化的paxos实现
 * 任期 term
 * 定时器
 * 投票
 * 随机时间
 * <p>
 * 一、简化角色
 * 只有三个角色
 * > 【follower】: 跟随者  节点初始状态都是follower，持有一个定时器，时间是随机数,当定时器时间到了还没有收到leader信息后会将自己转变为candidate,
 * -                       并参与Leader选举，选举成功后除leader外，其他节点全部转为follower
 * -                       当Leader节点出现故障而导致Leader失联，没有接收到心跳的Follower节点将准备成为Candidate进入下一轮Leader选举
 * <p>
 * > 【leader】: 领导者   周期性的给follower发心跳包，证明自己还活着，其他节点受到心跳以后就清空自己的计时器并回复Leader的心跳
 * <p>
 * > 【candidate】: 候选人  将消息发给其他节点来争取他们的投票，若其他节点长时间没有响应Candidate将重新发送选举信息.
 * -                        获得多数票数（超过半数即可）支持的candidate会成为第M任leader。
 * -                        若出现两个Candidate同时选举并获得了相同的票数，那么这两个Candidate将随机推迟一段时间后再向其他节点发出投票请求，
 * -                        这保证了再次发送投票请求以后不冲突。
 * <p>
 * -----------------------------------------选举过程------------------------------------------
 * 选举过程 ：
 * 1、在一个term里，同一个节点只会给出一次投票，先到先得。
 * 2、收到相同或更大Term的AppendEntry，承认对方为Leader，变成Follower。
 * 【同一term中，每个节点的计数器限制时间是随机数，所有总有早的节点先发出选举请求】
 * 3、超时，重新开始新的选举，通过随机的超时时间来减少这种情况得发生
 * <p>
 *
 * -----------------------------------------简化3个子问题---------------------------------------
 * 二、简化问题
 * 将复杂的问题分解成三个子问题：leader选举，日志复制，安全性
 * 日志状态复制：
 * 1、Leader负责接收来自Client的提案请求，写入本地log,之后并行地向所有Follower通过AppendEntry请求发送该Log Entry。
 * >  这些内容将包含在Leader发出的下一个心跳中。
 * 2、Follower接收到心跳以后，Follower对收到的Entry进行验证，包括验证其之前的一条Log Entry项是不是和Leader相同，
 * >  验证成功后写入本地Log并返回Leader成功；
 * 3、Leader接收到多数派Follower的回复以后，将当前Log Commit（如写入状态机）。然后回复Client
 * 4、 后续的AppendEntry及HeartBeat都会携带leader的Commit位置，Follower会提交该位置之前的所有Log Entry，随后所有的节点都拥有相同的数据
 * 上面的提案执行过程保证了两个事情：
 * 1、Follower以与Leader节点相同的顺序依次执行每个成功提案;
 * 2、每个成功提交的提案有足够多的成功副本，来保证后续的访问一致
 * 【脑裂问题】：
 * 若集群中出现网络异常，导致集群被分割，将出现多个Leader
 * 当集群再次连通时，follow将只听从最新任期Leader的指挥，旧Leader将退化为Follower，
 * 任期小的Leader 需要听从任期大的Leader的指挥，此时集群重新达到一致性状态
 *
 * 另外一种脑裂解决方案：
 * leader lease方式
 * 在集群选取两个leader,一个data leader,一个region leader。 region leader会持有一个lease
 * region转发所有的读写请求 给leader，转发成功则续期。
 *
 * lease 方式会牺牲一段时间的可用性
 *
 * 安全性
 * Leader Commit过的提案会向用户返回成功，因此Raft集群需要保证这些提案（日志）永远存在。
 *1、Leader Crash后，新的节点成为Leader，为了不让数据丢失，我们希望新Leader包含所有已经Commit的Entry。
 * > 为了避免数据从Follower到Leader的反向流动带来的复杂性，【Raft限制新Leader一定是当前Log最新的节点，即其拥有最多最大term的Log Entry】。
 * 2、通常对Log的Commit方式都是Leader统计成功AppendEntry的节点是否过半数。
 * > 在节点频发Crash的场景下 旧Leader Commit的Log Entry可能会被后续的Leader用不同的Log Entry覆盖，从而导致数据丢失。
 * > 造成这种错误的根本原因是Leader在Commit后突然Crash，拥有这条Entry的节点并不一定能在之后的选主中胜出。
 * > 这种情况在论文中有详细的介绍。Raft很巧妙的限制【Leader只能对自己本Term的提案采用统计大多数的方式Commit】，
 * > 而旧Term的提案则利用“Commit的Log之前的所有Log都顺序Commit”的机制来提交，从而解决了这个问题。
 * > 另一篇博客中针对这个问题有更详细的阐述Why Raft never commits log entries from previous terms directly
 * <p>
 * <p>
 * <p>
 * <p>
 * -----------------------------------------Raft的后续问题点-----------------------------------------------------------
 *
 * 一、【一致性读的问题】  【follower之间状态不一致和脑裂导致】
 * etcd-raft的实现模式是Leader + Followers，即存在一个Leader和多个Follower。
 * 所有的更新请求都经由Leader处理，Leader再通过日志同步的方式复制到Follower节点。
 * 读请求的处理则没有这种限制：所有的节点（Leader与Followers）都可以处理用户的读请求。
 * 但是由于以下几种原因，导致从不同的节点读数据可能会出现不一致：
 * >  1、Leader和Follower之间存在状态差：这是因为更新总是从Leader复制到Follower，因此，Follower的状态总的落后于Leader，
 * -    不仅于此，Follower之间的状态也可能存在差异。因此，如果不作特殊处理，从集群不同的节点上读数据，读出的结果可能不相同。
 * >  2、假如限制总是从某个特点节点读数据，一般是Leader，但是如果旧的Leader和集群其他节点出现了网络分区，其他节点选出了新的Leader，
 * -    但是旧Leader并没有感知到新的Leader，于是出现了所谓的脑裂现象，旧的Leader依然认为自己是主，但是它上面的数据已经是过时的了，
 * -    如果客户端的请求分别被旧的和新的Leader分别处理，其得到的结果也会不一致。
 * <p>
 * etcd-raft通过一种称为ReadIndex的机制来实现线性一致读，其基本原理也很简单：
 * Leader节点在处理读请求时，首先需要与集群多数节点确认自己依然是Leader，然后读取已经被应用到应用状态机的最新数据。
 * 基本原理包含了两方面内容：
 * >   1、Leader首先通过某种机制确认自己依然是Leader；
 * >   2、Leader需要给客户端返回最近已应用的数据：即最新被应用到状态机的数据
 * <p>
 * etcd的 readIndex 线性一致性读 ：
 * 如果是在Follower节点上执行ReadIndex，那么它必须先要向Leader去查询commit index，
 * 然后收到响应后在创建ReadState记录commit信息，后续的处理和Leader别无二致。
 * 这样，无论是从follower节点还是leader节点读，得到的都是leader commit的信息
 * <p>
 * etcd  LeaseRead 线性一致性读：
 * 我们在前面的ReadIndex中说明了etcd-raft如何通过ReadIndex的思路来实现线性一致性读。
 * 虽然在读的时候只会交互Heartbeat信息，这毕竟还是有代价，所以我们可以考虑做更进一步的优化。
 * 在 Raft 论文里面，提到了一种通过 clock + heartbeat 的 lease read 优化方法。
 * leader在发送 heartbeat 的时候，会首先记录一个时间点 start，当系统大部分节点都回复了 heartbeat response，
 * 那么我们就可以认为 leader 的 lease 有效期可以到 start + election timeout / clock drift bound 这个时间点。
 * <p>
 * 为什么能够这么认为呢？主要是在于 Raft 的选举机制，因为 follower 会在至少 election timeout 的时间之后，才会重新发生选举，
 * 所以下一个 leader 选出来的时间一定可以保证大于 start + election timeout / clock drift bound。
 * 虽然采用 lease 的做法很高效，但仍然会面临风险问题，也就是我们有了一个预设的前提，
 * 各个服务器的 CPU clock 的时间是准的，即使有误差，也会在一个非常小的 bound 范围里面，
 * 如果各个服务器之间 clock 走的频率不一样，有些太快，有些太慢，这套 lease 机制就可能出问题。
 *
 *
 * 二、【网络隔离后的重新选主问题】：
 * 假设有一个 Follower节点被网络隔离后，在等到electionTimeout没收到心跳之后,会发起选举，并转为Candidate。
 * 每次发起选举时，会把Term加一。由于网络隔离，它既不会被选成Leader，也不会收到Leader的消息，而是会一直不断地发起选举。Term会不断增大。
 *
 * 一段时间之后，这个节点的Term会非常大。在网络恢复之后，这个节点会把它的Term传播到集群的其他节点，
 * 导致其他节点更新自己的term，变为Follower。然后触发重新选主，但这个旧的Follower_2节点由于其日志不是最新，
 * 并不会成为Leader。整个集群被这个网络隔离过的旧节点扰乱，显然需要避免的。
 *
 * 这个时候就需要preVote算法。
 * raft的preVote算法：
 * 在PreVote算法中，Candidate首先要确认自己能赢得集群中大多数节点的投票，这样才会把自己的term增加，
 * 然后发起真正的投票。其他投票节点同意发起选举的条件是（同时满足下面两个条件）：
 * 1、没有收到有效领导的心跳，至少有一次选举超时。
 * 2、Candidate的日志足够新（Term更大，或者Term相同raft index更大）。
 * PreVote算法解决了网络分区节点在重新加入时，会中断集群的问题。在PreVote算法中，网络分区节点由于无法获得大部分节点的许可，
 * 因此无法增加其Term。然后当它重新加入集群时，它仍然无法递增其Term，因为其他服务器将一直收到来自Leader节点的定期心跳信息。
 * 一旦该服务器从领导者接收到心跳，它将返回到Follower状态，Term和Leader一致。
 *
 * Prevote是一个典型的2PC协议，第一阶段先征求其他节点是否同意选举，如果同意选举则发起真正的选举操作，
 * 否则降为Follower角色。这样就避免了网络分区节点重新加入集群，触发不必要的选举操作。
 *
 * ----------------------------------sofaraft-----------------------------------------
 * sofa-raft做的一些优化
 * 随机时间选举
 * Raft 算法采用随机超时时间触发选举来避免选票被瓜分的情况，保证选举的顺利完成。
 * 这是主要为了保证在任何的时间段内，Raft 集群最多只能存在一个 Leader 角色的节点。
 * SOFAJRaft 的做法是，在 Node 触发选举的定时任务— electionTimer 中的设置每次定时执行的时间点：
 * 时间区间 [electionTimeoutMs，electionTimeoutMs + maxElectionDelayMs) 中的任何时间点。
 *
 * 随机时间选举的问题
 *1、 下一个任期 Term，Raft 集群中谁会成为 Leader 角色是不确定的，集群中的其他节点成为 Leader 角色的随机性较强，无法预估。
 * -  试想这样的一个场景：假设部署 Raft 集群的服务器采用不同性能规格，业务用户总是期望 Leader 角色节点总是在性能最强的服务器上，
 * -  这样能够为客户端提供较好的读写能力，而上面这种“随机超时时间选举机制”将不能满足需求；
 * 2、  由于会存在选票被瓜分的场景，集群中的各个 Candidate 角色节点将在下一个周期内重新发起选举。
 * -    而在这个极短的时间内，由于集群中不存在 Leader 角色所以是无法正常向客户端提供读写能力，
 * -    因此业务用户需要通过其他方式来避免短时间的不可用造成的影响；
 *
 *
 * sofa对于随机时间选举的优化
 * 基于优先级的半确定性选举机制
 * 主要的算法思想是：通过配置参数的方式预先定义 Raft 集群中各个节点的 priority 选举优先级的值，
 * 每个 Raft 节点进程在启动运行后是能够知道集群中所有节点的 priority 的值（包括它自身的、本地会维护 priority 变量）。
 * 在 Raft 节点进程初始化阶段，通过对所有节点 priority 值求最大值来设置至节点自身维护的 targetPriority 本地全局变量里。
 * 在上面这个例子中，节点的 targetPriority 本地全局变量值就被设置为 160，而自身的 priority 值为 100
 *
 * 故障转移时的问题：
 * 最大优先级的机器挂掉了，如果其他各个节点维护的本地全局变量 targetPriority 值如果不发生改变，
 * 因为节点自身的 priority 值是小于前者的，那其他 Raft 节点不就永远都无法来参与竞选 Leader 角色，
 * 没有 Leader 节点整个 Raft 集群也就无法向外提供读写服务了，这将是设计中的重大缺陷问题！！！
 *
 * 为了解决上述 Raft 集群在发生故障转移时，其他节点无法参与竞选新 Leader 角色的问题。
 * 作者在设计时，引入了 “decayTargetPriority()” 目标优先级衰减降级函数，如果在上一轮由随机超时时间触发的选举周期内没有投票选出 Leader 角色，
 * 那么 Raft 集群中其他各个节点会对本地全局变量 targetPriority 的值按照每次减少 20% 进行衰减，直至衰减值优先级的最小值“1”。
 * 这样在故障转移的时候，集群可以选出次优先级的机器作为leader
 *
 * 参考 《SOFAJRaft 特性解析》系列
 *
 * ---------------------------------demo实现思路---------------------------------------
 * 下面我们来简单实现raft
 */
public class RaftTest {


}
