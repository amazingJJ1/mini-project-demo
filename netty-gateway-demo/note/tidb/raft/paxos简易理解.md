## 背景

Paxos算法是分布式技术大师Lamport提出的，主要目的是通过这个算法，让参与分布式处理的每个参与者逐步达成一致意见。用好理解的方式来说，就是在一个选举过程中，让不同的选民最终做出一致的决定。 

Lamport为了讲述这个算法，假想了一个叫做Paxos的希腊城邦进行选举的情景，这个算法也是因此而得名。在他的假想中，这个城邦要采用民主提议和投票的方式选出一个最终的决议，但由于城邦的居民没有人愿意把全部时间和精力放在这种事情上，所以他们只能不定时的来参加提议，不定时来了解提议、投票进展，不定时的表达自己的投票意见。**Paxos算法的目标就是让他们按照少数服从多数的方式**，最终达成一致意见。

自Paxos问世以来就持续垄断了分布式一致性算法，Paxos这个名词几乎等同于分布式一致性。Google的很多大型分布式系统都采用了Paxos算法来解决分布式一致性问题，如Chubby、Megastore以及Spanner等。开源的ZooKeeper，以及MySQL 5.7推出的用来取代传统的主从复制的MySQL Group Replication等纷纷采用Paxos算法解决分布式一致性问题



## 流程

三个角色

- 提议者 Proposer : 提出提案 (Proposal)。Proposal信息包括提案编号 (Proposal ID) 和提议的值 (Value)。
- 接收者 acceptor : 参与决策，回应Proposers的提案。收到Proposal后可以接受提案，若Proposal获得多数Acceptors的接受，则称该Proposal被批准。
- 学习者 learner : 不参与决策，从Proposers/Acceptors学习最新达成一致的提案（Value）。

三个阶段

- 第一阶段：Prepare阶段。Proposer向Acceptors发出Prepare请求，Acceptors针对收到的Prepare请求进行Promise承诺。

- 第二阶段：Accept阶段。Proposer收到多数Acceptors承诺的Promise后，向Acceptors发出Propose请求，Acceptors针对收到的Propose请求进行Accept处理。

- 第三阶段：Learn阶段。Proposer在收到多数Acceptors的Accept之后，标志着本次Accept成功，决议形成，将形成的决议发送给所有Learners。用于感知value最终是否被大多数acceptor认可，如果已经超过半数认可则value被定下来，并通知所有actor关闭本次更新流程。更新应该是连续的，每次更新都会有一个编号。实际应用过程中还需要对更新编号进行处理，避免老的数据残留。控制每一轮paxos流程的结束周期

> 在多副本状态机中，每个副本同时具有Proposer、Acceptor、Learner三种角色。

第一阶段一次广播rpc,第二阶段一次广播rpc，第三阶段一次广播

网络交互次数=提议者个数X3X集群节点数，网络开销略大



Paxos算法流程中的每条消息描述如下：

- **Prepare**: Proposer生成全局唯一且递增的Proposal ID (可使用时间戳加Server ID)，向所有Acceptors发送Prepare请求，这里无需携带提案内容，只携带Proposal ID即可。
- **Promise**: Acceptors收到Prepare请求后，做出“**两个承诺，一个应答**”。

两个承诺：

1. 不再接受Proposal ID小于等于（注意：这里是<= ）当前请求的Prepare请求。

2. 不再接受Proposal ID小于（注意：这里是< ）当前请求的Propose请求。

一个应答：

   不违背以前作出的承诺下，回复已经Accept过的提案中Proposal ID最大的那个提案的Value和Proposal ID，没有则返回空值。

- **Propose**: Proposer 收到多数Acceptors的Promise应答后，从应答中选择Proposal ID最大的提案的Value，作为本次要发起的提案。如果所有应答的提案Value均为空值，则可以自己随意决定提案Value。然后携带当前Proposal ID，向所有Acceptors发送Propose请求。
- **Accept**: Acceptor收到Propose请求后，在不违背自己之前作出的承诺下，接受并持久化当前Proposal ID和提案Value。
- **Learn**: Proposer收到多数Acceptors的Accept后，决议形成，将形成的决议发送给所有Learners。



#### 伪代码过程

