# SimpBan 1.0

SimpBan 是面向 Minecraft 服务器的处罚管理插件，作者为 GPT5.5、Minecraft0122、SimpMC。

本仓库包含两个产物：

- Minecraft 插件本体：只负责封禁、禁言、踢出、历史记录、GUI、数据库和 Discord Webhook。
- 独立网页访问服务：位于 `simpban-web/`，单独运行，不再嵌入插件 JAR。

## 构建插件

```bash
mvn package
```

插件 JAR 输出位置：

```text
target/simpban-1.0.jar
```

## 构建独立网页服务

```bash
mvn -f simpban-web/pom.xml package
```

网页服务 JAR 输出位置：

```text
simpban-web/target/simpban-web-1.0.jar
```

## 网页服务启动示例

指定 SQLite 文件或目录：

```bash
java -jar simpban-web-1.0.jar --sqlite "C:\servers\paper\plugins\SimpBan"
```

使用 MySQL：

```bash
java -jar simpban-web-1.0.jar --mysql --mysql-host localhost --mysql-database simpban --mysql-user root --mysql-password password
```

使用配置文件：

```bash
java -jar simpban-web-1.0.jar --config config.yml
```

示例配置见 `simpban-web/config.example.yml`。
