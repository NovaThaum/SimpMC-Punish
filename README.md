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

新安装默认使用 Minecraft 原版可翻译封禁组件，与 Essentials/Bukkit 原生封禁一致。Velocity 开启连接日志时，临时玩家封禁会在控制台显示：

```text
multiplayer.disconnect.banned.reasonmultiplayer.disconnect.banned.expiration
```

这是代理端对原版翻译组件的纯文本表示；玩家客户端仍会按自身语言显示实际的封禁原因和到期时间。永久玩家封禁和踢出只包含 `multiplayer.disconnect.banned.reason`，踢出不会追加到期翻译键。

为避免升级时静默覆盖已有自定义封禁页，旧版 `config.yml` 若没有下面这个配置项，会继续使用 `messages.yml` 中的原有模板。要启用原版组件，请在 Paper 和 Velocity 两端各自的 `config.yml` 中添加：

```yaml
behavior:
  vanilla-ban-components: true
```

设为 `false` 可随时恢复对应 `messages.yml` 中的自定义封禁页和踢出页。

处罚执行广播默认发送给所有在线玩家。被禁言的玩家尝试发言时，会收到发言被禁止的提示，并看到禁言原因和剩余时间。

插件支持 `/warn <玩家> [原因]`、`/tempwarn <玩家> <时长> [原因]` 和 `/warnings <玩家>` 管理警告记录。警告会进入处罚历史，并根据配置广播给所有在线玩家。

启用多个 Paper/Folia 后端时，禁言缓存默认每 5 秒重新同步一次共享数据库，因此在一个后端解除禁言后，其他后端通常会在 5 秒内同步解除状态。缓存未命中时，聊天监听器也会回查共享数据库。可通过 `cache.mute-expiry-seconds` 和 `cache.mute-check-timeout-ms` 调整同步周期与查询等待时间。

如果数据库连接失败，Paper 插件会记录初始化原因并自动停用，不会继续注册封禁登录检查，也不会为每次玩家登录重复打印“数据库尚未初始化”。修正数据库地址、端口、库名、账号或密码后重启服务器即可。

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

要让 Velocity 控制台显示上述原版翻译键，请在 `velocity.toml` 中保持：

```toml
log-player-connections = true
```

修改后需要完整重启 Velocity。若设为 `false`，Velocity 会关闭自身的连接和断开日志，因此也不会输出上述翻译键；插件仍会按 `logging.backend-kicks` 记录未匹配到处罚记录的后端 kick。

## 升级到 3.0.0

此版本将网页管理能力合并到 Velocity 插件，并要求 Paper 与 Velocity 共同使用 MySQL，属于不兼容升级。停止旧的独立网页 JAR，删除 `simpmc-punish-web` 配置；将 Paper 的 `database.type` 改为 `mysql`，然后在 Velocity 配置中填写同一组数据库连接信息。管理命令仍为 `/simpunish`，不提供 `/smp` 别名。
