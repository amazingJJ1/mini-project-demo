# /etc/sysctl.conf 配置

该文件内容为0，表示禁止数据包转发，1表示允许。
net.ipv4.ip_forward = 0
 
表示开启SYN Cookies。当出现SYN等待队列溢出时，启用cookies来处理，可防范少量SYN攻击，默认为0，表示关闭；
net.ipv4.tcp_syncookies = 1
 
如果该文件指定的值为非0，则激活sysctem request key。默认值：0。
kernel.sysrq = 0
 
默认coredump filename是“核心”。通过设置core_uses_pid为1(默认值为0)，文件名的coredump成为核心PID。如果core_pattern不包括“%p”（默认是不）和core_uses_pid设置。那时pid将附加到文件名上。
kernel.core_uses_pid = 1
 
该文件指定在一个消息队列中最大的字节数 缺省设置：16384。
kernel.msgmnb = 65536
 
该文件指定了从一个进程发送到另一个进程的消息最大长度。进程间的消息传递是在内核的内存中进行的。不会交换到硬盘上。所以如果增加该值，则将增加操作系统所使用的内存数量。
kernel.msgmax = 65536
 
该参数定义了共享内存段的最大尺寸（以字节为单位）。默认是32M。
可取的最大值为物理内存值 -1byte ，建议值为多于物理内存的一半，一般取值大于 SGA_MAX_SIZE 即可，可以取物理内存 -1byte 。  
内存为 12G 时，该值为 12*1024*1024*1024-1 = 12884901887
内存为 16G 时，该值为 16*1024*1024*1024-1 = 17179869183
内存为 32G 时，该值为 32*1024*1024*1024-1 = 34359738367
内存为 64G 时，该值为 64*1024*1024*1024-1 = 68719476735
内存为 128G 时，该值为 128*1024*1024*1024-1 = 137438953471
kernel.shmmax = 17179869183
 
该参数表示统一一次可以使用的共享内存总量（以页为单位）。默认是2097152，通常不需要修改。
Linux 共享内存页大小为 4KB, 共享内存段的大小都是共享内存页大小的整数倍。
一个共享内存段的最大大小是 16G ，那么需要共享内存页数是 16GB/4KB==4194304 （页），
当内存为 12G 时， kernel.shmall = 3145728
当内存为 16G 时， kernel.shmall = 4194304
当内次为 32G 时， kernel.shmall = 8388608
当内存为 64G 时， kernel.shmall = 16777216
当内存为 128G 时， kernel.shmall = 33554432
kernel.shmall = 4194304
 
表示系统同时保持TIME_WAIT套接字的最大数量，如果超过这个数字，TIME_WAIT套接字将立刻被清除并打印警告信息。
默认为180000，改为 5000。对于Apache、Nginx等服务器，参数可以很好地减少TIME_WAIT套接字数量，
但是对于Squid，效果却不大。此项参数可以控制TIME_WAIT套接字的最大数量，避免Squid服务器被大量的TIME_WAIT套接字拖死
net.ipv4.tcp_max_tw_buckets = 6000
 
用来查找特定的遗失的数据包---因此有助于快速恢复状态
net.ipv4.tcp_sack = 1
 
设置tcp/ip会话的滑动窗口大小是否可变。参数值为布尔值，为1时表示可变，为0时表示不可变。tcp/ip通常使用的窗口最大可达到 65535 字节，对于高速网络，该值可能太小，这时候如果启用了该功能，可以使tcp/ip滑动窗口大小增大数个数量级，从而提高数据传输的能力
net.ipv4.tcp_window_scaling = 1
 
TCP读buffer
net.ipv4.tcp_rmem = 4096 87380 4194304
 
TCP写buffer
net.ipv4.tcp_wmem = 4096 16384 4194304
 
默认的发送窗口大小
net.core.wmem_default = 8388608
 
默认的接收窗口大小
net.core.rmem_default = 8388608
 
最大socket接收缓冲
net.core.rmem_max = 16777216
 
最大socket发送缓冲
net.core.wmem_max = 16777216
 
表示当每个网络接口接收数据包的速率比内核处理这些包的速率快时，允许发送到队列的数据包的最大数目。
net.core.netdev_max_backlog = 262144
 
默认值是128， 这个参数用于调节系统同时发起的tcp连接数，在高并发的请求中，默认的值可能会导致链接超时或 # 者重传，因此，需要结合并发请求数来调节此值。
net.core.somaxconn = 65535
 
