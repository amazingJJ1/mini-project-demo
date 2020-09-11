#!/usr/bin/env bash
#部署1个tidb,3个pd,3个tikv
#先启动pd,给全局提供原子钟
#主机名	IP	            部署服务	数据盘挂载
#host1	192.168.1.101	PD1 & TiDB	/data
#host2	192.168.1.102	PD2	        /data
#host3	192.168.1.103	PD3	        /data
#host4	192.168.1.104	TiKV1	    /data
#host5	192.168.1.105	TiKV2	    /data
#host6	192.168.1.106	TiKV3	    /data


#pd1 HOST1
docker run -d --name pd1 \
  -p 2379:2379 \
  -p 2380:2380 \
  -v /etc/localtime:/etc/localtime:ro \
  -v /data:/data \
  pingcap/pd:latest \
  --name="pd1" \
  --data-dir="/data/pd1" \
  --client-urls="http://0.0.0.0:2379" \
  --advertise-client-urls="http://192.168.1.101:2379" \
  --peer-urls="http://0.0.0.0:2380" \
  --advertise-peer-urls="http://192.168.1.101:2380" \
  --initial-cluster="pd1=http://192.168.1.101:2380,pd2=http://192.168.1.102:2380,pd3=http://192.168.1.103:2380"

#pd2 HOST2
docker run -d --name pd2 \
  -p 2379:2379 \
  -p 2380:2380 \
  -v /etc/localtime:/etc/localtime:ro \
  -v /data:/data \
  pingcap/pd:latest \
  --name="pd2" \
  --data-dir="/data/pd2" \
  --client-urls="http://0.0.0.0:2379" \
  --advertise-client-urls="http://192.168.1.102:2379" \
  --peer-urls="http://0.0.0.0:2380" \
  --advertise-peer-urls="http://192.168.1.102:2380" \
  --initial-cluster="pd1=http://192.168.1.101:2380,pd2=http://192.168.1.102:2380,pd3=http://192.168.1.103:2380"

#pd3 HOST3
docker run -d --name pd3 \
  -p 2379:2379 \
  -p 2380:2380 \
  -v /etc/localtime:/etc/localtime:ro \
  -v /data:/data \
  pingcap/pd:latest \
  --name="pd3" \
  --data-dir="/data/pd3" \
  --client-urls="http://0.0.0.0:2379" \
  --advertise-client-urls="http://192.168.1.103:2379" \
  --peer-urls="http://0.0.0.0:2380" \
  --advertise-peer-urls="http://192.168.1.103:2380" \
  --initial-cluster="pd1=http://192.168.1.101:2380,pd2=http://192.168.1.102:2380,pd3=http://192.168.1.103:2380"

#启动 TiKV
#TIKV1 HOST4
docker run -d --name tikv1 \
  -p 20160:20160 \
  --ulimit nofile=1000000:1000000 \
  -v /etc/localtime:/etc/localtime:ro \
  -v /data:/data \
  pingcap/tikv:latest \
  --addr="0.0.0.0:20160" \
  --advertise-addr="192.168.1.104:20160" \
  --data-dir="/data/tikv1" \
  --pd="192.168.1.101:2379,192.168.1.102:2379,192.168.1.103:2379"

#TIKV2 HOST5
docker run -d --name tikv2 \
  -p 20160:20160 \
  --ulimit nofile=1000000:1000000 \
  -v /etc/localtime:/etc/localtime:ro \
  -v /data:/data \
  pingcap/tikv:latest \
  --addr="0.0.0.0:20160" \
  --advertise-addr="192.168.1.105:20160" \
  --data-dir="/data/tikv2" \
  --pd="192.168.1.101:2379,192.168.1.102:2379,192.168.1.103:2379"

#TIKV3 HOST6
docker run -d --name tikv3 \
  -p 20160:20160 \
  --ulimit nofile=1000000:1000000 \
  -v /etc/localtime:/etc/localtime:ro \
  -v /data:/data \
  pingcap/tikv:latest \
  --addr="0.0.0.0:20160" \
  --advertise-addr="192.168.1.106:20160" \
  --data-dir="/data/tikv3" \
  --pd="192.168.1.101:2379,192.168.1.102:2379,192.168.1.103:2379"

#启动 TiDB HOST1
docker run -d --name tidb \
  -p 4000:4000 \
  -p 10080:10080 \
  -v /etc/localtime:/etc/localtime:ro \
  pingcap/tidb:latest \
  --store=tikv \
  --path="192.168.1.101:2379,192.168.1.102:2379,192.168.1.103:2379"

#测试 host1 安装mysql cli
mysql -h 127.0.0.1 -P 4000 -u root -D test
#show databases;

#如何自定义配置文件
#TiKV 和 PD 可以通过指定配置文件的方式来加载更加丰富的启动参数，用于性能调优。
#假定配置文件在宿主机上的存放路径 /path/to/config/pd.toml 和 /path/to/config/tikv.toml。
# 启动 Docker 时需要调整相应的启动参数，以 tikv1 和 pd1 为例：
docker run -d --name tikv1 \
  -p 20160:20160 \
  -v /etc/localtime:/etc/localtime:ro \
  -v /data:/data \
  -v /path/to/config/tikv.toml:/tikv.toml:ro \
  pingcap/tikv:latest \
  --addr="0.0.0.0:20160" \
  --advertise-addr="192.168.1.104:20160" \
  --data-dir="/data/tikv1" \
  --pd="192.168.1.101:2379,192.168.1.102:2379,192.168.1.103:2379" \
  --config="/tikv.toml"

docker run -d --name pd1 \
  -p 2379:2379 \
  -p 2380:2380 \
  -v /etc/localtime:/etc/localtime:ro \
  -v /data:/data \
  -v /path/to/config/pd.toml:/pd.toml:ro \
  pingcap/pd:latest \
  --name="pd1" \
  --data-dir="/data/pd1" \
  --client-urls="http://0.0.0.0:2379" \
  --advertise-client-urls="http://192.168.1.101:2379" \
  --peer-urls="http://0.0.0.0:2380" \
  --advertise-peer-urls="http://192.168.1.101:2380" \
  --initial-cluster="pd1=http://192.168.1.101:2380,pd2=http://192.168.1.102:2380,pd3=http://192.168.1.103:2380" \
  --config="/pd.toml"