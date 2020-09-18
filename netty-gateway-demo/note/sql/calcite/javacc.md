### 前置知识
[编译原理]
[ast](https://blog.csdn.net/weixin_39408343/article/details/95984062)

语法描述文件通常会使用.jj的后缀文件

语法描述文件的常用形式
```text
options {
  JavaCC 的选项
}

PARSER_BEGIN(解析器类名)
package 包名;
import 库名;

public class 解析器类名 {
  任意的Java代码
}
PARSER_END (解析器类名)

扫描器的描述

解析器的描述
```

示例

```text
options {
  STATIC = false;  //解析器可以在多线程环境下使用。设置为true则单线程。
}

PARSER_BEGIN(Person)
import java.io.*;

class Person {
 static public void main (String[] args) {
  for (String arg : args) {
   try {
    System.out.println(evaluate(arg));
   }
   catch (ParseException ex) {
    System.err.println(ex.getMessage);
   }
  }
 }

 static public long evaluate (String src) throws ParseException {
  Reader reader = new StringReader(src);
  return new Person(reader).expr();
 }
}
PARSER_END(Person)

SKIP: { < [" ","\t","\r","\n"] > }

TOKEN: {
 <INTEGER: (["0"-"9"])+>
}

long expr():

{
 Token x,y;
}
{
 x=<INTEGER> "+" y=<INTEGER> <EOF>
 {
   return Long.parseLong(x.image) + Long.parseLong(y.image);
 }
}
```

- PARSER_BEGIN到PARSER_END是解析器类的定义。解析器类中需要定义的成员和方法也写在这。为了实现及时只有Person类也能够运行，这里定义类main函数。
- SKIP和TOKEN部分定义类扫描器。SKIP表示要跳过空格，制表符和换行符。TOKEN表示扫描整数字符并生成TOKEN。
- 从long expr...开始到最后的部分定义了狭义的解析器。这部分解析token序列并执行某些操作

### 词法定义关键字
TOKEN可以定义一些需要识别的关键字，其中以#号开头的token只是在词法分析时使用，
而不能做法语法分析的输入，也就是说。它相对词法分析是局部的，内部的.

```text
TOKEN:/* 定义整数 */
{
  <INTEGER:["1"-"9"](<DIGIT>)*>
}
TOKEN:/* 定义实数 */
{
  <REAL:(<DIGIT>)+
  | (<DIGIT>)+"."
  | (<DIGIT>)+"."(<DIGIT>)+
  | "."(<DIGIT>)+>
}
TOKEN:/*定义数字*/
{
  <#DIGIT:["0"-"9"]>
}
```

### 语法规则实现
第一行是声明返回值和函数名，然后的{TOKEN ...}是声明函数中用到的变量名，最后的{..}中是函数的逻辑实现，
其中第一行是匹配输入的语句，这里匹配的语句是 "a+b+c"的类型的，如果不是这个类型，那么会报错

```text
long expr():
{
    Token x, y, z;
}
{
    x=<INTEGER> "+" y=<INTEGER> "+" z=<INTEGER><EOF>
    {
        return Long.parseLong(x.image) + Long.parseLong(y.image) + Long.parseLong(z.image);
    }
}
```

### 测试

使用JavaCC来处理语法描述文件Person.jj
```bash
javacc Person
```

如果描述文件有问题，处理的时候会报错：

```text
➜  java javacc Person.jj
Java Compiler Compiler Version 5.0 (Parser Generator)
(type "javacc" with no arguments for help)
Reading from file Person.jj . . .
org.javacc.parser.ParseException: Encountered " ";" "; "" at line 33, column 12.
Was expecting one of:
    "throws" ...
    ":" ...
```

处理成功后类似：

```text
Java Compiler Compiler Version 5.0 (Parser Generator)
(type "javacc" with no arguments for help)
Reading from file Person.jj . . .
File "TokenMgrError.java" does not exist.  Will create one.
File "ParseException.java" does not exist.  Will create one.
File "Token.java" does not exist.  Will create one.
File "SimpleCharStream.java" does not exist.  Will create one.
Parser generated successfully.
```
➜  java javacc Person.jj

除了生成Person.java文件以外，还会生成其他的辅助类,如ParseException等

编译Person.java文件
```bash
javac Person.java
```

执行java文件
```bash
java java Person '111 + 222'
333
```

计算结果没毛病！

所以我们编写的语法规则，现在已经可以正常的计算了。
