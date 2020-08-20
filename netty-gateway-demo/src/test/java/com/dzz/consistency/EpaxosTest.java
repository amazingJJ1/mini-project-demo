package com.dzz.consistency;

/**
 * @author zoufeng
 * @date 2020-8-20
 *
 * 相比
 * EPaxos是一个Leaderless的一致性算法，任意副本均可提交日志，通常情况下，一次日志提交需要一次或两次网络来回。
 *
 * EPaxos无Leader选举开销，一个副本不可用可立即访问其他副本，具有更高的可用性。
 * 各副本负载均衡，无Leader瓶颈，具有更高的吞吐量。
 * 客户端可选择最近的副本提供服务，在跨AZ跨地域场景下具有更小的延迟
 */
public class EpaxosTest {
}
