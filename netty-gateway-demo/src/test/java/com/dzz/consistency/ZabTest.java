package com.dzz.consistency;

/**
 * @author zoufeng
 * @date 2020-8-20
 *
 * ZAB也是对Multi Paxos算法的改进，大部分和raft相同
 * 和raft算法的主要区别：
 * 1、对于Leader的任期，raft叫做term，而ZAB叫做epoch
 * 2、在状态复制的过程中，raft的心跳从Leader向Follower发送，而ZAB则相反。
 */
public class ZabTest {
}
