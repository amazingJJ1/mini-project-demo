```bash
docker run -d --name my-oracle \
 -p 8080:8080 -p 1521:1521 \
 -v $PWD/data:/mnt \
 -e TZ=Asia/Shanghai sath89/oracle-12c
 
 #hostname: localhost
 #port: 1521
 #sid: xe
 #username: system
 #password: oracle
 
 docker exec -it my-oracle /bin/bash
 
 #切换到oracle用户
 su oracle
 #使用sysdba登陆 ，sqlplus登录
 $ORACLE_HOME/bin/sqlplus / as sysdba
 #create tablespace 表空间名称 datafile 表空间路劲 size 3000m;
 create tablespace bspdb datafile '/u01/app/oracle/oradata/xe/bspdb.dbf' size 3000m;
 #create user 用户名 identified by 密码 default tablespace 用户默认使用哪一个表空间;
 create user bspdb identified by 123456 default tablespace bspdb;
 #grant 角色1,角色2 to 用户名;
 grant dba, connect to bspdb;
 
 
 #忘记密码处理
 #sqlplus登录，首先解锁用户
 alter user ordsys account unlock; 
 exit
 #重新sqlplus username ,重置密码
```

 查看表空间大小
 ```sql
 select b.name,sum(a.bytes/1000000)总大小 from v$datafile a,v$tablespace b where a.ts#=b.ts# group by b.name;
```
