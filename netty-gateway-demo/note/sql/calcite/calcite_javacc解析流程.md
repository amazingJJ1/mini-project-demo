## calcite sql解析demo

```java
public class JavaccDemoTest {

    @Test
    public void test(){
        SchemaPlus rootSchema = Frameworks.createRootSchema(true);
        //setCaseSensitive() 大小是写否敏感，比如说列名、表名、函数名
        //setQuoting() 设置引用一个标识符，比如说MySQL中的是``, Oracle中的""
        //setQuotedCasing Quoting策略，不变，变大写或变成小写，代码中的全部设置成变大写
        //setUnquotedCasing 当标识符没有被Quoting后的策略，值同上
        //更多可以更以参考Calcite类Lex, 你也可以直接设置成MySQL、Oracle、MySQL_ANSI语法，
        // 如果需要定制化的话可以单独设置上面4个参数
        final FrameworkConfig config = Frameworks.newConfigBuilder()
                .parserConfig(SqlParser.configBuilder()
                        .setParserFactory(SqlParserImpl.FACTORY)
                        .setCaseSensitive(false)
                        .setQuoting(Quoting.BACK_TICK)
                        .setQuotedCasing(Casing.TO_UPPER)
                        .setUnquotedCasing(Casing.TO_UPPER)
                        .setConformance(SqlConformanceEnum.ORACLE_12)
                        .build())
                .build();

        String sql = "select ids, name from test where id < 5 and name = 'zhang'";
        SqlParser parser = SqlParser.create(sql, config.getParserConfig());
        try {
            SqlNode sqlNode = parser.parseStmt();
            System.out.println(sqlNode.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```
1. javacc 根据Parser.jj的语法文件和config.fmpp的变量生成org.apache.calcite.sql.parser.impl包下的类。
    其中生成最重要的类是**org.apache.calcite.sql.parser.impl.SqlParserImpl**，负责将传入的sql语句
    解析成sqlNode。而**sqlNode本质是一颗sql解析树**。
    
    >注意: Parser只会解析SQL, 不会去验证SQL是否正确，可能这么说有点矛盾，有人会想parser难道不会检查语法正确与否吗？
    我的回答是、也不是。上面的例子如果有人执行了之后发现居然可以通过，
    而在代码中我们并没有明确表名、列名、列信息之类，为什么不会报错？
    因为 Calcite parser 只会识别关键字(Keyword)与标识符(Identifier)， 
    上面Sql关键字有select、from、where、<、=，其他为标识符，即Parsr会规定关键字与标识符的相对位置是否正确，
    不会关心标识符的值是否存在、是否正确， 至于什么时候会检查标识符--会在Validator阶段
    
2. 根据sqlNode的类型，是DDL还是DML。这里先分析DML，比如select语句。
    接着将sqlNode转换为RelRoot。转化的过程是：首先是org.apache.calcite.sql.validate.SqlValidator去访问SqlNode,
    然后sqlToRelConverter.convertQuery(sqlQuery, needsValidation, true)将sqlNode转化relNode,
    生成relNode是一个递归的过程。
    > RelNode result = convertQueryRecursive(query, top, null).rel;
    
    调用过程可以在org.apache.calcite.prepare.CalcitePrepareImpl#prepare2_里面查看
     
3. RelRoot其实就是语义分析后的logical plan(逻辑计划)