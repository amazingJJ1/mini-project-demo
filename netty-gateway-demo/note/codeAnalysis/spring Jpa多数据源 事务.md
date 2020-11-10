spring Jpa多数据源 事务

最近在做的项目需要操作两个数据源，并且是一个service需要同时调用两个数据源，刚开始按照网上说的配置通过切面操作AbstractRoutingDataSource这个类，发现单独去调用每一个数据源可以灵活切换，后来涉及事务一个service调用两个数据源就发现动态数据源无法切换了，琢磨了很久，终于找到原因。

问题根源：

spring涉及事务的代码调用顺序:

service注解上

1. @transactional
2. TransactionInterceptor.interpter()
3. TransactionAspectSupport.createTransactionIfNecessary()
4. AbstractPlatformTransactionManager.getTransaction()
5. DataSourceTransactionManager.doBegin()
6. AbstractRoutingDataSource.determineTargetDataSource()[lookupKey==null去拿默认的Datasource, 不为空则使用获取到的连接]
7. DataSourceTransactionManager.setTransactional()[将连接设置到TransactionUtils的threadLocal中]
8. Repository@Annotation-->执行一般调用链, 问题在于SpringManagedTransaction.getConnection()
9. openConnection()-->DataSourceUtils.getConnection()
10. TransactionSynchronizationManager.getResource(dataSource)不为空[从TransactionUtils的threadLocal中获取数据源], 所以不会再去调用DynamicDataSource去获取数据源

需要解决问题：在操作完数据库后把threadLocal中的数据源清除！
