package com.dzz.confuse;

/**
 * @author zoufeng
 * @date 2020-8-11
 * java 混淆
 *
 * proguard混淆的流程如下
 * 压缩（shrink）：会检测递归地确定哪些class被使用。所有起的类和方法将会被删除
 * 优化（optimize）：将非入口的方法、类设置为私有、静态或者不可更改的，没有使用的变量删除一些方法会被横线划掉。
 * 混淆（obfuscate）：将那些不是入口点的类、方法重命名。在整个过程中保证入口点确保他们始终能够被原有的名字访问到。
 * 预检（preverify）：对处理后的代码进行预检，确保加载的class文件是可执行的
 *
 *
 * 混淆原理探究：
 * 从现象上看混淆是通过字符串替换的方式
 * 替换字符串的映射关系在生成的文件中有记录
 *
 *
 */
public class Proguard {


}
