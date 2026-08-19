# 更新日志

本项目的版本更新遵循 [SimpMC-Punish 变更与版本规范](CONTRIBUTING.md)。

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
