# 文档
## 初步设计
1.io处理，全流程异步

- 代理转发使用netty client【一期任务】
- rxjava 进行流背压处理

   - 解决channel申请的内存释放
   - clinet channel的空闲检测,空闲读、写后进行channel关闭
   

2.与springboot整合，简化部署【一期任务】

- 与springboot actuator整合 内嵌一个tomcat

3.监控
- Metrics 服务接入Prometheus
- 调用链接入Skywalking?待确认

4.责任链设计

5.日志处理

6.管理平台