1. 获取一个Proposal ID n，为了保证Proposal ID唯一，可采用时间戳+Server ID生成；

2. Proposer向所有Acceptors广播Prepare(n)请求；

3. Acceptor比较n和minProposal，如果n>minProposal，minProposal=n，并且将 acceptedProposal 和 acceptedValue 返回；

4. Proposer接收到过半数回复后，如果发现有acceptedValue返回，将所有回复中acceptedProposal最大的acceptedValue作为本次提案的value，否则可以任意决定本次提案的value；

5. 到这里可以进入第二阶段，广播Accept (n,value) 到所有节点；

6. Acceptor比较n和minProposal，如果n>=minProposal，则acceptedProposal=minProposal=n，acceptedValue=value，本地持久化后，返回；否则，返回minProposal。

7. 提议者接收到过半数请求后，如果发现有返回值result >n，表示有更新的提议，跳转到1；否则value达成一致。

   

#### 例子模拟流程

假设有2个proposer：`提议者1`,`提议者2`。3个acceptor,3个learn。

`提议者1`的prepare阶段：

1. `提议者1`向3个接受者广播preare请求(p1,v1)
2. 3个接受者开始没有任何记录（acceptProposalId=null,accpectValue=null），按照上面promise原则（只接受ProposalId>acceptProposalId的prepare请求），于是记录(p1,v1) ，响应prepare请求成功，因为之前接收者没有记录任何值，返回（acceptProposalId=null,accpectValue=null）

`提议者1`获得了prepare的半数响应，进入他的accpect阶段：

3. `提议者1`向`接收者1`发起提议请求（p1,v1）,`接受者1`查看自己的记录，根据promise原则（只接受ProposalId>=acceptProposalId的Propose请求）,返回接受成功，并记录（acceptProposalId=p1,accpectValue=v1）

这个时候`提议者2`也突然发起了prepare阶段：

4. `提议者2`向3个接受者广播preare请求(p2,v2)，其中p2>p1。`接受者1`和`接受者2`收到prepare请求
5. 对于`接受者1`来说，因为acceptProposalId=p1<p2，响应prepare成功，同时返回（acceptProposalId=p1,accpectValue=v1）。对于`接受者2`来说，还没有来的及接受提议者1的请求，依旧是（acceptProposalId=null,accpectValue=null），记录(p2,v2)，响应成功，同时返回（acceptProposalId=null,accpectValue=null）。

这个时候`提议者2`也获得了prepare的半数响应，同样进入accept阶段:

6. `提议者1`继续向`接受者2`和`接受者3`发起propose请求。对于`接受者2`来说，由于他记录收到最大的prepare请求id是p2,于是拒绝提议者1的请求。而对于`接收者3`，记录的prepare请求Id最大的依旧是p1，于是接受请求，设置（acceptProposalId=p1,accpectValue=v1）。这里`提议者1`其实已经获得了accept的半数响应，得到了一致性的值v1。
7. `提议者2`收到5流程`接收者1`和`接受者2`的结果，得知之前有更早`提议1`被接受，且值为v1，于是把自己的值也改成v1(这里其实很大程度体现了paxos算法的思想，算法是为了获得一个一致性结果，这里改变值是为了更快的获得一个一致性的结果)。
8. `提议2`向`接受者1`和`接受者2`发起propose请求。对于`接受者1`和`接受者2`来说，由于他记录收到最大的prepare请求id是p2,于是接受提议者2的请求，记录（acceptProposalId=p2,accpectValue=v1）并响应成功。（接受者3其实也会响应成功）这个时候提议者2也获得accepte阶段的半数响应，最终的一致性的值也是v1。

#### 例子的一些问题

1. 为什么提议者1已经获得最终一致性的值依旧还有走提议者2的accept阶段？

> 其实上述流程走完6就已经结束了，`提议者1`接着进入Learn阶段对所有的learn进行广播，learn等到一致性结果后结束一轮paxos流程。
>
> 但是分布式环境有很多不确定性，可能`提议者1`获得一致性结果后就挂了，需要继续发起新一轮提案。而`提议者2`继续流程能保证最终的提案和值是一致性的。如果learn获得了一致性值可以提前结束`提议者2`的流程

2. 如果`提议者2`在`提议者1`之前先进入accept阶段，会怎么样？