用于设定系统中最多有多少个TCP套接字不被关联到任何一个用户文件句柄上。如果超过这个数字，孤立连接将立即 # 被复位并打印出警告信息。这个限制只是为了 防止简单的DoS攻击。不能过分依靠这个限制甚至人为减小这个值， # 更多的情况下应该增加这个值。
net.ipv4.tcp_max_orphans = 3276800
 
用于记录那些尚未收到客户端确认信息的连接请求的最大值。对于有128MB内存的系统而言，此参数的默认值是 # 1024，对小内存的系统则是128。
net.ipv4.tcp_max_syn_backlog = 262144
 
开启TCP时间戳
以一种比重发超时更精确的方法（请参阅 RFC 1323）来启用对 RTT 的计算；为了实现更好的性能应该启用这个选项。
net.ipv4.tcp_timestamps = 0
 
参数的值决定了内核放弃连接之前发送SYN+ACK包的数量。
net.ipv4.tcp_synack_retries = 1
 
表示在内核放弃建立连接之前发送SYN包的数量。
net.ipv4.tcp_syn_retries = 1
 
用于设置开启重用，允许将TIME-WAIT sockets重新用于新的TCP连接
net.ipv4.tcp_tw_reuse = 1
 
同样有3个值,意思是:net.ipv4.tcp_mem[0]:低于此值，TCP没有内存压力。 # net.ipv4.tcp_mem[1]:在此值下，进入内存压力阶段。 # net.ipv4.tcp_mem[2]:高于此值，TCP拒绝分配socket。
net.ipv4.tcp_mem = 94500000 915000000 927000000
 
决定了套接字保持在FIN-WAIT-2状态的时间。默认值是60秒。正确设置这个值非常重要，有时即使一个负载很小的 # Web服务器，也会出现大量的死套接字而产生内存溢出的风险。
net.ipv4.tcp_fin_timeout = 1
 
表示当keepalive启用的时候，TCP发送keepalive消息的频度。默认值是2小时（7200）。1200/60 = 20 分钟
net.ipv4.tcp_keepalive_time = 1200
 
用来设定允许系统打开的端口范围。
net.ipv4.ip_local_port_range = 1350 65535
 
禁ping，如果有nagios监控等需要ping保持通讯的此项关闭
net.ipv4.icmp_echo_ignore_all = 1

