# EasyScheduler 源码解析

## easyScheduler基本组件

一共如下组件

- ui 前端操作
- api 提供UI api接口，主要进行工作流操作，将操作抽象成command入库
- master 分析工作流定义，解析并存储job(工作流实例)和trigger,quartz定时触发后，master将job分解成DAG的任务节点，并将task入库
- work 执行DAG的最小task任务
- rpc logger 和work组件部署同一台机器，仅提供日志查看功能，可将两个组件合并
- alert 扫描告警表，推送告警信息
- zk 只负责以上组件的注册，提供分布式锁功能，可替换其他包含分布式锁的注册中心组件，比如etcd,consoul等。

## 一、api部分

主要是提供用户，租户，队列，项目，工作流定义等基础信息的增删改查，没有太复杂的逻辑，另外集成logClient,对接rpc log提供日志查看服务。

额外的还有对其他组件的监控，通过从zk获取其他组件节点ip，然后长连接获取组件信息

## 二、master 部分

主要负责定时任务的启停操作，当quartz调起任务后，Master内部会有线程池具体负责处理任务的后续操作

- **MasterSchedulerThread**：定时扫描数据库中的 command 表，根据不同的命令类型进行不同的业务操作
- **MasterExecThread**：DAG任务切分、任务提交监控、各种不同命令类型的逻辑处理
- **MasterTaskExecThread**：负责任务的持久化

个人认为master是easheduler最核心的部分

1. 入口是masterServer,启动的时候开启zk心跳检测线程
2. quartz初始化
3. 添加优雅停机的shutdownHook
4. masterSchedulerThread启动，开始扫描数据库command

#### masterscheduler线程流程：

1. 获取zk分布式锁，zookeeper.escheduler.lock.masters，保证每次只有一个master节点可以扫描command表
2. 扫描command表时一次只取一个command，会根据线程池的活跃线程数来取相应的command数量
3. 扫描后会将command根据不同commandType构造成processInstance入库持久化，并删除command
4. 接着processInstance被扔给MasterExecThread进行处理，进入masterExecThread流程
5. 线程休眠一秒，减少扫描command表频率，释放分布式锁

#### masterExecThread流程

1. 检查processInstance状态

2. 执行工作流实例，分3个阶段：

   - 准备阶段：

     获取该工作流实例下的任务实例，即taskInstance。

     分析工作流定义，将TaskNode构建成processDag

   - 运行: 分析taskInstance的依赖关系，确定是否执行，如果需要执行则将TaskInstance抛给MasterTaskExecThread进行持久化，更新processInstance状态

   - 结束： 校验和告警

3. 结束ProcessInstance流程，如果是开发模式，会在本地建立工作目录，非开发模式会在运行完毕删除这些工作目录。

关于**DAG检验**：

> 这里使用的是kahn算法，利用有向无环图的的方向性原理：
>
> 比如从源点开始，入度为0，删出该点后，源点指向的点成为新源点，入度也为0。
>
> 依次递归删除，如果删除后还剩下点且入度不为0，表示是有环的，反正则是无环图。
>
> 一言以蔽之，从头删到尾。

## 三、work部分

主要负责任务的执行和提供日志服务。

WorkerServer服务启动时向Zookeeper注册临时节点，并维持心跳

- **FetchTaskThread**： 从zk队列中拉取任务
- **TaskScheduleThread**：  根据任务类型执行任务，比如ShellTask

#### zk拉取任务流程：

1. 检验工作节点的资源（设置的cpu load,可使用的内存等），资源不够则等下一轮循环

2. 拿到zk队列下的所有任务，即tasks_queue目录下的节点。如果获取的tasklist为空，继续下一轮循环

3. 创建zk的work节点分布式锁，zookeeper.escheduler.lock.workers。分布式锁保证任务只会被取一次，但是如果锁失效后重建，如何保证任务不会重复执行呢？

   easyscheduler是使用curator创建分布式锁，使用的是临时节点，客户端宕机则删除节点，没有分布式死锁的问题。但是这里还有其他几个个问题：

   	-  不配置zookeeper.escheduler.lock.workers的话，所有work节点都需要争抢同一个分布式锁。同时只有一个work节点在拉取任务，保证了安全性，但执行效率略低。如果可以让master下发任务时指定工作节点，work节点可以不需要分布式锁。master另外添加任务空闲检测功能，将长时间机器不执行的任务转移到活的工作节点上。
   	-  客户端宕机恢复后，任务可能被重复执行

4. 根号任务字符串从数据库获取taskInstance。并标记执行的节点ip，执行时间等信息，创建linux工作目录。提交给TaskScheduleThread执行

5. 任务执行完毕后从zk队列删除

6. 删除分布式锁

7. 下一轮循环

#### 任务执行

根据任务类型执行任务，比如shell脚本执行：

1. 从配置的hdfs中下载执行文件到本地
2. 获取工作流定义，工作流实例、租户信息
3. 替换任务参数
4. 任务执行前处理
5. 任务执行。shell任务即执行shell脚本，在执行前会替换变量参数
6. 任务执行后处理

## master与quartz结合原理

### quartz部分

#### 基本组件：

- **job**  是一个接口，只有一个方法void execute(JobExecutionContext context)，开发自定义任务的实现
- **trigger** 触发器，用于定义任务调度的时间规则，有SimpleTrigger,CronTrigger,DateIntervalTrigger和NthIncludedDayTrigger，其中CronTrigger用的比较多，本文主要介绍这种方式。CronTrigger在spring中封装在CronTriggerFactoryBean中。
  - 一个job可关联多个trigger，而一个trigger只能关联一个job。这个很好理解，任务可以被不同的时间规则触发，而一个时间规则一次只能触发一个任务
