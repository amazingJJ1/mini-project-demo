### 前置学习
[使用calcite实现一个简单数据库](https://www.ctolib.com/topics-135940.html)

本文[来自](https://matt33.com/2019/03/07/apache-calcite-process-flow/#SQL-Parser-%E6%B5%81%E7%A8%8B)

其他[calcite教程](https://blog.csdn.net/QXC1281/article/details/89070285)

## 关系代数的基本知识
关系代数是关系型数据库操作的理论基础，关系代数支持并、差、笛卡尔积、投影和选择等基本运算。
关系代数也是 Calcite 的核心，任何一个查询都可以表示成由关系运算符组成的树。
在 Calcite 中，它会先将 SQL 转换成关系表达式（relational expression），
然后通过规则匹配（rules match）进行相应的优化，优化会有一个成本（cost）模型为参考。

这里先看下关系代数相关内容，这对于理解 Calcite 很有帮助，特别是 Calcite Optimizer 这块的内容，
关系代数的基础可以参考这篇文章 SQL 形式化语言——关系代数，简单总结如下：

|名称|英文|符号|说明|
|:----:|:----:|:----:|:----:|
|选择|select|σ|类似于 SQL 中的 where|
|投影|project|Π|类似于 SQL 中的 select|
|并|union|∪|类似于 SQL 中的 union|
|集合差|set-difference|-|SQL中没有对应的操作符|
|笛卡儿积|Cartesian-product|×|类似于 SQL 中不带 on 条件的 inner join|
|重命名|rename|ρ|类似于 SQL 中的 as|
|集合交|intersection|∩|SQL中没有对应的操作符|
|自然连接|natural join|⋈|类似于 SQL 中的 inner join|
|赋值|assignment|||

### 查询优化
查询优化主要是围绕着 等价交换 的原则做相应的转换，这部分可以参考【《数据库系统概念（中文第六版）》第13章——查询优化】，
关于查询优化理论知识，这里就不再详述，列出一些个人不错不错的博客，大家可以参考一下：
- [数据库查询优化入门: 代数与物理优化基础](https://www.jianshu.com/p/edf503a2a1e7)；
- [高级数据库十五：查询优化器（一）](https://blog.csdn.net/u013007900/article/details/78978271)；
- [高级数据库十六：查询优化器（二）](https://blog.csdn.net/u013007900/article/details/78993101)；
- [「 数据库原理 」查询优化（关系代数表达式优化）](http://www.ptbird.cn/optimization-of-relational-algebraic-expression.html)；
- [4.1.3 关系数据库系统的查询优化（1）](http://book.51cto.com/art/201306/400084.htm)；
- [4.1.3 关系数据库系统的查询优化（10）](http://book.51cto.com/art/201306/400085.htm)；

### calcite基本概念
|类型|描述|特点|
|:----|:----|:----|
|RelOptRule|transforms an expression into another。<br>对 expression 做等价转换|根据传递给它的 RelOptRuleOperand <br>来对目标 RelNode 树进行规则匹配，匹配成功后，<br>会再次调用 matches() 方法（默认返回真）进行进一步检查。<br>如果 mathes() 结果为真，则调用 onMatch() 进行转换。|
|ConverterRule|Abstract base class for a rule which converts <br>from one calling convention to another without<br> changing semantics.|它是RelOptRule 的子类，专门用来做数据源之间的转换<br>（Calling convention），ConverterRule <br>一般会调用对应的 Converter 来完成工作，比如说：<br>JdbcToSparkConverterRule 调用 JdbcToSparkConverter<br> 来完成对 JDBC Table 到 Spark RDD 的转换。|
|RelNode|relational expression，RelNode 会标识其<br> input RelNode 信息，这样就构成了一棵 RelNode 树|代表了对数据的一个处理操作，常见的操作有 Sort、Join、Project、Filter、Scan 等。它蕴含的是对整个 Relation 的操作，而不是对具体数据的处理逻辑。|
Converter|A relational expression implements the<br> interface Converter to indicate that it converts<br> a physical attribute, or RelTrait of a relational <br>expression from one value to another.|用来把一种 RelTrait 转换为另一种 RelTrait 的 RelNode。<br>如 JdbcToSparkConverter 可以把 JDBC 里的 table 转换为 Spark RDD<br>。如果需要在一个 RelNode 中处理来源于异构系统的逻辑表，<br>Calcite 要求先用 Converter 把异构系统的逻辑表转换为同一种 Convention。
RexNode|Row-level expression|行表达式（标量表达式）<br>，蕴含的是对一行数据的处理逻辑。每个行表达式都有数据的类型。<br>这是因为在 Valdiation 的过程中，编译器会推导出<br>表达式的结果类型。常见的行表达式包括字面量 RexLiteral，<br> 变量 RexVariable， 函数或操作符调用 RexCall 等。<br> RexNode 通过 RexBuilder 进行构建。
RelTrait|RelTrait represents the manifestation of a <br>relational expression trait within a trait definition.|用来定义逻辑表的物理相关属性（physical property），<br>三种主要的 trait 类型是：Convention、RelCollation、RelDistribution；
Convention|Calling convention used to repressent a <br>single data source, inputs must be in the same convention|继承自 RelTrait，类型很少，代表一个单一的数据源，一个<br> relational expression 必须在同一个 convention 中；
RelTraitDef||主要有三种：ConventionTraitDef：用来代表数据源<br> RelCollationTraitDef：用来定义参与排序的字段；<br>RelDistributionTraitDef：用来定义数据在物理存储上的分布方式<br>（比如：single、hash、range、random 等）；
RelOptCluster|An environment for related relational<br> expressions during the optimization of a query.|palnner 运行时的环境，保存上下文信息；
RelOptPlanner|A RelOptPlanner is a query optimizer: <br>it transforms a relational expression <br>into a semantically equivalent relational expression, <br>according to a given set of rules and a cost model.|也就是优化器，Calcite 支持RBO（Rule-Based Optimizer） <br>和 CBO（Cost-Based Optimizer）。Calcite 的 <br>RBO （HepPlanner）称为启发式优化器（heuristic implementation ），<br>它简单地按 AST 树结构匹配所有已知规则，直到没有规则能够匹配为止；<br>Calcite 的 CBO 称为火山式优化器（VolcanoPlanner）<br>成本优化器也会匹配并应用规则，<br>当整棵树的成本降低趋于稳定后，优化完成，<br>成本优化器依赖于比较准确的成本估算。<br>RelOptCost 和 Statistic 与成本估算相关；
RelOptCost|defines an interface for optimizer cost in <br>terms of number of rows processed, CPU cost, and I/O cost.|优化器成本模型会依赖；

### calcite 架构
关于 Calcite 的架构，可以参考下图（图片来自前面那篇论文），它与传统数据库管理系统有一些相似之处，
相比而言，它将数据存储、数据处理算法和元数据存储这些部分忽略掉了，这样设计带来的好处是：
对于涉及多种数据源和多种计算引擎的应用而言，Calcite 因为可以兼容多种存储和计算引擎，
使得 Calcite 可以提供统一查询服务，Calcite 将会是这些应用的最佳选择。

![](image/calcite_arc.png)

在 Calcite 架构中，最核心地方就是 Optimizer，也就是优化器，一个 Optimization Engine 包含三个组成部分：

- rules：也就是匹配规则，Calcite 内置上百种 Rules 来优化 relational expression，当然也支持自定义 rules；
- metadata providers：主要是向优化器提供信息，这些信息会有助于指导优化器向着目标
    （减少整体 cost）进行优化，信息可以包括行数、table 哪一列是唯一列等，
    也包括计算 RelNode 树中执行 subexpression cost 的函数；
- planner engines：它的主要目标是进行触发 rules 来达到指定目标，
    比如像 cost-based optimizer（CBO）的目标是减少cost（Cost 包括处理的数据行数、CPU cost、IO cost 等）。
    
### calcite 执行流程
Sql 的执行过程一般可以分为下图中的四个阶段，Calcite 同样也是这样：

![](image/calcite_dataflow.png)

但这里为了讲述方便，把 SQL 的执行分为下面五个阶段（跟上面比比又独立出了一个阶段）：

1. 解析 SQL， 把 SQL 转换成为 AST （抽象语法树），在 Calcite 中用 SqlNode 来表示；
2. 语法检查，根据数据库的元数据信息进行语法验证，验证之后还是用 SqlNode 表示 AST 语法树；
3. 语义分析，根据 SqlNode 及元信息构建 RelNode 树，也就是最初版本的逻辑计划（Logical Plan）；
4. 逻辑计划优化，优化器的核心，根据前面生成的逻辑计划按照相应的规则（Rule）进行优化；
5. 物理执行，生成物理计划，物理执行计划执行。

这里我们只关注前四步的内容，会配合源码实现以及一个示例来讲解。

#### step1: sql解析阶段 （SQL–>SqlNode）
使用 Calcite 进行 Sql 解析的代码如下：
```java
SqlParser parser = SqlParser.create(sql, SqlParser.Config.DEFAULT);
SqlNode sqlNode = parser.parseStmt();
```

Calcite 使用 JavaCC 做 SQL 解析，JavaCC 根据 Calcite 中定义的 Parser.jj 文件，生成一系列的 java 代码，
生成的 Java 代码会把 SQL 转换成 AST 的数据结构（这里是 SqlNode 类型）。

>与 Javacc 相似的工具还有 ANTLR，JavaCC 中的 jj 文件也跟 ANTLR 中的 G4文件类似，Apache Spark 中使用这个工具做类似的事情。

**javacc**
关于 Javacc 内容可以参考下面这几篇文章，这里就不再详细展开，
可以通过下面文章的例子把 JavaCC 的语法了解一下，这样我们也可以自己设计一个 DSL（Doomain Specific Language）。

- [JavaCC 研究与应用( 8000字 心得 源程序)](https://www.cnblogs.com/Gavin_Liu/archive/2009/03/07/1405029.html)；
- [JavaCC、解析树和 XQuery 语法，第 1 部分](https://www.ibm.com/developerworks/cn/xml/x-javacc/part1/index.html)；
- [JavaCC、解析树和 XQuery 语法，第 2 部分](https://www.ibm.com/developerworks/cn/xml/x-javacc/part2/index.html)；
- [编译原理之Javacc使用](https://www.yangguo.info/2014/12/13/%E7%BC%96%E8%AF%91%E5%8E%9F%E7%90%86-Javacc%E4%BD%BF%E7%94%A8/)；
- [javacc tutorial](http://www.engr.mun.ca/~theo/JavaCC-Tutorial/javacc-tutorial.pdf)；

回到 Calcite，Javacc 这里要实现一个 SQL Parser，它的功能有以下两个，这里都是需要在 jj 文件中定义的。

1. 设计词法和语义，定义 SQL 中具体的元素；
2. 实现词法分析器（Lexer）和语法分析器（Parser），完成对 SQL 的解析，完成相应的转换。

**SQL Parser 流程**
其中 SqlParser 中 parser 指的是 SqlParserImpl 类（SqlParser.Config.DEFAULT 指定的），
它就是由 JJ 文件生成的解析类，其处理流程如下，具体解析逻辑还是要看 JJ 文件中的定义。

#### step2、语法检查:SqlNode 验证（SqlNode–>SqlNode）
经过上面的第一步，会生成一个 SqlNode 对象，它是一个未经验证的抽象语法树，下面就进入了一个语法检查阶段，
**语法检查前需要知道元数据信息*，这个检查会包括表名、字段名、函数名、数据类型的检查。进行语法检查的实现如下：

这个过程主要是表对象的初始化

```java
//note: 二、sql validate（会先通过Catalog读取获取相应的metadata和namespace）
//note: get metadata and namespace
SqlTypeFactoryImpl factory = new SqlTypeFactoryImpl(RelDataTypeSystem.DEFAULT);
CalciteCatalogReader calciteCatalogReader = new CalciteCatalogReader(
    CalciteSchema.from(rootScheme),
    CalciteSchema.from(rootScheme).path(null),
    factory,
    new CalciteConnectionConfigImpl(new Properties()));

//note: 校验（包括对表名，字段名，函数名，字段类型的校验。）
SqlValidator validator = SqlValidatorUtil.newValidator(SqlStdOperatorTable.instance(), calciteCatalogReader, factory,
    conformance(frameworkConfig));
SqlNode validateSqlNode = validator.validate(sqlNode);

```

我们知道 Calcite 本身是不管理和存储元数据的，在检查之前，需要先把元信息注册到 Calcite 中，
一般的操作方法是实现 SchemaFactory，由它去创建相应的 Schema，在 Schema 中可以注册相应的元数据信息
（如：通过 getTableMap() 方法注册表信息），如下所示：

```java
protected Map<String, Table> getTableMap() {
  return ImmutableMap.of();
}

//org.apache.calcite.adapter.csvorg.apache.calcite.adapter.csv.CsvSchemasvSchema
//note: 创建表
@Override protected Map<String, Table> getTableMap() {
  if (tableMap == null) {
    tableMap = createTableMap();
  }
  return tableMap;
}
```

结合前面的例子再来分析，在前面定义了 CalciteCatalogReader 实例，该实例就是用来读取 Schema 中的元数据信息的。
真正检查的逻辑是在 SqlValidatorImpl 类中实现的，这个 check 的逻辑比较复杂，在看代码时通过两种手段来看：

- DEBUG 的方式，可以看到其方法调用的过程；
- 测试程序中故意构造一些 Case，观察其异常栈。

**SqlValidatorImpl 检查过程**
语法检查验证是通过 SqlValidatorImpl 的 validate() 方法进行操作的.它的处理逻辑主要分为三步：

1. rewrite expression，将其标准化，便于后面的逻辑计划优化；
2. 注册这个 relational expression 的 scopes 和 namespaces（这两个对象代表了其元信息）；
3. 进行相应的验证，这里会依赖第二步注册的 scopes 和 namespaces 信息。

**registerQuery**
这里的功能主要就是将[元数据]转换成 SqlValidator 内部的 对象 进行表示，
也就是 SqlValidatorScope 和 SqlValidatorNamespace 两种类型的对象：

- SqlValidatorNamespace：a description of a data source used in a query，
    它代表了 SQL 查询的数据源，它是一个逻辑上数据源，可以是一张表，也可以是一个子查询；
- SqlValidatorScope：describes the tables and columns accessible at a particular point in the query，
    代表了在某一个程序运行点，当前可见的字段名和表名。
    
这个理解起来并不是那么容易，在 SelectScope 类中有一个示例讲述，这个示例对这两个概念的理解很有帮助。

```java
/**
 * <h3>Scopes</h3>
 *
 * <p>In the query</p>
 *
 * <blockquote>
 * <pre>
 * SELECT expr1
 * FROM t1,
 *     t2,
 *     (SELECT expr2 FROM t3) AS q3
 * WHERE c1 IN (SELECT expr3 FROM t4)
 * ORDER BY expr4</pre>
 * </blockquote>
 *
 * <p>The scopes available at various points of the query are as follows:</p>
 *
 * <ul>
 * <li>expr1 can see t1, t2, q3</li>
 * <li>expr2 can see t3</li>
 * <li>expr3 can see t4, t1, t2</li>
 * <li>expr4 can see t1, t2, q3, plus (depending upon the dialect) any aliases
 * defined in the SELECT clause</li>
 * </ul>
 *
 * <h3>Namespaces</h3>
 *
 * <p>In the above query, there are 4 namespaces:</p>
 *
 * <ul>
 * <li>t1</li>
 * <li>t2</li>
 * <li>(SELECT expr2 FROM t3) AS q3</li>
 * <li>(SELECT expr3 FROM t4)</li>
 */
```

#### step3、语义分析 SqlNode–>RelNode/RexNode
经过第二步之后，这里的 SqlNode 就是经过语法校验的 SqlNode 树，接下来这一步就是将 SqlNode 转换成 RelNode/RexNode，
也就是**生成相应的逻辑计划（Logical Plan）**

过程如下：
初始化 RexBuilder；
初始化 RelOptPlanner;
初始化 RelOptCluster；
初始化 SqlToRelConverter；
进行转换；

SqlToRelConverter 中的 convertQuery() 将 SqlNode 转换为 RelRoot

在 convertSelectImpl() 方法中会依次对 SqlSelect 的各个部分做相应转换

这部分方法调用过程是：
```text
convertQuery -->
convertQueryRecursive -->
convertSelect -->
convertSelectImpl -->
convertFrom & convertWhere & convertSelectList
```

#### step4、优化 RelNode–>RelNode

终于来来到了第四阶段，也就是 Calcite 的核心所在，优化器进行优化的地方，
前面 sql 中有一个明显可以优化的地方就是过滤条件的下压（push down），在进行 join 操作前，
先进行 filter 操作，这样的话就不需要在 join 时进行全量 join，减少参与 join 的数据量。

关于filter 操作下压，在 Calcite 中已经有相应的 Rule 实现，就是 FilterJoinRule.FilterIntoJoinRule.FILTER_ON_JOIN，
这里使用 HepPlanner 作为示例的 planer，并注册 FilterIntoJoinRule 规则进行相应的优化，其代码实现如下：

```text
HepProgramBuilder builder = new HepProgramBuilder();
builder.addRuleInstance(FilterJoinRule.FilterIntoJoinRule.FILTER_ON_JOIN); //note: 添加 rule
HepPlanner hepPlanner = new HepPlanner(builder.build());
hepPlanner.setRoot(relNode);
relNode = hepPlanner.findBestExp();
```
在 Calcite 中，提供了两种 planner：HepPlanner 和 VolcanoPlanner，关于这块内容可以参考【Drill/Calcite查询优化系列】
这几篇文章（讲述得非常详细，赞），这里先简单介绍一下 HepPlanner 和 VolcanoPlanner，后面会关于这两个 planner 的代码实现做深入的讲述。

**HepPlanner**

特点（来自 Apache Calcite介绍）：
1. HepPlanner is a heuristic optimizer similar to Spark’s optimizer，与 spark 的优化器相似，HepPlanner 是一个 heuristic 优化器；
2. Applies all matching rules until none can be applied：将会匹配所有的 rules 直到一个 rule 被满足；
3. Heuristic optimization is faster than cost- based optimization：它比 CBO 更快；
4. Risk of infinite recursion if rules make opposing changes to the plan：如果没有每次都不匹配规则，可能会有无限递归风险；

**VolcanoPlanner**
特点（来自 Apache Calcite介绍）：

1. VolcanoPlanner is a cost-based optimizer：VolcanoPlanner是一个CBO优化器；
2. Applies matching rules iteratively, selecting the plan with the cheapest cost on each iteration：
    迭代地应用 rules，直到找到cost最小的plan；
3. Costs are provided by relational expressions；
4. Not all possible plans can be computed：不会计算所有可能的计划；
5. Stops optimization when the cost does not significantly improve through 
    a determinable number of iterations：根据已知的情况，如果下面的迭代不能带来提升时，这些计划将会停止优化；

### step5、生成物理计划 javaplan
这小结[参考](https://blog.csdn.net/feeling890712/article/details/106378175)

通过优化获得了Jdbc的物理表达式后，要将这些表达式生成Java代码后才能真正将执行计划跑起来。
在生成Java代码之前，将Jdbc的物理表达式的根节点变为EnumerableCalc，JdbcToEnumerableConverter变为它的子节点

Calcaite如何通过这些物理关系表达式算子生成可执行的Java代码？
在EnumerableRel的每个算子的implement方法中会将这个算子要实现的算法写成Linq4j的表达式，然后通过这些Linq4j表达式生成Java Class。

查看EnumerableInterpretable的toBindable方法的片段，jdbc流程中在获得上一小节讲解的物理表达式树后，
经过上面的代码生成一个EnumerableRelImplementor对象，通过调用它的implementRoot方法会调用物理表达式树根节点
(这里是EnumerableCalc)的implement方法，父节点又会迭代调用子节点的implement方法。
最终返会获得生成的linq4j表达式树。通过上面代码片段的Expressions.toString方法可以生成java代码字符串。

```java
  public static Bindable toBindable(Map<String, Object> parameters,
      CalcitePrepare.SparkHandler spark, EnumerableRel rel,
      EnumerableRel.Prefer prefer) {
    EnumerableRelImplementor relImplementor =
        new EnumerableRelImplementor(rel.getCluster().getRexBuilder(),
            parameters);

    final ClassDeclaration expr = relImplementor.implementRoot(rel, prefer);
    String s = Expressions.toString(expr.memberDeclarations, "\n", false);

    if (CalciteSystemProperty.DEBUG.value()) {
      Util.debugCode(System.out, s);
    }

    Hook.JAVA_PLAN.run(s);

    try {
      if (spark != null && spark.enabled()) {
        return spark.compile(expr, s);
      } else {
        return getBindable(expr, s, rel.getRowType().getFieldCount());
      }
    } catch (Exception e) {
      throw Helper.INSTANCE.wrap("Error while compiling generated Java code:\n"
          + s, e);
    }
  }
```
上面的代码是EnumerableInterpretable的toBindable方法的片段，jdbc流程中在获得上一小节讲解的物理表达式树后，
经过上面的代码生成一个EnumerableRelImplementor对象，通过调用它的implementRoot方法会调用物理表达式树根节点
(这里是EnumerableCalc)的implement方法，父节点又会迭代调用子节点的implement方法。最终返会获得生成的linq4j表达式树。
通过上面代码片段的**Expressions.toString方法可以生成java代码字符串**

得到生成的java代码后，Calcite会调用**Janino编译器动态编译这个java类**，并且实例化这个类的一个对象。
后面在创建CalciteResultSet的时候会调用这个对象的bind方法,这个方法返回一个Eumerable对象，
通过这个对象可以迭代JDBC查询的结果。这个Eumerable对象的实际工作是委托给ResultSetEnumerable的enumerator()方法生成的枚举器实现的。

最后通过上面ResultSetEnumerable的enumerator()方法，在生成枚举器的时候就开始执行真正的数据库查询，获得实际的ResultSet，用ResultSetEnumerator包装起来，通过ResultSetEnumerator就能操作ResultSet。
  在executeSql方法的最后，会返回CalciteResultSet
  
简单的总计java plan：

> 就是每个算子（比如filter,project,scan）先转化程linq4j表达式，然后linq4j将表达式转换为java 的function代码，
接着janino将这些代码编译并且加载生成执行对象。最后resultSet执行的时候将整个物理计划串起来，得到最后的结果

后续文章[Apache Calcite 优化器详解（二）](https://matt33.com/2019/03/17/apache-calcite-planner/)

更高级一点的技巧，使用Filterable Table实现谓词下推
更酷一点的技巧，基于TranslateTable使用Rule实现逻辑表达式的转换

### RelOptUtil.toString(RelNode) 查看逻辑计划
我们只是在调用build方法之前添加一个调用project方法：
```text
final FrameworkConfig config;
final RelBuilder builder = RelBuilder.create(config);
final RelNode node = builder
  .scan("EMP")
  .project(builder.field("DEPTNO"), builder.field("ENAME"))
  .build();
System.out.println(RelOptUtil.toString(node));
```
输出是
```text
LogicalProject(DEPTNO=[$7], ENAME=[$1])
    LogicalTableScan(table=[[scott, EMP]])
```
等同于
```text
SELECT ename, deptno FROM scott.EMP;
```

查看[文档](https://blog.csdn.net/QXC1281/article/details/89060551)
查看[文档2](https://blog.csdn.net/QXC1281/article/details/89187221)