# 实时socket参数查看 
## /proc/sys/net目录
|参数（路径+文件）|描述|默认值|优化值|
|----|----|----|----|
|/proc/sys/net/core/rmem_default|默认的TCP数据接收窗口大小（字节）。|229376|256960|
|/proc/sys/net/core/rmem_max|最大的TCP数据接收窗口（字节）。|131071|513920|
|/proc/sys/net/core/wmem_default|默认的TCP数据发送窗口大小（字节）。|229376|256960|
|/proc/sys/net/core/wmem_max|最大的TCP数据发送窗口（字节）。|131071|513920|
|/proc/sys/net/core/netdev_max_backlog|在每个网络接口接收数据包的速率比内核处理这些包的速率快时，允许送到队列的数据包的最大数目。|1000|2000|
|/proc/sys/net/core/somaxconn|定义了系统中每一个端口最大的监听队列的长度，这是个全局的参数。|128|2048|
|/proc/sys/net/core/optmem_max|表示每个套接字所允许的最大缓冲区的大小。|20480|81920|
|/proc/sys/net/ipv4/tcp_mem|确定TCP栈应该如何反映内存使用，每个值的单位都是内存页（通常是4KB）。第一个值是内存使用的下限；第二个值是内存压力模式开始对缓冲区使用应用压力的上限；第三个值是内存使用的上限。在这个层次上可以将报文丢弃，从而减少对内存的使用。对于较大的BDP可以增大这些值（注意，其单位是内存页而不是字节）。|94011  125351  188022|131072  262144  524288|
|/proc/sys/net/ipv4/tcp_rmem|为自动调优定义socket使用的内存。第一个值是为socket接收缓冲区分配的最少字节数；第二个值是默认值（该值会被rmem_default覆盖），缓冲区在系统负载不重的情况下可以增长到这个值；第三个值是接收缓冲区空间的最大字节数（该值会被rmem_max覆盖）。|4096  87380  4011232|8760  256960  4088000|
|/proc/sys/net/ipv4/tcp_wmem|为自动调优定义socket使用的内存。第一个值是为socket发送缓冲区分配的最少字节数；第二个值是默认值（该值会被wmem_default覆盖），缓冲区在系统负载不重的情况下可以增长到这个值；第三个值是发送缓冲区空间的最大字节数（该值会被wmem_max覆盖）。|4096  16384  4011232|8760  256960  4088000|
|/proc/sys/net/ipv4/tcp_keepalive_time|TCP发送keepalive探测消息的间隔时间（秒），用于确认TCP连接是否有效。|7200|1800|
|/proc/sys/net/ipv4/tcp_keepalive_intvl|探测消息未获得响应时，重发该消息的间隔时间（秒）。|75|30|
|/proc/sys/net/ipv4/tcp_keepalive_probes|在认定TCP连接失效之前，最多发送多少个keepalive探测消息。|9|3|
|/proc/sys/net/ipv4/tcp_sack|启用有选择的应答（1表示启用），通过有选择地应答乱序接收到的报文来提高性能，让发送者只发送丢失的报文段，（对于广域网通信来说）这个选项应该启用，但是会增加对CPU的占用。|1|1|
|/proc/sys/net/ipv4/tcp_fack|启用转发应答，可以进行有选择应答（SACK）从而减少拥塞情况的发生，这个选项也应该启用。|1|1|
|/proc/sys/net/ipv4/tcp_timestamps|TCP时间戳（会在TCP包头增加12个字节），以一种比重发超时更精确的方法（参考RFC 1323）来启用对RTT 的计算，为实现更好的性能应该启用这个选项。|1|1|
|/proc/sys/net/ipv4/tcp_window_scaling|启用RFC 1323定义的window scaling，要支持超过64KB的TCP窗口，必须启用该值（1表示启用），TCP窗口最大至1GB，TCP连接双方都启用时才生效。|1|1|
|/proc/sys/net/ipv4/tcp_syncookies|表示是否打开TCP同步标签（syncookie），内核必须打开了CONFIG_SYN_COOKIES项进行编译，同步标签可以防止一个套接字在有过多试图连接到达时引起过载。|1|1|
|/proc/sys/net/ipv4/tcp_tw_reuse|表示是否允许将处于TIME-WAIT状态的socket（TIME-WAIT的端口）用于新的TCP连接 。|0|1|
|/proc/sys/net/ipv4/tcp_tw_recycle|能够更快地回收TIME-WAIT套接字。|0|1|
|/proc/sys/net/ipv4/tcp_fin_timeout|对于本端断开的socket连接，TCP保持在FIN-WAIT-2状态的时间（秒）。对方可能会断开连接或一直不结束连接或不可预料的进程死亡。|60|30|
|/proc/sys/net/ipv4/ip_local_port_range|表示TCP/UDP协议允许使用的本地端口号|32768  61000|1024  65000|
|/proc/sys/net/ipv4/tcp_max_syn_backlog|对于还未获得对方确认的连接请求，可保存在队列中的最大数目。如果服务器经常出现过载，可以尝试增加这个数字。|2048|2048|
|/proc/sys/net/ipv4/tcp_low_latency|允许TCP/IP栈适应在高吞吐量情况下低延时的情况，这个选项应该禁用。|0|
|/proc/sys/net/ipv4/tcp_westwood|启用发送者端的拥塞控制算法，它可以维护对吞吐量的评估，并试图对带宽的整体利用情况进行优化，对于WAN 通信来说应该启用这个选项。|0|
|/proc/sys/net/ipv4/tcp_bic|为快速长距离网络启用Binary Increase Congestion，这样可以更好地利用以GB速度进行操作的链接，对于WAN通信应该启用这个选项。|1||


/etc/sysctl.conf文件

/etc/sysctl.conf是一个允许你改变正在运行中的Linux系统的接口。它包含一些TCP/IP堆栈和虚拟内存系统的高级选项，可用来控制Linux网络配置，
由于/proc/sys/net目录内容的临时性，建议把TCPIP参数的修改添加到/etc/sysctl.conf文件, 然后保存文件，使用命令“/sbin/sysctl –p”使之立即生效。具体修改方案参照上文：

net.core.rmem_default = 256960

net.core.rmem_max = 513920

