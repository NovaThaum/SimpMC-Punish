# 更新日志

本项目的版本更新遵循 [SimpMC-Punish 变更与版本规范](CONTRIBUTING.md)。

## [3.3.1] - 2026-09-13

### 移除

- 移除 Discord Webhook 转发、后台批量发送任务及相关配置。
- 移除 `behavior.vanilla-ban-components` 开关，Paper 与 Velocity 始终使用 Minecraft 原版可翻译封禁组件。

## [3.3.0] - 2026-09-13

### 新增

- 增加可配置的按次数递进处罚命令，例如 `/punish1 <玩家> [原因]`。
- 自定义命令的处罚次数持久化到数据库，支持 `kick`、`ban`、`tempban`、`mute`、`tempmute`、`warn` 和 `tempwarn`。
- 支持执行 `/simpunish 重载` 后重新注册自定义处罚命令。
- `steps` 支持按 `1:`、`2:` 编写递进次数，步骤默认自动继承原命令的玩家和原因参数。

## [3.2.1] - 2026-09-13

### 修复

- 修复数据库表异步初始化导致服务器重启后禁言查询或写入失败的问题。
- 插件现在会在处罚表和玩家 IP 表初始化完成后再注册命令与监听器。

## [3.2.0] - 2026-09-12

### 新增

- 增加 `/warn`、`/tempwarn` 和 `/warnings`，支持永久警告、临时警告、原因、到期时间和当前生效警告查询。
- 警告记录复用现有处罚历史、数据库和广播体系，并显示在 Velocity 管理面板的记录与统计中。
- 增加 `/clearwarnings` 清除当前生效警告，以及 `/punishinfo <编号>` 查看完整处罚详情。
- IP 封禁和 IP 禁言的 UUID 记录与 IP 记录改为在同一个数据库事务中写入，避免部分写入导致处罚状态不一致。

## [3.1.4] - 2026-09-12

### 修复

- 缩短 UUID 和 IP 禁言缓存的默认有效期至 5 秒，修复跨服执行解除禁言后其他后端可能继续使用旧禁言状态数分钟的问题。

## [3.1.3] - 2026-09-12

### 修复

- 修复永久封禁玩家被踢下线后，因 `OfflinePlayer.hasPlayedBefore()` 判断失败而无法使用 `/unban` 解除的问题；现在直接按目标 UUID 处理解除请求。

## [3.1.2] - 2026-09-12

### 优化

- `kick`、`ban`、`mute` 及处罚菜单的处罚广播现在发送给所有在线玩家，不再要求 `simpmc-punish.staff` 权限。
- 玩家被禁言后尝试发言时，改为立即提示“你的发言已被禁止”，并显示禁言原因和剩余时间，行为与 Essentials 一致。

## [3.1.1] - 2026-09-06

### 优化

- Paper/Folia 开启 `behavior.vanilla-ban-components` 后，踢出玩家的断开消息同样改用 Minecraft 原版可翻译组件 `multiplayer.disconnect.banned.reason`，玩家客户端会按自身语言显示踢出原因；踢出没有到期时间，因此不会追加 `multiplayer.disconnect.banned.expiration`。
- 关闭该开关时，踢出仍使用 `messages.yml` 中的 `punishments.kick.screen` 模板。

## [3.1.0] - 2026-09-05

### 新增

- Paper/Folia 与 Velocity 新增 `behavior.vanilla-ban-components` 开关；新安装默认开启，旧配置缺少该项时继续使用原有自定义封禁页。

### 优化

- 启用新开关后，Paper/Folia 与 Velocity 的封禁断开消息改用 Minecraft 原版可翻译组件，与 Essentials/Bukkit 原生封禁行为一致。
- 临时玩家封禁在 Velocity 控制台中会显示 `multiplayer.disconnect.banned.reasonmultiplayer.disconnect.banned.expiration`，玩家客户端仍会按自身语言显示封禁原因和到期时间。
- 永久玩家封禁只使用 `multiplayer.disconnect.banned.reason`，不会追加到期翻译键。
- 修正部署说明，不再要求关闭会输出上述翻译键的 Velocity 原生连接日志。

## [3.0.1] - 2026-08-23

### 修复

- 修复数据库初始化失败后仍注册封禁登录监听器的问题。
- 数据库不可用时不再为每次玩家登录打印完整异常堆栈；插件会记录一次初始化错误并自动停用，避免控制台刷屏。
- 登录封禁检查增加数据库连接状态保护，防止数据库连接池关闭或初始化失败时重复查询。

## [3.0.0] - 2026-08-19

### 不兼容变更

- 新增可独立部署的 `SimpMC-Punish-Velocity` 组件；Paper 端写入处罚，Velocity 端只读共享 MySQL 并执行全网封禁同步。
- 管理网页已合并到 Velocity JAR，移除独立 `simpmc-punish-web` 服务和单独启动方式。
- Velocity 负责登录、切服阶段的封禁拦截，以及后端 kick 的统一断开处理；数据库故障默认 fail-open，可配置为拒绝登录。
- Paper 与 Velocity 组件版本统一为 `3.0.0`，部署时必须使用同一 MySQL 数据库。
- 为彻底消除 Velocity 原生多行 kick 日志，部署时需将 `velocity.toml` 的 `log-player-connections` 设为 `false`；Velocity 组件会保留单条压平审计日志。
- 玩家端 kick/ban 断开页面恢复为可配置多行内容；2.0.1 的精确默认单行模板会自动迁移，自定义模板保持不变。

## [2.0.1] - 2026-08-18

### 修复

- 管理命令统一为 `/simpunish`，移除错误的 `/simpmc-punish` 和 `/smp` 注册及帮助文本。
- 插件构建和发布文件名统一为 `SimpMC-Punish-<版本号>.jar`。
- kick 和 ban 的玩家断开提示改为 EssentialsX 风格的单行理由；已有 `messages.yml` 中的多行模板也会在运行时压成单行，避免代理或服务端控制台逐行刷屏。
- 处罚广播已覆盖控制台或工作人员执行者时，不再重复发送一条成功消息；异步处罚结果会切回服务器调度器后再通知。
- 已有 `messages.yml` 中残留的 `SimpBan` 名称会在加载时自动迁移为 `SimpMC-Punish`。

## [2.0.0] - 2026-08-17

### 不兼容变更

- 插件及网页服务统一更名为 SimpMC-Punish；包名、Maven 坐标、构建产物、权限节点、管理命令和网页模块目录已同步更新。
- 插件数据目录变更为 `plugins/SimpMC-Punish`，升级时需要迁移现有 SQLite 数据库；管理权限需从 `simpban.*` 更新为 `simpmc-punish.*`。

### 优化

- 封禁登录检查采用 EssentialsX 风格：仅生成玩家拒绝连接提示，不再为每次已封禁的连接尝试记录信息日志。
- 本插件执行的踢出和封禁会抑制默认离服广播，仅保留一次可配置的工作人员处罚通知。

## [1.1.0] - 2026-07-31

### 新增

- 新增 `/banlist [页码]` 指令，用于查看当前生效中的玩家与 IP 封禁。
- 新增 `simpmc-punish.banlist` 权限及可配置的封禁列表消息。

### 优化

- 封禁列表使用一致快照分页查询，避免总数与列表内容不一致。
- 增加稳定排序和分页专用索引，改善大量处罚记录下的查询性能。
- 插件描述文件改为从 Maven 项目版本自动生成版本号。
