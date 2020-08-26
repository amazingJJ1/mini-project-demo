## rocket mq 负载均衡

一、路由信息
路由记录了broker集群节点的通讯地址、broker名称和读写队列数量等信息。

- 写队列writequque
    表示生产者可以写入的队列个数，如果不做特别配置默认是4，队列号从0开始，如果是4个，queueId就是0，1，2，3。
    broker收到消息后根据queueId生成消息队列。生产者负载均衡的过程的实质就是选择broker集群和queueId的过程。
    
-  读队列readqueue
    表示broker中可以供消费者读去消息的队列个数，默认也是4个，队列号从0开始。
    消费者拿到路由信息后会选择queueId，从对应的broker中读取数据消费。
    
## 路由的数据结构
- TopicRouteData 
    topic有关系的broker节点信息，内部包含多个QueueData对象（可以有多个broker集群支持该topic）
    和多个BrokerData信息（多个集群的多个节点信息都在该列表中）。）和brokerName
- QueueData 记录了broker名称、写队列个数与读队列个数
- BrokerData 记录broker集群所有节点信息，如所有节点的ip、brokerid（每个节点都有一个brokerId，brokerId是0表示master节点

## messageQueue
messageQueue是生产消费负载均衡的核心
包含以下信息：
- topic
- brokenName
- queueId

### broken 负载均衡
先看broken的集群模式：
- 单 master 模式：
    - 优点：除了配置简单没什么优点，适合个人学习使用。
    - 缺点：不可靠，该机器重启或宕机，将导致整个服务不可用。

- 多 master 模式：
    多个 master 节点组成集群，单个 master 节点宕机或者重启对应用没有影响。
    - 优点：所有模式中性能最高
    - 缺点：单个 master 节点宕机期间，未被消费的消息在节点恢复之前不可用，消息的实时性就受到影响。

注意：使用同步刷盘可以保证消息不丢失，同时 **Topic相对应的queue 应该分布在集群中各个节点**(即创建topic的时候选择多个broken)，而不是只在某各节点上，
否则，该节点宕机会对订阅该 topic 的应用造成影响。

创建topic和queue注意事项
> 1. 拥有一致的brokerName的broker节点上的队列信息是一致的。
    brokerId为0的为master，其余为slave，master可以写/读， slave只可以读，slave无法自动升级为master
> 2. 创建主题时可以选择创建在多个主节点（brokerName不一样）上，
    不同主节点上的队列是独立的，均可以提供读写操作，具体如何分布在多个主节点上是根据用户自己设置的。
> 3. 不建议自动创建主题，自动创建主题时它的分布情况可能会不一致，或者与想象中不一样。
> 4. writeQueueNum和readQueueNum建议一致，否则可能会导致消息无法被消费。

- 多 master 多 slave 异步复制模式：
    在多 master 模式的基础上，每个 master 节点都有至少一个对应的 slave。master
    节点可读可写，但是 slave 只能读不能写，类似于 mysql 的主备模式。
    - 优点： 在 master 宕机时，消费者可以从 slave读取消息，消息的实时性不会受影响，性能几乎和多 master 一样。
    - 缺点：使用异步复制的同步方式有可能会有消息丢失的问题。

- 多 master 多 slave 同步双写模式：
    同多 master 多 slave 异步复制模式类似，区别在于 master 和 slave 之间的数据同步方式。
    - 优点：同步双写的同步模式能保证数据不丢失。
    - 缺点：发送单个消息 RT 会略长，性能相比异步复制低10%左右。

刷盘策略：同步刷盘和异步刷盘（指的是节点自身数据是同步还是异步存储）
同步方式：同步双写和异步复制（指的一组 master 和 slave 之间数据的同步）

注意：要保证数据可靠，需采用同步刷盘和同步双写的方式，但性能会较其他方式低。

broken负载均衡：
Broker是以group为单位提供服务。
一个group里面分master和slave,master和slave存储的数据一样，slave从master同步数据（同步双写或异步复制看配置）。

通过nameserver暴露给客户端后，只是客户端关心（注册或发送）一个个的topic路由信息。
路由信息中会细化为message queue的路由信息。而message queue会分布在不同的broker group。