> 那最终得到的一致性值可能就是V2,因为大概率`提议者2`的提议先被接受，获得大多数的accept的响应



### basic paxos的问题

prepare阶段的冲突导致活锁问题：

假设5个节点n1-n5：

1. prepare1阶段，n1-n3节点prepare成功

2. prepare2阶段，n3-n5节点prepare成功
3. prepare1进入accept1阶段，请求propose到n3-n5响应失败，因为n3-n5记录的prepare Id p2>p1
4. 这个时候又有提议发起prepare3阶段，n1-n3节点prepare成功
5. prepare2进入accept2阶段，请求propose到n1-n1响应失败，因为n1-n1记录的prepare Id p3>p2
6. 依次循环，prepare阶段成功，accept阶段失败...



原始的Paxos算法（Basic Paxos）只能对一个值形成决议，决议的形成至少需要两次网络来回，在高并发情况下可能需要更多的网络来回，极端情况下甚至可能形成活锁。如果想连续确定多个值，Basic Paxos搞不定了。因此Basic Paxos几乎只是用来做理论研究，并不直接应用在实际工程中。



### multi-paxos

实际应用中几乎都需要连续确定多个值，而且希望能有更高的效率。Multi-Paxos正是为解决此问题而提出。Multi-Paxos基于Basic Paxos做了两点改进：

1. 针对每一个要确定的值，运行一次Paxos算法实例（Instance），形成决议。每一个Paxos实例使用唯一的Instance ID标识。
2. 在所有Proposers中选举一个Leader，由Leader唯一地提交Proposal给Acceptors进行表决。这样没有Proposer竞争，解决了活锁问题。在系统中仅有一个Leader进行Value提交的情况下，Prepare阶段就可以跳过，从而将两阶段变为一阶段，提高效率。

Multi-Paxos首先需要选举Leader，Leader的确定也是一次决议的形成，所以可执行一次Basic Paxos实例来选举出一个Leader。选出Leader之后只能由Leader提交Proposal，在Leader宕机之后服务临时不可用，需要重新选举Leader继续服务。在系统中仅有一个Leader进行Proposal提交的情况下，Prepare阶段可以跳过。

Multi-Paxos通过改变Prepare阶段的作用范围至后面Leader提交的所有实例，从而使得Leader的连续提交只需要执行一次Prepare阶段，后续只需要执行Accept阶段，将两阶段变为一阶段，提高了效率。为了区分连续提交的多个实例，每个实例使用一个Instance ID标识，Instance ID由Leader本地递增生成即可。

Multi-Paxos允许有多个自认为是Leader的节点并发提交Proposal而不影响其安全性，这样的场景即退化为Basic Paxos。

Chubby和Boxwood均使用Multi-Paxos。ZooKeeper使用的Zab也是Multi-Paxos的变形。



### e-paxos

Multi-Paxos首先选举Leader，Leader选出来后Instance的提议权都归Leader，无需竞争Instance的提议权，因此可以省略Prepare阶段，只需要一阶段。Leader的存在提高了达成决议的效率，但同时也成为了性能和可用性的瓶颈。

Leader需要处理比其它副本更多的消息，各副本负载不均衡，资源利用率不高。Leader宕机后系统不可用，直到新Leader被选举出来，才能恢复服务，降低了可用性。

Basic Paxos每个副本都能提议，可用性高，但因为竞争冲突导致效率低下；Multi-Paxos选举Leader避免冲突，提高效率，但同时又引入了Leader瓶颈，降低了可用性。效率和可用性能否兼顾？EPaxos正是为了解决此问题而提出。不同于Multi-Paxos引入Leader来避免冲突，EPaxos采用另一种思路，它直面冲突，试图解决冲突问题。



epaxos算法的创新

- 1）分布式算法执行的过程就是对cmd进行排序的过程：multi-paxox, g-paxos完全由leader分配顺序；mencius是按照规则预先建立起来的槽位；
- 2）Epaxos的顺序是动态决定的，给每个cmd添加排序的约束（依赖关系），每个节点通过依赖关系最终保证commit的顺序是一样的

#### epaxos协议的设计

- Preliminaries ：

