# SimpBan 网页服务 1.0

这是 SimpBan 的独立网页访问服务。它不再作为 Minecraft 插件内置网页运行，而是单独启动一个 JAR，通过 SimpBan 的数据库读取处罚记录。

## 构建

```bash
mvn -f simpban-web/pom.xml package
```

构建产物：

```text
simpban-web/target/simpban-web-1.0.jar
```

## 启动示例

使用配置文件：

```bash
java -jar simpban-web-1.0.jar --config config.yml
```

直接指定 SQLite 文件或目录：

```bash
java -jar simpban-web-1.0.jar --sqlite "C:\servers\paper\plugins\SimpBan"
```

使用 MySQL：

```bash
java -jar simpban-web-1.0.jar --mysql --host 0.0.0.0 --port 8080
```

网页默认地址：

```text
http://localhost:8080
```
