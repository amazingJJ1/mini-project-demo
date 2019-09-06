## 初步设计

---
### 1.io处理，全流程异步

- 代理转发使用netty client,复用channel,后面参考netty channel pool
   
   - proxy client channel 空闲检测
   - proxy client channel 获取时的阻塞策略，如何异步化处理
   - channel的释放及关闭处理
- rxjava 进行流背压处理 | netty本身的高低水位流控
   
   - 参考zuul2的背压处理
   - rxNetty的异步调用，futrue和Promise的使用经验



### 2.监控
- Metrics 服务接入Prometheus
- 调用链接入Skywalking?待确认

### 3.责任链设计
- 扩展性设计，预留接口+pipeline

### 4.日志处理
- jul
- slf4j+logback
### 5.管理平台

### 6.与springboot整合，简化部署

- 与springboot actuator整合 内嵌一个tomcat
