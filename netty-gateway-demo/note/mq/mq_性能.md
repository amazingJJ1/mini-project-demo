kafka高性能原因

#### 生产者

Kafka会把收到的消息都写入到硬盘中，它绝对不会丢失数据。为了优化写入速度Kafak采用了两个技术，
**顺序写入和MMFile**。

顺序写入因为硬盘是机械结构，每次读写都会寻址->写入，其中寻址是一个“机械动作”，它是最耗时的。
所以硬盘最“讨厌”随机I/O，最喜欢顺序I/O。为了提高读写硬盘的速度，Kafka就是使用顺序I/O。收到消息后Kafka会把数据插入到文件末尾。
这种方法有一个缺陷——没有办法删除数据，所以Kafka是不会删除数据的，它会把所有的数据都保留下来，
每个消费者（Consumer）对每个Topic都有一个offset用来表示读取到了第几条数据。

Memory Mapped FilesKafka的数据并不是实时的写入硬盘，它充分利用了现代操作系统分页存储来利用内存提高I/O效率。
Memory Mapped Files也被翻译成内存映射文件，在64位操作系统中一般可以表示20G的数据文件，它的工作原理是直接利用操作系统的Page来实现文件到物理内存的直接映射。
完成映射之后你对物理内存的操作会被同步到硬盘上（操作系统在适当的时候）。

这种方法也有一个很明显的缺陷——不可靠，写到mmap中的数据并没有被真正的写到硬盘，操作系统会在程序主动调用flush的时候才把数据真正的写到硬盘。
Kafka提供了一个参数——producer.type来控制是不是主动flush，如果Kafka写入到mmap之后就立即flush然后再返回Producer叫同步(sync)；
写入mmap之后立即返回Producer不调用flush叫异步(async)。

消费者
zero copy传统read/write方式进行网络文件传输的方式，文件数据实际上是经过了四次copy操作：

> **硬盘—>内核buf—>用户buf—>socket相关缓冲区—>协议引擎**

kafka基于sendfile实现Zero Copy，直接从内核空间（DMA的）到内核空间（Socket的），然后发送网卡。

批量压缩

在很多情况下，系统的瓶颈不是CPU或磁盘，而是网络IO。
进行数据压缩会消耗少量的CPU资源，不过对于kafka而言，网络IO更应该需要考虑。
Kafka使用了批量压缩，即将多个消息一起压缩而不是单个消息压缩。
Kafka允许使用递归的消息集合，批量的消息可以通过压缩的形式传输并且在日志中也可以保持压缩格式，直到被消费者解压缩。

Kafka支持多种压缩协议，包括Gzip和Snappy压缩协议。


rocketMq高性能原因

#### 生产者

顺序写入消息存储是由ConsumeQueue和CommitLog配合完成的。
一个Topic里面有多个MessageQueue，每个MessageQueue对应一个ConsumeQueue。
ConsumeQueue里记录着消息物理存储地址。
CommitLog就存储文件具体的字节信息。
文件大小默认1g，文件名称20位数，左边补0右边为偏移量。
消息顺序写入文件，文件满了则写入下一个文件。

####  消费者

随机读

每次读消息时先读逻辑队列consumQue中的元数据，再从commitlog中找到消息体。
但是入口处rocketmq采用package机制，可以批量地从磁盘读取，作为cache存到内存中，加速后续的读取速度。
随机读具体流程

- Consumer每20s重新做一次负载均衡更新，根据从Broker存储的ConsumerGroup和Topic信息，
把MessageQueue分发给不同的Consumer，负载策略默认是分页每个MessageQueue对应一个pullRequest，
全部存储到该Consumer的pullRequestQueue队列里面。

- Consumer启动独立后台PullMessageService线程，不停的尝试从pullRequestQueue.take()获取PullRequest。

- 捞取到PullRequest会先做缓存校验（默认一个Queue里面缓存待处理消息个数不超过1000个，消息大小不超过100M，否则会延迟50ms再重试），
从而保证客户端的缓存负载不会过高。