所以对于客户端来说，分布在不同broker group的message queue为成为一个服务集群，但客户端会把请求分摊到不同的queue。
而由于压力分摊到了不同的queue,不同的queue实际上分布在不同的Broker group，也就是说压力会分摊到不同的broker进程，这样消息的存储和转发均起到了负载均衡的作用。Broker一旦需要横向扩展，只需要增加broker group，然后把对应的topic建上，客户端的message queue集合即会变大，这样对于broker的负载则由更多的broker group来进行分担。并且由于每个group下面的topic的配置都是独立的，也就说可以让group1下面的那个topic的queue数量是4，其他group下的topic queue数量是2，这样group1则得到更大的负载。

### 生产者负载均衡

生产者负载均衡实质上是在选择MessageQueue对象（内部包含topic，brokerName，queueId）。有两种策略：
- 默认策略，从MessageQueue列表中随机选择一个，实现过程是通过自增随机数对列表大小取余获取位置信息，但获得的MessageQueue所在的集群不能是上次的失败集群。
- 集群超时容忍策略，先随机选择一个MessageQueue，如果因为超时等异常发送失败，会优先选择该broker集群下其他的messeagequeue进行发送；
    如果没有找到则从之前发送失败broker集群中选择一个MessageQueue进行发送；如果还没有找到则使用默认策略
 
分析：当broker集群A接收数据超时后，生产者采用默认策略会选出另一个broker集群B，
B的流量会瞬间出现峰值，给服务带来抖动，因此**超时容忍策略的作用就会突出，虽然短时间内发送会失败，但不会对其他集群带来波峰流量**   

### 消费者负载均衡
六种负载策略
同样也是可以根据messageQueue去进行负载，也可以通过自定义配置负载。
- 平均消费算法 ： AllocateMessageQueueAveragely
     MessageQueue列表是topic下可以拉去消息的队列，消费客户端是订阅topic的消费者，当消息队列个数小于可消费客户端时，消息队列与客户端对应情况如下；
     
     mq1 mq2 mq3  c1  c2  c3  c4  c5
     
     c1,c4 -->mq1
     c2,5 -->mq2
     c3 -->mq3
          
     当消息队列个数大于可消费客户端时，消息队列与客户端对应情况如下。
     
     mq1 mq2 mq3 mq4 mq5  c1  c2  c3
     
     c1 -->mq1,mq4
     c1 -->mq2,mq5
     c1 -->mq3

- AllocateMessageQueueAveragelyByCircle 轮询分配
    轮询分配。例如队列MQ1、2、3、4、5、6、7、8， 消费者C1、2、3，
    则C1消费MQ1、4、7，
    C2消费MQ2、5、8，C3消费MQ3、6
    
- 临近机房分配策略 AllocateMachineRoomNearby算法
    首先统计消费者与broker所在机房，**保证broker中的消息优先被同机房的消费者消费**。
    如果机房中没有消费者，则有其他机房消费者消费。
    实际的队列分配（同机房或跨机房）可以是指定其他算法。
    假设有三个机房，实际负载策略使用算法1，机房1和机房3中存在消费者，机房2没有消费者。机房1、机房3中的队列会分配给各自机房中的消费者，机房2的队列会被所有的消费者平均分配。
    
- AllocateMessageQueueByConfig 根据配置消费

- AllocateMessageQueueByMachineRoom 机房分配
 
 
- AllocateMessageQueueConsistentHash 一致性哈希
     使用一致性哈希算法进行负载，每次负载都会重新创建一致性hash路由表，获取本地客户端负责的所有队列信息。
     RocketMQ默认的hash算法为MD5。假设有4个客户端的clientId和两个消息队列mq1，mq2，
     通过hash后分布在hash环的不同位置，按照一致性hash的顺时针查找原则，mq1被client2消费，mq2被client3消费。
     
     
## consumer group
Rocketmq中有ConsumerGroup的概念。在集群模式下，多台服务器配置相同的ConsumerGroup，
能够使得每次只有一台服务器消费消息（注意，但不保证只消费一次，存在网络抖动的情况）

一个Consumer Group中的各个Consumer实例分摊去消费消息，即一条消息只会投递到一个Consumer Group下面的一个实例。
实际上，每个Consumer是平均分摊Message Queue的去做拉取消费。

例如某个Topic有3条Q，其中一个Consumer Group 有 3 个实例（可能是 3 个进程，或者 3 台机器），那么每个实例只消费其中的1条Q。


