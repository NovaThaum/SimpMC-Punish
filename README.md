# SimpMC-Punish

SimpMC-Punish 是面向 Minecraft 服务器的处罚管理插件，作者为 GPT5.5、Minecraft0122、SimpMC。

本仓库包含两个运行组件：

- Paper/Folia 插件：负责处罚写入、管理命令、历史记录、菜单界面、数据库和 Discord 通知。
- Velocity 插件：只读共享 MySQL，负责全网封禁同步、后端 kick 处理和内嵌管理网页。

## 查看封禁列表

拥有 `simpmc-punish.banlist` 权限的管理员可以查看当前仍在生效的玩家及 IP 封禁：

```text
/banlist [页码]
```

每页显示 10 条记录，包括封禁类型、执行者、原因、封禁时间和剩余时间。

## 部署组件

每个 Paper/Folia 后端服务器都需要安装 `SimpMC-Punish-<版本号>.jar`。Paper 端的配置、消息文件和 SQLite 数据（仅单服模式）保存在：

```text
plugins/SimpMC-Punish/
```

启用 Velocity 全网同步时，必须让所有 Paper 后端和 Velocity 组件连接同一个 MySQL 数据库；Velocity 组件只读该数据库，不会写入处罚记录。Velocity 只安装一份：

```text
plugins/SimpMC-Punish-Velocity-<版本号>.jar
```

放入 Velocity 的 `plugins/` 目录即可。网页、网页接口和数据库查询都由这份 Velocity JAR 提供，不再存在单独的 Web JAR 或 Web 进程。

## 开发与版本

所有准备合并的变更都必须更新受影响产物的版本号和更新日志。详细规则见 [变更与版本规范](CONTRIBUTING.md)，历史记录见 [更新日志](CHANGELOG.md)。

## 构建插件

```bash
mvn package
```

插件 JAR 输出位置：

```text
target/SimpMC-Punish-<版本号>.jar
```

## 构建 Velocity 组件

```bash
mvn -f simpmc-punish-velocity/pom.xml package
```

Velocity JAR 输出位置：

```text
simpmc-punish-velocity/target/SimpMC-Punish-Velocity-<版本号>.jar
```

Velocity 配置和网页

把 `SimpMC-Punish-Velocity-<版本号>.jar` 放入 Velocity 的 `plugins/` 目录。Velocity 首次启动后会生成：

```text
plugins/simpmc-punish-velocity/config.yml
plugins/simpmc-punish-velocity/messages.yml
```

Velocity 组件必须连接 Paper 端使用的同一个 MySQL 数据库，不能读取 Paper 端的 SQLite 文件。管理网页默认只监听 `127.0.0.1:8080`，访问 `/` 即可；生产环境建议通过反向代理或受限网络暴露。

为避免 Velocity 在插件接管前按多行理由输出后端 kick 日志，请在 `velocity.toml` 中设置：

```toml
log-player-connections = false
```

然后完整重启 Velocity。SimpMC-Punish-Velocity 会为后端 kick 保留一条压平后的审计日志；该设置同时会关闭 Velocity 自身的连接/断开日志。

## 升级到 3.0.0

此版本将网页管理能力合并到 Velocity 插件，并要求 Paper 与 Velocity 共同使用 MySQL，属于不兼容升级。停止旧的独立网页 JAR，删除 `simpmc-punish-web` 配置；将 Paper 的 `database.type` 改为 `mysql`，然后在 Velocity 配置中填写同一组数据库连接信息。管理命令仍为 `/simpunish`，不提供 `/smp` 别名。
