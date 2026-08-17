# SimpMC-Punish 网页服务 2.0.0

这是 SimpMC-Punish 的独立网页访问服务。它不再作为 Minecraft 插件内置网页运行，而是单独启动一个 JAR，通过 SimpMC-Punish 的数据库读取处罚记录。

## 构建

```bash
mvn -f simpmc-punish-web/pom.xml package
```

构建产物：

```text
simpmc-punish-web/target/simpmc-punish-web-2.0.0.jar
```

## 启动示例

使用配置文件：

```bash
java -jar simpmc-punish-web-2.0.0.jar --config config.yml
```

直接指定 SQLite 文件或目录：

```bash
java -jar simpmc-punish-web-2.0.0.jar --sqlite "C:\servers\paper\plugins\SimpMC-Punish"
```

使用 MySQL：

```bash
java -jar simpmc-punish-web-2.0.0.jar --mysql --host 0.0.0.0 --port 8080
```

网页默认地址：

```text
http://localhost:8080
```