- PullRequest发送给Broker，如果Broker发现该Queue有待处理的消息，就会直接返回给Consumer，Consumer接收响应以后，
重新把该PullRequest丢到自己的pullRequestQueue队列里面,从而重复执行捞取消息的动作，保证消息的及时性。

- PullRequest发送给Broker，如果Broker发现该Queue没有待处理的消息，则会Hold住这个请求，暂不响应给Consumer，
默认长轮询是5s重试获取一次待处理消息，如果有新的待处理消息则立刻Response给Consumer，
当客户端检测到消息挂起超时（客户端有默认参数 响应超时时间 20s），会重新发起PullRequest给Broker。

###消费模型
常见消费模型有以下几种：

- push：producer发送消息后，broker马上把消息投递给consumer。

这种方式好在实时性比较高，但是会增加broker的负载；而且消费端能力不同，如果push推送过快，消费端会出现很多问题。

- pull：producer发送消息后，broker什么也不做，等着consumer自己来读取。
它的优点在于主动权在消费者端，可控性好；但是间隔时间不好设置，间隔太短浪费资源，间隔太长又会消费不及时。

- 长轮询：当consumer过来请求时，broker会保持当前连接一段时间 默认15s,如果这段时间内有消息到达，则立刻返回给consumer；
15s没消息的话则返回空然后重新请求。这种方式的缺点就是服务端要保存consumer状态，客户端过多会一直占用资源。

RocketMQ默认是采用pushConsumer方式消费的，从概念上来说是推送给消费者，它的本质是**pull+长轮询**。
这样既通过长轮询达到了push的实时性，又有了pull的可控性。

系统收到消息后会自动处理消息和offset(消息偏移量)，如果期间有新的consumer加入会自动做负载均衡
**(集群模式下offset存在broker中; 广播模式下offset存在consumer里)**。

当然我们也可以设置为pullConsumer模式，这样灵活性会提高，但是代码却会很复杂，需要手动维护offset，消息存储和状态。

#### zero copy

零拷贝技术有mmap及sendfile，sendfile大文件传输快，mmap小文件传输快。MMQ发送的消息通常都很小，rocketmq就是以mmap+write方式实现的。

#### 为什么kafka比RocketMQ吞吐量更高

kafka性吞吐量更高主要是由于Producer端将多个小消息合并，批量发向Broker。

kafka采用异步发送的机制，当发送一条消息时，消息并没有发送到broker而是缓存起来，然后直接向业务返回成功，
当缓存的消息达到一定数量时再批量发送。

此时减少了网络io，从而提高了消息发送的性能，但是如果消息发送者宕机，会导致消息丢失，业务出错，所以理论上kafka利用此机制提高了io性能却降低了可靠性。

#### RocketMQ为何无法使用同样的方式？

RocketMQ通常使用的Java语言，缓存过多消息会导致频繁GC。

Producer调用发送消息接口，消息未发送到Broker，向业务返回成功，此时Producer宕机，会导致消息丢失，业务出错。

Producer通常为分布式系统，且每台机器都是多线程发送，我们认为线上的系统单个Producer每秒产生的数据量有限，不可能上万。
缓存的功能完全可以由上层业务完成。

#### 为什么选择RocketMQ

当broker里面的topic的partition数量过多时，kafka的性能却不如rocketMq。
kafka和rocketMq都使用文件存储，但是kafka是一个分区一个文件，当topic过多，分区的总量也会增加，
kafka中存在过多的文件，**当对消息刷盘时，就会出现文件竞争磁盘**，出现性能的下降。一个partition（分区）一个文件，顺序读写。
一个分区只能被一个消费组中的一个 消费线程进行消费，因此可以同时消费的消费端也比较少。

rocketMq所有的队列都存储在一个文件中，每个队列的存储的消息量也比较小，因此topic的增加对rocketMq的性能的影响较小。
rocketMq可以存在的topic比较多，可以适应比较复杂的业务。