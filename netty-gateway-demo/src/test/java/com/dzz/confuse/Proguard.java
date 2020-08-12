package com.dzz.confuse;

/**
 * @author zoufeng
 * @date 2020-8-11
 * java 【混淆】
 *
 * proguard混淆的流程如下
 * 压缩（shrink）：会检测递归地确定哪些class被使用。所有起的类和方法将会被删除
 * 优化（optimize）：将非入口的方法、类设置为私有、静态或者不可更改的，没有使用的变量删除一些方法会被横线划掉。
 * 混淆（obfuscate）：将那些不是入口点的类、方法重命名。在整个过程中保证入口点确保他们始终能够被原有的名字访问到。
 * 预检（preverify）：对处理后的代码进行预检，确保加载的class文件是可执行的
 *
 *
 * 代码混淆的原理上述代码混淆的定义里有两个比较重要的点：
 * 别人没办法搞懂我代码在干嘛、机器运行起来懂我代码在干嘛。
 * 所以代码混淆要做的事情就是在不改变输出的情况下把原程序搞得乱七八糟。
 * 一般的混淆方法有：
 * 1、Name obfuscation.（变量名混淆）
 * 2、Code flow obfuscation.（代码流混淆）
 * 3、Incremental obfuscation.（增量混淆）
 * 4、Intermediate code optimization.（中间层代码优化）
 * 5、Debug information obfuscation.（调试信息混淆）
 * 6、Watermarking. （数字水印）
 * 7、Source code obfuscation.（源代码混淆）
 *
 * 作者：彭家进
 * 链接：https://zhuanlan.zhihu.com/p/24818060
 *
 * 说了这么多，那源代码混淆要怎么做呢？
 * 现在比较常见的做法是将代码转换成一棵AST树（Abstract Syntax Tree，见：Abstract syntax tree），
 * 代码中的变量、分隔符等等在树中对应一个个节点，通过对树进行增删改查，实现对代码的混淆。
 * 对树的操作并不难，难的是如何精准地定义要表达的混淆方法，以及如何通过各种各样奇葩的测试用例。
 * 以下就通过一个完整的项目例子，来做一次对JAVA代码的变量名称混淆。
 *
 *
 * 混淆原理探究：
 * 从现象上看混淆是通过字符串替换的方式
 * 替换字符串的映射关系在生成的文件中有记录
 *
 * 混淆的原理主要是解析过程中生成抽象语法树AST
 * 对AST进行操作分析
 *
 *
 */
public class Proguard {


}