- - Instance的顺序不是事先分配的（比如paxos中在issue一个proposal时就要决定出一个instanceID），而是随着cmd被commit时决定出来的；
  - commit和execute顺序没有必然联系，是不同的操作（raft的commit顺序和execute是一样的，g-paxos是不一样的），两者没有必要保持一致；
  - replica告知client一个cmd被commit了，client并不能知晓这个cmd是否被execute了，只能通过发起一次read操作，replica会保证先execute；

- 定义command interfere：如果执行[a, b]的顺序和执行[b, a]产生的结果不一样，就说a,b 之间是有依赖关系

> 其实没搞明白e-paxos，先跳过把





作者：祥光

链接：https://zhuanlan.zhihu.com/p/31780743

## 附Paxos算法推导过程



Paxos算法的设计过程就是从正确性开始的，对于分布式一致性问题，很多进程提出（Propose）不同的值，共识算法保证最终只有其中一个值被选定，Safety表述如下：

- 只有被提出（Propose）的值才可能被最终选定（Chosen）。
- 只有**一**个值会被选定（Chosen）。
- 进程只会获知到已经确认被选定（Chosen）的值。

Paxos以这几条约束作为出发点进行设计，只要算法最终满足这几点，正确性就不需要证明了。Paxos算法中共分为三种参与者：Proposer、Acceptor以及Learner，通常实现中每个进程都同时扮演这三个角色。

Proposers向Acceptors提出Proposal，为了保证最多只有**一**个值被选定（Chosen），Proposal必须被超过一半的Acceptors所接受（Accept），且每个Acceptor只能接受一个值。

为了保证正常运行（必须有值被接受），所以Paxos算法中：



**P1：Acceptor必须接受（Accept）它所收到的第一个Proposal。**



先来先服务，合情合理。但这样产生一个问题，如果多个Proposers同时提出Proposal，很可能会导致无法达成一致，因为没有Propopal被超过一半Acceptors的接受，因此，Acceptor必须能够接受多个Proposal，不同的Proposal由不同的编号进行区分，当某个Proposal被超过一半的Acceptors接受后，这个Proposal就被选定了。

既然允许Acceptors接受多个Proposal就有可能出现多个不同值都被最终选定的情况，这违背了Safety要求，为了保证Safety要求，Paxos进一步提出：



**P2：如果值为v的Proposal被选定（Chosen），则任何被选定（Chosen）的具有更高编号的Proposal值也一定为v。**



只要算法同时满足**P1**和**P2**，就保证了Safety。**P2**是一个比较宽泛的约定，完全没有算法细节，我们对其进一步延伸：



**P2a：如果值为v的Proposal被选定（Chosen），则对所有的Acceptors，它们接受（Accept）的任何具有更高编号的Proposal值也一定为v。**



如果满足**P2a**则一定满足**P2**，显然，因为只有首先被接受才有可能被最终选定。但是**P2a**依然难以实现，因为acceptor很有可能并不知道之前被选定的Proposal（恰好不在接受它的多数派中），因此进一步延伸：



**P2b：如果值为v的Proposal被选定（Chosen），则对所有的Proposer，它们提出的的任何具有更高编号的Proposal值也一定为v。**



更进一步的：



**P2c：为了提出值为v且编号为n的Proposal，必须存在一个包含超过一半Acceptors的集合S，满足(1) 没有任何S中的Acceptors曾经接受（Accept）过编号比n小的Proposal，或者(2) v和S中的Acceptors所接受过(Accept)的编号最大且小于n的Proposal值一致。**



满足**P2c**即满足**P2b**即满足**P2a**即满足**P2**。至此Paxos提出了Proposer的执行流程，以满足**P2c**：

1. Proposer选择一个新的编号n，向超过一半的Acceptors发送请求消息，Acceptor回复: (a)承诺不会接受编号比n小的proposal，以及(b)它所接受过的编号比n小的最大Proposal（如果有）。该请求称为Prepare请求。
2. 如果Proposer收到超过一半Acceptors的回复，它就可以提出Proposal，Proposal的值为收到回复中编号最大的Proposal的值，如果没有这样的值，则可以自由提出任何值。
3. 向收到回复的Acceptors发送Accept请求，请求对方接受提出的Proposal。