- **scheduler** 任务调度器，**将job和trigger关联起来**,是实际执行任务调度的控制器。在spring中通过SchedulerFactoryBean封装起来

简单示例

```java
        SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
        Scheduler sched = schedFact.getScheduler();
        sched.start();
        // define the job and tie it to our HelloJob class
        JobDetail job = newJob(HelloJob.class)
                .withIdentity("myJob", "group1")
                .build();
        // Trigger the job to run now, and then every 40 seconds
        trigger = newTrigger()
                .withIdentity("myTrigger", "group1")
                .startNow()
                .withSchedule(simpleSchedule()
                        .withIntervalInSeconds(40)
                        .repeatForever())
                .build();
        // Tell quartz to schedule the job using our trigger
        sched.scheduleJob(job, trigger);
```

#### Scheduler 调度线程

主要有两个：

- 执行常规调度的线程
- 执行 misfired trigger 的线程

常规调度线程轮询存储的所有 trigger，如果有需要触发的 trigger，即到达了下一次触发的时间，则从任务执行线程池获取一个空闲线程，执行与该 trigger 关联的任务。Misfire 线程是扫描所有的 trigger，查看是否有 misfired trigger，如果有的话根据 misfire 的策略分别处理

#### 数据存储

Quartz 中的 trigger 和 job 需要存储下来才能被使用。

Quartz 中有两种存储方式：

-  RAMJobStore 是将 trigger 和 job 存储在内存中
- JobStoreSupport 是基于 jdbc 将 trigger 和 job 存储到数据库中。

RAMJobStore 的存取速度非常快，但是由于其在系统被停止后所有的数据都会丢失，所以在通常应用中，都是使用 JobStoreSupport。在 Quartz 中，JobStoreSupport 使用一个驱动代理来操作 trigger 和 job 的数据存储：StdJDBCDelegate。StdJDBCDelegate 实现了大部分基于标准 JDBC 的功能接口，但是对于各种数据库来说，需要根据其具体实现的特点做某些特殊处理，因此各种数据库需要扩展 StdJDBCDelegate 以实现这些特殊处理。Quartz 已经自带了一些数据库的扩展实现：

- DB2V8Delegate
- PostgreSqlDelegate
- MYSQLDelegate

#### misfireHandler线程

misfire产生需要有2个前置条件

- 一个是job到达触发时间时没有被执行
- 二是被执行的延迟时间超过了Quartz配置的misfireThreshold阀值。

如果延迟执行的时间小于阀值，则Quartz不认为发生了misfire，立即执行job；如果延迟执行的时间大于或者等于阀值，则被判断为misfire，然后会按照指定的策略来执行。



产生misfire的原因有以下4点：

1. 当job达到触发时间时，所有线程都被其他job占用，没有可用线程。

2. 在job需要触发的时间点，scheduler停止了（可能是意外停止的）。

3. job使用了@DisallowConcurrentExecution注解，job不能并发执行，当达到下一个job执行点的时候，上一个任务还没有完成。

4. job指定了过去的开始执行时间，例如当前时间是8点00分00秒，指定开始时间为7点00分00秒。

   

Quartz 中为 trigger 定义了处理策略，主要有下面两种：

- **MISFIRE_INSTRUCTION_FIRE_ONCE_NOW**：针对 misfired job 马上执行一次；
- **MISFIRE_INSTRUCTION_DO_NOTHING**：忽略 misfired job，等待下次触发；

默认是MISFIRE_INSTRUCTION_SMART_POLICY，该策略在CronTrigger中和MISFIRE_INSTRUCTION_FIRE_ONCE_NOW一样，线程默认1分钟执行一次；在一个事务中，默认一次最多recovery 20个；

#### QuartzSchedulerThread线程

QuartzSchedulerThread线程是quartz执行的核心线程，贯穿quartz整个调度逻辑：

1. 先获取线程池中的可用线程数量（若没有可用的会阻塞，直到有可用的。

2. 获取30m内要执行的trigger(即acquireNextTriggers)：

   - 获取trigger的锁，通过select …**for update**方式实现；

   - 获取30m内（可配置）要执行的triggers（**需要保证集群节点的时间一致**），若**@ConcurrentExectionDisallowed**且列表存在该条trigger则跳过，否则更新trigger状态为ACQUIRED(刚开始为WAITING)；插入firedTrigger表，状态为ACQUIRED;

     > 在RAMJobStore中，有个timeTriggers，**排序方式是按触发时间nextFireTime排的**。JobStoreSupport从数据库取出triggers时是按照nextFireTime排序）;

3. 等待直到获取的trigger中最先执行的trigger在2ms内；

4. triggersFired：

   1. 更新firedTrigger的status=EXECUTING;
   2. 更新trigger下一次触发的时间；
   3. 更新trigger的状态：无状态的trigger->WAITING，有状态的trigger->BLOCKED，若nextFireTime==null ->COMPLETE。
   4.  commit connection,释放锁；

5. 针对每个要执行的trigger，创建JobRunShell，并放入线程池执行：

   1. execute:执行job
   2. 获取TRIGGER_ACCESS锁
   3. 若是有状态的job：更新trigger状态：BLOCKED->WAITING,PAUSED_BLOCKED->BLOCKED
   4. 若@PersistJobDataAfterExecution，则updateJobData
   5. 删除firedTrigger
   6. commit connection，释放锁

### quartz和easyScheduler联系

通过quartz原理部分可以大致了解，easyScheduler将工作流定义视为job，工作流实例上面设置触发器，相当于触发器和job绑定。quartz触发工作流实例的调度后，master将工作流的任务分析组合成DAG，拆分成task，交给work节点执行