Consumer Group标识一类Consumer的集合名称，这类Consumer通常消费一类消息，且消费逻辑一致。
同一个Consumer Group下的各个实例将共同消费topic的消息，起到负载均衡的作用。
消费进度以Consumer Group为粒度管理，不同Consumer Group之间消费进度彼此不受影响，
即消息A被Consumer Group1消费过，也会再给Consumer Group2消费。

注： RocketMQ要求同一个Consumer Group的消费者必须要拥有相同的注册信息，即必须要听一样的topic(并且tag也一样)

#### 广播消费
消息将对一个Consumer Group下的各个Consumer实例都投递一遍。
即即使这些 Consumer 属于同一个Consumer Group，消息也会被Consumer Group 中的每个Consumer都消费一次。

实际上，是一个消费组下的每个消费者实例都获取到了topic下面的每个Message Queue去拉取消费。
所以消息会投递到每个消费者实例。这种模式下，消费进度会存储持久化到实例本地。


### 顺序消费
在实际开发有些场景中，我并不需要消息完全按照完全按的先进先出，而是某些消息保证先进先出就可以了。

就好比一个订单涉及 订单生成，订单支付、订单完成。我不用管其它的订单，只保证同样订单ID能保证这个顺序就可以了。
我们知道 生产的message最终会存放在Queue中，如果一个Topic关联了16个Queue,如果我们不指定消息往哪个队列里放，那么默认是平均分配消息到16个queue，

好比有100条消息，那么这100条消息会平均分配在这16个Queue上，那么每个Queue大概放5～6个左右。这里有一点很重的是:

同一个queue，存储在里面的message 是按照先进先出的原则
这个时候思路就来了，好比有orderId=1的3条消息，分别是 订单生产、订单付款、订单完成。只要保证它们放到同一个Queue那就保证消费者先进先出了。
这就保证局部顺序了，即同一订单按照先后顺序放到同一Queue,那么取消息的时候就可以保证先进先取出。

那么全局消息呢？这个就简单啦，你把所有消息都放在一个Queue里,这样不就保证全局消息了。
就这么简单当然不是，这里还有很关键的一点，好比在一个消费者集群的情况下，消费者1先去Queue拿消息，它拿到了 订单生成，它拿完后，消费者2去queue拿到的是 订单支付。

拿的顺序是没毛病了，但关键是先拿到不代表先消费完它。会存在虽然你消费者1先拿到订单生成，但由于网络等原因，消费者2比你真正的先消费消息。这是不是很尴尬了。
订单付款还是可能会比订单生成更早消费的情况。那怎么办。

分布式锁来了
**Rocket采用的是分段锁，它不是锁整个Broker而是锁里面的单个Queue，因为只要锁单个Queue就可以保证局部顺序消费了**。

所以最终的消费者这边的逻辑就是
消费者1去Queue拿 订单生成，它就锁住了整个Queue，只有它消费完成并返回成功后，这个锁才会释放。
然后下一个消费者去拿到 订单支付 同样锁住当前Queue,这样的一个过程来真正保证对同一个Queue能够真正意义上的顺序消费，而不仅仅是顺序取出。

生产者生产消息的时候根据orderId选择queueId,保证一个订单的消息只会发到一个队列。修改messageQueueSelector的策略即可
消费者这边可以用rocket本身支持的顺序消费模式MessageListenerOrderly（有序消费）.
也可以继续并发消费：不过**MessageListenerConcurrently就需要使用单线程去消费消息来保证顺序了**。
（这里假设触发了重排导致queue分配给了别人也没关系，由于queue的消息永远是FIFO，最多只是已经消费的消息重复而已，queue内顺序还是能保证）

但的确会有一些异常场景会导致乱序。如master宕机，导致写入队列的数量上出现变化。
如果还是沿用取模的seletor，就会一批订单号的消息前面散列到q0,后面的可能散到q1，这样就不能保证顺序了。
除非选择牺牲failover特性，如master挂了无法发通接下来那批消息。
从消费端，如果想保证这批消息是M1消费完成再消费M2的话，可以使用MessageListenerOrderly接口，但是这样的话会有以下问题：
1. 遇到消息失败的消息，无法跳过，当前队列消费暂停，需要额外处理
2. 目前版本的RocketMQ的MessageListenerOrderly是不能从slave消费消息的

注意2：还需要额外设置consumer.setVipChannelEnabled(false);