仔细品味Proposer的执行流程，其完全吻合**P2c**中的要求，但你可能也发现了，当多个Proposer同时运行时，有可能出现没有任何Proposal可以成功被接受的情况（编号递增的交替完成第一步），这就是Paxos算法的Liveness问题，或者叫“活锁”，论文中建议通过对Proposers引入选主算法选出Distinguished Proposer来全权负责提出Proposal来解决这个问题，但是即使在出现多个Proposers同时提出Proposal的情况时，Paxos算法也可以保证Safety。

接下来看看Acceptors的执行过程，和我们对**P2**做的事情一样，我们对**P1**进行延伸：



**P1a：Acceptor可以接受（Accept）编号为n的Proposal当且仅当它没有回复过一个具有更大编号的Prepare消息。**



易见，**P1a**包含了**P1**，对于Acceptors：

1. 当收到Prepare请求时，如果其编号n大于之前所收到的Prepare消息，则回复。
2. 当收到Accept请求时，仅当它没有回复过一个具有更大编号的Prepare消息，接受该Proposal并回复。

以上涵盖了满足**P1a**和**P2b**的一套完整一致性算法。





### paxos,raft,zab对比

作者：朱一聪

链接：https://www.zhihu.com/question/36648084/answer/82332860

来源：知乎

著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。

Raft协议比paxos的优点是 容易理解，容易实现。它强化了leader的地位，把整个协议可以清楚的分割成两个部分，并利用日志的连续性做了一些简化： （1）Leader在时。由Leader向Follower同步日志 （2）Leader挂掉了，选一个新Leader，Leader选举算法。

​      但是本质上来说，它容易的地方在于流程清晰，描述更清晰，关键之处都给出了伪代码级别的描述，可以直接用于实现，而paxos最初的描述是针对非常理论的一致性问题，真正能应用于工程实现的mulit-paxos，Lamport老爷爷就提了个大概，之后也有人尝试对multi-paxos做出更为完整详细的描述，但是每个人描述的都不大一样。

​      Zookeeper的ZAB，Viewstamped Replication（VR），raft，multi-paxos，这些都可以被称之为Leader-based一致性协议。不同的是，multi-paxos leader是作为对经典paxos的优化而提出，通过选择一个proposer作为leader降低多个proposer引起冲突的频率，合并阶段一从而将一次决议的平均消息代价缩小到最优的两次，实际上就算有多个leader存在，算法还是安全的，只是退化为经典的paxos算法。而经典的paxos，从一个提案被提出到被接受分为两个阶段，第一个阶段去询问值，第二阶段根据询问的结果提出值。这两个阶段是无法分割的，两个阶段的每个细节都是精心设计的，相互关联，共同保障了协议的一致性。而VR,ZAB,Raft这些强调**合法leader的唯一性**协议，它们直接从leader的角度描述协议的流程，也从leader的角度出发论证正确性。**但是实际上它们使用了和Paxos完全一样的原理来保证协议的安全性**，当同时存在多个节点同时尝试成为leader或者不知一个节点认为自己时leader时，本质上它们和经典Paxos中多个proposer并存的情形没什么不同。