net.core.wmem_default = 256960

net.core.wmem_max = 513920

net.core.netdev_max_backlog = 2000

net.core.somaxconn = 2048

net.core.optmem_max = 81920

net.ipv4.tcp_mem = 131072  262144  524288

-- 发送接收缓冲区
net.ipv4.tcp_rmem = 8760  256960  4088000
net.ipv4.tcp_wmem = 8760  256960  4088000

net.ipv4.tcp_keepalive_time = 1800

net.ipv4.tcp_keepalive_intvl = 30

net.ipv4.tcp_keepalive_probes = 3

net.ipv4.tcp_sack = 1

net.ipv4.tcp_fack = 1

net.ipv4.tcp_timestamps = 1

net.ipv4.tcp_window_scaling = 1

net.ipv4.tcp_syncookies = 1

net.ipv4.tcp_tw_reuse = 1

net.ipv4.tcp_tw_recycle = 1

net.ipv4.tcp_fin_timeout = 30

net.ipv4.ip_local_port_range = 1024  65000

net.ipv4.tcp_max_syn_backlog = 2048

 

Doc2：

   可调优的内核变量存在两种主要接口：sysctl命令和/proc文件系统，proc中与进程无关的所有信息都被移植到sysfs中。IPV4协议栈的sysctl参数主要是sysctl.net.core、sysctl.net.ipv4，对应的/proc文件系统是/proc/sys/net/ipv4和/proc/sys/net/core。只有内核在编译时包含了特定的属性，该参数才会出现在内核中。

    对于内核参数应该谨慎调节，这些参数通常会影响到系统的整体性能。内核在启动时会根据系统的资源情况来初始化特定的变量，这种初始化的调节一般会满足通常的性能需求。

    应用程序通过socket系统调用和远程主机进行通讯，每一个socket都有一个读写缓冲区。读缓冲区保存了远程主机发送过来的数据，如果缓冲区已满，则数据会被丢弃，写缓冲期保存了要发送到远程主机的数据，如果写缓冲区已慢，则系统的应用程序在写入数据时会阻塞。可知，缓冲区是有大小的。

socket缓冲区默认大小：
/proc/sys/net/core/rmem_default     对应net.core.rmem_default
/proc/sys/net/core/wmem_default     对应net.core.wmem_default
    上面是各种类型socket的默认读写缓冲区大小，然而对于特定类型的socket则可以设置独立的值覆盖默认值大小。例如tcp类型的socket就可以用/proc/sys/net/ipv4/tcp_rmem和tcp_wmem来覆盖。

socket缓冲区最大值：
/proc/sys/net/core/rmem_max        对应net.core.rmem_max
/proc/sys/net/core/wmem_max        对应net.core.wmem_max

/proc/sys/net/core/netdev_max_backlog    对应 net.core.netdev_max_backlog
    该参数定义了当接口收到包的速率大于内核处理包的速率时，设备的输入队列中的最大报文数。

/proc/sys/net/core/somaxconn        对应 net.core.somaxconn
    通过listen系统调用可以指定的最大accept队列backlog，当排队的请求连接大于该值时，后续进来的请求连接会被丢弃。

/proc/sys/net/core/optmem_max          对应 net.core.optmem_max
    每个socket的副缓冲区大小。

TCP/IPV4内核参数：
    在创建socket的时候会指定socke协议和地址类型。TCP socket缓冲区大小是他自己控制而不是由core内核缓冲区控制。
/proc/sys/net/ipv4/tcp_rmem     对应net.ipv4.tcp_rmem
/proc/sys/net/ipv4/tcp_wmem     对应net.ipv4.tcp_wmem
    以上是TCP socket的读写缓冲区的设置，每一项里面都有三个值，第一个值是缓冲区最小值，中间值是缓冲区的默认值，最后一个是缓冲区的最大值，虽然缓冲区的值不受core缓冲区的值的限制，但是缓冲区的最大值仍旧受限于core的最大值。

/proc/sys/net/ipv4/tcp_mem  
    该内核参数也是包括三个值，用来定义内存管理的范围，第一个值的意思是当page数低于该值时，TCP并不认为他为内存压力，第二个值是进入内存的压力区域时所达到的页数，第三个值是所有TCP sockets所允许使用的最大page数，超过该值后，会丢弃后续报文。page是以页面为单位的，为系统中socket全局分配的内存容量。