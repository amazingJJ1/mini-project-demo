## mq选举
### kafka
kafka选举分为3部分
1. broken选举

    zk选举
    
2. partition选举（分区副本选举机制）
    
    在kafka的集群中，会存在着多个主题topic，在每一个topic中，又被划分为多个partition，为了防止数据不丢失，
    每一个partition又有多个副本，在整个集群中，总共有三种副本角色：
    
    - 首领副本（leader）：
    
        也就是leader主副本，每个分区都有一个首领副本，为了保证数据一致性，所有的生产者与消费者的请求都会经过该副本来处理。
        
    - 跟随者副本（follower）：
    
        除了首领副本外的其他所有副本都是跟随者副本，跟随者副本不处理来自客户端的任何请求，只负责从首领副本同步数据，
        保证与首领保持一致。如果首领副本发生崩溃，就会从这其中选举出一个leader。
    
    - 首选首领副本：
    
        创建分区时指定的首选首领。如果不指定，则为分区的第一个副本。
    
        follower需要从leader中同步数据，但是由于网络或者其他原因，导致数据阻塞，出现不一致的情况，为了避免这种情况，
        follower会向leader发送请求信息，这些请求信息中包含了follower需要数据的偏移量offset，而且这些offset是有序的。
        如果有follower向leader发送了请求1，接着发送请求2，请求3，那么再发送请求4，这时就意味着follower已经同步了前三条数据，否则不会发送请求4。
        leader通过跟踪 每一个follower的offset来判断它们的复制进度。
        默认的，如果follower与leader之间超过10s内没有发送请求，或者说没有收到请求数据，此时该follower就会被认为“不同步副本”。
        而持续请求的副本就是“同步副本”，当leader发生故障时，只有“同步副本”才可以被选举为leader。
        其中的请求超时时间可以通过参数replica.lag.time.max.ms参数来配置。
    
        我们希望每个分区的leader可以分布到不同的broker中，尽可能的达到负载均衡，所以会有一个首选首领，
        如果我们设置参数auto.leader.rebalance.enable为true，那么它会检查首选首领是否是真正的首领，如果不是，则会触发选举，让首选首领成为首领。
    
        这里的副本复制就是**ISR(In-Sync-Replicas)**：后面分析
       
3. 消费组选举

    在kafka的消费端，会有一个消费者协调器以及消费组，组协调器GroupCoordinator需要
    为消费组内的消费者选举出一个消费组的leader，那么如何选举的呢？

    如果消费组内还没有leader，那么第一个加入消费组的消费者即为消费组的leader，
    如果某一个时刻leader消费者由于某些原因退出了消费组，那么就会重新选举leader，如何选举？

    >private val members = new mutable.HashMap[String, MemberMetadata]
    
    > leaderId = members.keys.headOption

    上面代码是kafka源码中的部分代码，member是一个hashmap的数据结构，key为消费者的member_id，value是元数据信息，
    那么它会将leaderId选举为Hashmap中的第一个键值对，它和随机基本没啥区别。
    
    
### kakfa ISR
#### kafka replication
1. 当某个topic的replication-factor为N且N大于1时，每个Partition都会有N个副本(Replica)。kafka的replica包含leader与follower。
2. Replica的个数小于等于Broker的个数，也就是说，对于每个Partition而言，每个Broker上最多只会有一个Replica，因此可以使用Broker id 指定Partition的Replica。
3. 所有Partition的Replica默认情况会均匀分布到所有Broker上。

#### kafka replication pull
每个Partition有一个leader与多个follower，producer往某个Partition中写入数据是，只会往leader中写入数据，然后数据才会被复制进其他的Replica中。
 
数据是由leader push过去还是有flower pull过来？ 

kafka是由follower周期性或者尝试去pull(拉)过来(其实这个过程与consumer消费过程非常相似)，
读写都是在leader上，flower只是数据的一个备份，保证leader被挂掉后顶上来，并不往外提供服务。

#### kafka replication commit (ISR)
- 同步复制： 只有所有的follower把数据拿过去后才commit，一致性好，可用性不高。 
- 异步复制： 只要leader拿到数据立即commit，等follower慢慢去复制，可用性高，立即返回，一致性差一些。 
Commit：是指leader告诉客户端，这条数据写成功了。kafka尽量保证commit后立即leader挂掉，其他flower都有该条数据。

kafka不是完全同步，也不是完全异步，是一种ISR机制： 

1. leader会维护一个与其基本保持同步的Replica列表，该列表称为ISR(in-sync Replica)，每个Partition都会有一个ISR，而且是由leader动态维护 
2. 如果一个flower比一个leader落后太多，或者超过一定时间未发起数据复制请求，则leader将其重ISR中移除 
3. 当ISR中所有Replica都向Leader发送ACK时，leader才commit

既然所有Replica都向Leader发送ACK时，leader才commit，那么flower怎么会leader落后太多？ 

producer往kafka中发送数据，不仅可以一次发送一条数据，还可以发送message的数组；
批量发送，同步的时候批量发送，异步的时候本身就是就是批量；底层会有队列缓存起来，批量发送，
对应broker而言，就会收到很多数据(假设1000)，这时候leader发现自己有1000条数据，flower只有500条数据，
落后了500条数据，就把它从ISR中移除出去，这时候发现其他的flower与他的差距都很小，就等待；
如果因为内存等原因，差距很大，就把它从ISR中移除出去。

#### 问题思考
1. Data Replication如何处理Replica恢复

    leader挂掉了，从它的follower中选举一个作为leader，并把挂掉的leader从ISR中移除，继续处理数据。
    一段时间后该leader重新启动了，它知道它之前的数据到哪里了，尝试获取它挂掉后leader处理的数据，获取完成后它就加入了ISR

2. Data Replication如何处理Replica全部宕机

    1. 等待ISR中任一Replica恢复,并选它为Leader
       
       等待时间较长,降低可用性，或ISR中的所有Replica都无法恢复或者数据丢失,则该Partition将永不可用
       
    2. 选择第一个恢复的Replica为新的Leader,无论它是否在ISR中 
     
        并未包含所有已被之前Leader Commit过的消息,因此会造成数据丢失
        可用性较高
### rocket
这个就简单了，用的raft选举，日志复制也是一样