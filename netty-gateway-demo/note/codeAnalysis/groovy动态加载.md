groovy动态加载



AppClassLoader是java内置类加载器，用来加载用户应用程序的类。里面有一个parallelLockMap，

主要用来存储类锁，避免JVM加载同名的类，和提高类加载的并发度。

 

GroovyClassLoader如果加载的是无类名的Script，最终会生成一个随机的类名，每次都不一样。

导致parallelLockMap不断膨胀，幸运的是parallelLockMap只存储一个name和Object，并不会占用过多空间。

所以现象就是上线半个多月都没有问题，一个月后才会内存吃紧。8G的LVS甚至几个月都不会有问题。

 

这应该是一个JDK的BUG，只要加载过多的不同类，parallelLockMap就会不断的膨胀，导致memory leak，

最终机器就会宕机。这个这个BUG早已经提给官方，但是JDK7、JDK8都未修复，而且明确指出不修复。

![img](https://static.oschina.net/uploads/space/2017/0211/105828_00bE_2001825.png)

 





# 结论:

理论上是不合理的，因为这个Map只进不出。既然官方指出不修复，那我们只能规范使用流程了。

无论是Groovy还是通过别的方式动态加载类，尽量使用固定类名。如果类名是随机的，就要控制加载数量了。

如果你是4G的LVS，可以放心的创建500万个不同名的类。毛估500万个不同类名，占用大约800M内存。

如果程序仅加载变更后的类，相信500万次变更是肯定够用的。

如果还不够，那只能通过加内存来解决了。

 

使用Groovy动态加载类就算跳过了自带的无法卸载的类的坑，还是会踩进JDK自带的坑。

此次案例详细bug记录请预览<https://issues.apache.org/jira/browse/GROOVY-6655>

 

与此次案例无关的BUG预警：

Groovy推荐版本2.3.7，新版本有无法卸载类的坑，很容易导致OOM:PermGen space。

详细bug记录请预览<https://issues.apache.org/jira/browse/GROOVY-7913>

 

JDK8内存模型更新，默认已经没有PermGen了，也就是说不会发生OOM:PermGen space了。

但是使用的是LVS自带的内存，所以最好还是指定这个参数-XX:MaxMetaspaceSize=128m控制下大小

不然Groovy也会饰无忌惮的吃光LVS内存。

 

Groovy动态加载类使用方式推荐

```groovy
private static final GroovyClassLoader classLoader = new GroovyClassLoader();

public static Script loadScript(String rule) {
    return loadScript(rule, new Binding());

}

public static Script loadScript(String rule, Binding binding) {

    Script script = null;

    if (StringUtils.isEmpty(rule)) {
        return null;
    }

    try {
        Class ruleClazz = classLoader.parseClass(rule);
        if (ruleClazz != null) {
            log.info("load rule:" + rule + " success!");
            return InvokerHelper.createScript(ruleClazz, binding);
        }

    } catch (Exception e) {
        log.error(e.getMessage(), e);
    } finally {
        classLoader.clearCache();
    }
    
    return script;
}
```



因为

GroovyClassLoader是static的，所以想卸载无引用的Class，要执行classLoader.clearCache();

如果GroovyClassLoader每次都是new出来的，可以忽略执行classLoader.clearCache();