package com.dzz.consistency;

/**
 * @author zoufeng
 * @date 2020-8-20
 *
 *
 * 开源paxos实现
 * 58同城 WPaxos 2020-4
 *
 * 高性能：Multi-Paxos算法与Basic-Paxos算法结合，支持多Paxos分组，有序确定多个值
 *
 * 节点间可通过状态机checkpoint或逐条数据流两种方式对落后数据快速对齐
 *
 * 具有网络分区容错性，集群少数节点故障服务高可用性
 *
 * 提供有Master自动选举功能
 *
 * 集群可通过Paxos协议动态、安全的增加节点、删除节点
 *
 * 高扩展性：支持存储模块与异步通信模块自定义
 *
 * 一个Paxos实例可以同时挂载多个状态机
 *
 * 提交的数据支持增量checksum校验
 *
 * 可添加不参与提案投票，仅用于备份数据的follower节点
 *
 * 默认存储支持按照时间与holdcount两种清理paxoslog方式
 */
public class PaxosTest {
}
