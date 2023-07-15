# databuilder
* 用来大批量生成mysql测试数据（支持自定义日期范围、整型、字符串枚举），输出结果为sql脚本

# 准备模板
- 创建一个空文件夹如  D:\sql\
- 在sql文件夹下创建一个txt文档名称随便命名，文件内容如下 （D:\sql\ 文件夹下可创建多个txt文档，支持一次批量生成多个建表语句）
```
##建表语句##
CREATE TABLE `tb_student` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `name` varchar(64) NOT NULL COMMENT '姓名',
  `age` int(11) DEFAULT NULL COMMENT '年龄',
  `create_by` varchar(255) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统路由日志';
##数据量##
100
##基础数据##
`id`
`name` 刘德华 张学友 郭富城 黎明
`age` 18 19 20 21 22 23 24 25 26 27 28 29 30
`create_time` 2020-01-01 23:59:59~2021-06-01 23:59:59 2023-01-15 23:59:59~2023-12-15 23:59:59
```

# 打包&生成sql
```
mvn clean install
```
- 在target目录执行java -jar mysql-datafaker.jar
- 命令行会提示输入工作路径，输入上面准备的工作路径：D:\sql\
- 回车，sql就会生成
- 到D:\sql\工作目录下，会发现新生成的一个文件夹下面是生成的sql
- 在对应的mysql工具下执行该sql
