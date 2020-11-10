spring cloud stream流程解析

一、设计抽象

二、执行流程

简单描述是：

动态注册bean definition ----> bind service 绑定服务 (获取spi实现类)------> 执行绑定（实例化producer consumer的过程）

1. @EnableBinding 触发动态注册，将Sink和Source的bean定义注册到bean Factory中

2. StreamListen注册DirectWithAttributesChannel

3. 获取MessageChannelBinder的实现类，比如我们是用rocketmq的binder实现类就是RocketMQMessageChannelBinder

4. Spring application Context完成时，触发onRefresh()方法，引发 getLifecycleProcessor().onRefresh()，开始OutputBindingLifecycle的start()方法，最终触发BindService执行bindProducer()/binderConsumer()的过程

5. 这里先看下绑定消费者的过程doBindConsumer

   - 创建RocketMQListenerBindingContainer ，作为comsumer的容器。默认的消费者实现是DefaultMQPushConsumer
   - 包装成RocketMQInboundChannelAdapter
   - 触发adpater实例的初始化和他作为lifecycle的start()方法，本质是启动dedaultMqPushComsumer的客户端，开始消费。具体的实例 是mqClientFactory
   - netty监听到sockect消息后 MQClientAPIImpl异步拉取消息,放入本地队列，DefaultMQPushConsumerz最终会消费到者条消息，通过org.springframework.cloud.stream.binder.rocketmq.consuming.RocketMQListenerBindingContainer.DefaultMessageListenerConcurrently#consumeMessage消费

   