​    Paxos和raft都是一旦一个entries（raft协议叫日志，paxos叫提案，叫法而已）得到多数派的赞成，这个entries就会定下来，不丢失，值不更改，最终所有节点都会赞成它。Paxos中称为提案被决定，Raft,ZAB,VR称为日志被提交，这只是说法问题**。一个日志一旦被提交(或者决定），就不会丢失，也不可能更改，这一点这4个协议都是一致的**。**Multi-paxos和Raft都用一个数字来标识leader的合法性**，multi-paxos中叫proposer-id，Raft叫term，意义是一样的，**multi-paxos proposer-id最大的Leader提出的决议才是有效的，raft协议中term最大的leader才是合法的**。实际上raft协议在leader选举阶段，由于老leader可能也还存活，也会存在不只一个leader的情形，**只是不存在term一样的两个leader**，因为选举算法要求leader得到同一个term的多数派的同意，同时赞同的成员会承诺不接受term更小的任何消息。这样可以根据term大小来区分谁是合法的leader。Multi-paxos的区分leader的合法性策略其实是一样的，谁的proproser-id大谁合法，而proposer-id是唯一的。**因此它们其实在同一个时刻，都只会存在一个合法的leader。**同时raft协议的Leader选举算法，新选举出的Leader已经拥有全部的可以被提交的日志，而multi-paxos择不需要保证这一点，这也意味multi-paxos需要额外的流程从其它节点获取已经被提交的日志。因此raft协议日志可以简单的只从leader流向follower在raft协议中，而multi-paxos则需要额外的流程补全已提交的日志。**需要注意的是日志可以被提交和日志已经被提交是两个概念**，它们的区别就像是我前方有块石头和我得知我前方有块石头。但是实际上，Raft和multi-Paxos一旦日志可以被提交，就能会保证不丢失，multi-paxos天然保证了这一点，**这也是为什么新leader对于尚未被确认已经提交的日志需要重新执行经典paxos的阶段一，来补全可能缺失的已经被提交的日志**，Raft协议通过强制新Leader首先提交一个本term的no-op 日志，配合前面提到的Leader选举算法所保证的性质，确保了这一点。一条日志一旦被多数派的节点写入本地日志文件中，就可以被提交，但是leader只有得知这一点后，才会真正commit这条日志，此时日志才是已经被提交的。

​       **Raft协议强调日志的连续性，multi-paxos则允许日志有空洞**。**日志的连续性蕴含了这样一条性质：如果两个不同节点上相同序号的日志，只要term相同，那么这两条日志必然相同，且这和这之前的日志必然也相同的，**这使得leader想follower同步日志时，比对日志非常的快速和方便；同时Raft协议中**日志的commit（提交）也是连续的**，一条日志被提交，代表这条日志之前所有的日志都已被提交，一条日志可以被提交，代表之前所有的日志都可以被提交。日志的连续性使得Raft协议中，知道一个节点的日志情况非常简单，只需要获取它最后一条日志的序号和term。可以举个列子，A,B,C三台机器，C是Leader，term是3，A告诉C它们最后一个日志的序列号都是4，term都是3，那么C就知道A肯定有序列号为1,2,3,4的日志，而且和C中的序列号为1,2,3,4的日志一样，这是raft协议日志的连续性所强调的，好了那么Leader知道日志1，2，3，4已经被多数派（A,C)拥有了，可以提交了。同时，这也保证raft协议在leader选举的时候，**一个多数派里必然存在一个节点拥有全部的已提交的日志，这是由于最后一条被commit的日志，至少被多数派记录，而由于日志的连续性，拥有最后一条commit的日志也就意味着拥有全部的commit日志，即至少有一个多数派拥有所有已commit的日志。**并且只需要从一个多数集中选择最后出最后一条日志**term最大且序号最大**的节点作为leader，新leader必定是拥有全部已commit的日志(关于这一点的论证，可以通过反证法思考一下，多数集中节点A拥有最后一条已commit的日志，但是B没有，而B当选leader。根据选主的法则只能有两种可能(1)当选而A最后一条日志的term小于B；(2)A最后一条日志的term等于B，但是A的日志少于B。1,2可能嘛？）而对于multi-paxos来说，日志是有空洞的，每个日志需要单独被确认是否可以commit，也可以单独commit。因此当新leader产生后，它只好重新对每个未提交的日志进行确认，已确定它们是否可以被commit，甚至于新leader可能缺失可以被提交的日志，需要通过Paxos阶段一向其它节点学习到缺失的可以被提交的日志，当然这都可以通过向一个多数派询问完成（这个流程存在着很大的优化空间，例如可以将这个流程合并到leader选举阶段，可以将所有日志的确认和学习合并到一轮消息中，减少消息数目等）。但是无论是Raft还是multi-paxos，**新leader对于那些未提交的日志，都需要重新提交，不能随意覆写，因为两者都无法判定这些未提交的日志是否已经被之前的leader提交了**。所以本质上，两者是一样的。一个日志被多数派拥有，那么它就可以被提交，但是Leader需要通过某种方式得知这一点，同时为了已经被提交的日志不被新leader覆写，新leader需要拥有所有已经被提交的日志（或者说可以被提交，因为有时候并没有办法得知一条可以被提交的日志是否已经被提交，例如当只有老leader提交了该日志，并返回客户端成功，然而老leader挂了），之后才能正常工作，并且需要重新提交所有未commit的日志**。两者的区别在于Leader确认提交和获取所有可以被提交日志的方式上，而方式上的区别又是由于是日志是否连续造成的，Raft协议利用日志连续性，简化了这个过程**。



**在Raft和multi-paxos协议确保安全性的原理上**，更进一步的说，所有的凡是 满足 集群中存活的节点数还能构成一个多数派，一致性就能满足的算法，raft协议，paxos，zab，viewstamp都是利用了**同一个性质：两个多数派集合之间存在一个公共成员**。对于一致性协议来说，一旦一个变量的值被确定，那么这个变量的值应该是唯一的，不再更改的。Raft,paoxos等协议，对于一个变量v来说，一个由节点n1提出的值a只有被一个多数集q1认可并记录后，才会正式令v=a，如果另一个节点n2想要修改变量v的值为b，也需要一个多数集q2的认可，而q1和q2必然至少有一个共同的成员p,节点p已经记录了v=a。**因此只需要通过增加一些约束，让p能够告诉节点n2这个事实：v=a，使得n2放弃自己的提议，或者让节点p拒绝节点n2想要赋予v的值为b这个行为，都可以确保变量v的一致性不被破坏。**这个思想对于这个四个协议来说都是一样的，**4个协议都使用一个唯一的整数作为标识符来标明leader的合法性**，paxos叫做proposer-id，ZAB叫epoch，VR叫view，raft叫term。把leader看做是想要赋予变量v某个值的节点n1,n2，上面提到的情形中，如果n2是目前的合法leader，那么n2需要知道v=a这个事实，对于raft来说就是选择n2是已经记录了v=a的节点，对于multi-paxos来说，就是重新确认下v的值。如果n1是目前的合法leader，n2是老的leader，p就会根据leader的标识符拒绝掉n2的提案，n2的提案会由于得不到一个多数派的接受而失效。**最直接的从理论上阐明这个原理的是经典的paxos算法**，关于这个原理更具体的阐述可以看看我在[如何浅显易懂地解说 Paxos 的算法？](https://www.zhihu.com/question/19787937)下的回答。所以确实在一定程度上可以视raft,ZAB,VR都是paxos算法的一种改进，一种简化，一种优化，一种具象化。Lamport老人家还是叼叼叼。。。。。。。不过值得一提的是，ZAB和raft作者确实是受了paxos很多启发，**VR是几乎同时独立于paxos提出的。**

​       Raft容易实现在于它的描述是非常规范的，包括了所有的实现细节。如上面的人说的有如伪代码。而paxos的描述侧重于理论，工程实现按照谷歌chubby论文中的说话，大家从paxos出现，写着写着，处理了n多实际中的细节之后，已经变成另外一个算法了，这时候正确性已经无法得到理论的保证。所以它的实现非常难，因为一致性协议实非常精妙的。小细节上没考虑好，整个协议的一致性就崩溃了，**而发现并更正细节上的错误在没有详尽的现成的参考的情况下是困难的，这需要对协议很深的理解。**而且在Raft协议的博士论文CONSENSUS: BRIDGING THEORY AND PRACTICE，两位作者手把手事无巨细的教你如何用raft协议构建一个复制状态机。我表示智商正常的大学生，都能看懂。我相信在未来一致性现在被提起来，肯定不是现在这样，大部分人觉得好难啊，实现更难。。。。应该会成为一种常用技术。







## 思考

最后留下几个思考题，感兴趣的同学可以思考思考，欢迎大家在评论区留言：

1、Paxos的Proposal ID需要唯一吗，不唯一会影响正确性吗？

2、Paxos如果不区分max Proposal ID和Accepted Proposal ID，合并成一个max Proposal ID，过滤Proposal ID小于等于max Proposal ID的Prepare请求和Accept请求，会影响正确性吗？

3、Raft的PreVote有什么作用，是否一定需要PreVote？

4、Raft的Leader必须在Quorum里面吗？如果不在会有什么影响？

> Paxos 的Proposal ID 是否唯一 对正确性没有影响，但会影响活性. 甚至Proposal ID 递增，或者递减都没有关系。Paxos 是后者认同前者，并不是竞争关系，最终算法都会向同一个方向收敛到同一个值. Raft 是一个简化版本的Paxos , PreVote 当然不是必须的, 只是一种优化，防止某个节点出现网络分区后，把term 向前推进.