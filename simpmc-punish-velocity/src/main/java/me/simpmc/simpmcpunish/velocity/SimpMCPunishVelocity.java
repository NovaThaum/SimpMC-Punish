package me.simpmc.simpmcpunish.velocity;

import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;

@Plugin(
        id = "simpmc-punish-velocity",
        name = "SimpMC-Punish Velocity",
        version = "3.1.0",
        description = "SimpMC-Punish 的 Velocity 全网处罚执行与管理面板",
        authors = {"GPT5.5", "Minecraft0122", "SimpMC"})
public final class SimpMCPunishVelocity {
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private final ExecutorService databaseExecutor = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "SimpMC-Punish-Velocity-Database");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicReference<BanSnapshot> snapshot = new AtomicReference<>(BanSnapshot.empty());
    private final AtomicBoolean refreshRunning = new AtomicBoolean();
    private final AtomicLong lastDatabaseErrorLogAt = new AtomicLong();
    private final Map<String, Instant> loginLogTimes = new ConcurrentHashMap<>();
    private volatile VelocityConfiguration configuration;
    private volatile VelocityDatabase database;
    private volatile VelocityMessageRenderer renderer;
    private volatile EmbeddedWebServer webServer;
    private volatile ScheduledTask syncTask;

    public SimpMCPunishVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent ignored) {
        CompletableFuture.runAsync(this::initialize, this.databaseExecutor);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent ignored) {
        close();
    }

    @Subscribe(priority = Short.MIN_VALUE)
    public EventTask onLogin(LoginEvent event) {
        if (!event.getResult().isAllowed()) {
            return null;
        }
        CompletableFuture<Void> check = CompletableFuture.runAsync(() -> {
            VelocityDatabase currentDatabase = this.database;
            VelocityConfiguration currentConfiguration = this.configuration;
            VelocityMessageRenderer currentRenderer = this.renderer;
            if (currentDatabase == null || currentConfiguration == null || currentRenderer == null) {
                if (currentConfiguration != null && currentConfiguration.denyLoginOnDatabaseError()) {
                    event.setResult(ResultedEvent.ComponentResult.denied(currentRenderer == null
                            ? net.kyori.adventure.text.Component.text("处罚数据库暂时不可用，请稍后重试。")
                            : currentRenderer.databaseError()));
                }
                return;
            }
            String ip = remoteIp(event.getPlayer().getRemoteAddress());
            UUID uuid = event.getPlayer().getUniqueId();
            try {
                Optional<PunishmentRecord> punishment = currentDatabase.findActiveBan(uuid, ip);
                if (punishment.isPresent()) {
                    event.setResult(ResultedEvent.ComponentResult.denied(currentRenderer.banScreen(punishment.get())));
                    logDenied(event.getPlayer().getUsername(), ip, punishment.get());
                }
            } catch (Exception error) {
                logDatabaseFailure("读取登录处罚状态", error);
                if (currentConfiguration.denyLoginOnDatabaseError()) {
                    event.setResult(ResultedEvent.ComponentResult.denied(currentRenderer.databaseError()));
                }
            }
        }, this.databaseExecutor);
        return EventTask.resumeWhenComplete(check);
    }

    @Subscribe(priority = Short.MIN_VALUE)
    public EventTask onServerPreConnect(ServerPreConnectEvent event) {
        if (!event.getResult().isAllowed()) {
            return null;
        }
        CompletableFuture<Void> check = CompletableFuture.runAsync(() -> {
            VelocityDatabase currentDatabase = this.database;
            VelocityConfiguration currentConfiguration = this.configuration;
            VelocityMessageRenderer currentRenderer = this.renderer;
            if (currentDatabase == null || currentConfiguration == null || currentRenderer == null) {
                return;
            }
            Player player = event.getPlayer();
            String ip = remoteIp(player.getRemoteAddress());
            try {
                Optional<PunishmentRecord> punishment = currentDatabase.findActiveBan(player.getUniqueId(), ip);
                if (punishment.isPresent()) {
                    event.setResult(ServerPreConnectEvent.ServerResult.denied());
                    player.disconnect(currentRenderer.banScreen(punishment.get()));
                }
            } catch (Exception error) {
                logDatabaseFailure("读取切服处罚状态", error);
                if (currentConfiguration.denyLoginOnDatabaseError()) {
                    event.setResult(ServerPreConnectEvent.ServerResult.denied());
                    player.disconnect(currentRenderer.databaseError());
                }
            }
        }, this.databaseExecutor);
        return EventTask.resumeWhenComplete(check);
    }

    @Subscribe(priority = Short.MIN_VALUE)
    public EventTask onKickedFromServer(KickedFromServerEvent event) {
        CompletableFuture<Void> check = CompletableFuture.runAsync(() -> {
            VelocityConfiguration currentConfiguration = this.configuration;
            VelocityMessageRenderer currentRenderer = this.renderer;
            VelocityDatabase currentDatabase = this.database;
            if (currentConfiguration == null || currentRenderer == null) {
                return;
            }
            Player player = event.getPlayer();
            Optional<PunishmentRecord> punishment = Optional.empty();
            if (currentDatabase != null) {
                try {
                    punishment = currentDatabase.findActiveBan(player.getUniqueId(), remoteIp(player.getRemoteAddress()));
                } catch (Exception error) {
                    logDatabaseFailure("读取后端 kick 处罚状态", error);
                    punishment = this.snapshot.get().find(player.getUniqueId(), remoteIp(player.getRemoteAddress()));
                }
            }
            if (punishment.isPresent()) {
                event.setResult(KickedFromServerEvent.DisconnectPlayer.create(currentRenderer.banScreen(punishment.get())));
                return;
            }
            if (currentConfiguration.disconnectBackendKicks()) {
                net.kyori.adventure.text.Component reason = event.getServerKickReason().orElseGet(currentRenderer::fallbackKick);
                if (currentConfiguration.logBackendKicks()) {
                    this.logger.info("玩家 {} 被后端 {} 踢出: {}", player.getUsername(),
                            event.getServer().getServerInfo().getName(), currentRenderer.auditReason(reason));
                }
                event.setResult(KickedFromServerEvent.DisconnectPlayer.create(reason));
            }
        }, this.databaseExecutor);
        return EventTask.resumeWhenComplete(check);
    }

    private void initialize() {
        try {
            VelocityConfiguration loaded = VelocityConfiguration.load(this.dataDirectory, this.logger);
            VelocityMessages messages = VelocityMessages.load(this.dataDirectory);
            VelocityMessageRenderer messageRenderer = new VelocityMessageRenderer(
                    messages,
                    loaded.maxReasonLength(),
                    loaded.vanillaBanComponents());
            this.configuration = loaded;
            this.renderer = messageRenderer;
            VelocityDatabase loadedDatabase = new VelocityDatabase(loaded, this.logger);
            this.database = loadedDatabase;
            try {
                loadedDatabase.validateSchema();
            } catch (Exception error) {
                logDatabaseFailure("校验处罚数据库", error);
            }
            queueRefresh();
            this.syncTask = this.proxy.getScheduler().buildTask(this, this::queueRefresh)
                    .repeat(Duration.ofMillis(loaded.syncIntervalMs())).schedule();
            if (loaded.webEnabled()) {
                this.webServer = new EmbeddedWebServer(loaded, new VelocityPunishmentRepository(loadedDatabase));
                this.webServer.start();
                this.logger.info("SimpMC-Punish 管理面板已启动: http://{}:{}", loaded.webHost(), loaded.webPort());
            }
            this.logger.info("SimpMC-Punish Velocity 已启动，共享 MySQL 处罚同步已启用");
            if (loaded.vanillaBanComponents()) {
                this.logger.info("原版封禁组件已启用；如需在控制台查看其翻译键，"
                        + "请保持 velocity.toml 的 log-player-connections=true。");
            }
        } catch (Exception error) {
            this.logger.error("SimpMC-Punish Velocity 初始化失败，插件保持 fail-open: {}", error.getMessage(), error);
        }
    }

    private void refreshSnapshot() {
        VelocityDatabase currentDatabase = this.database;
        if (currentDatabase == null) {
            return;
        }
        try {
            BanSnapshot previous = this.snapshot.getAndSet(currentDatabase.loadActiveBans());
            BanSnapshot current = this.snapshot.get();
            for (Player player : this.proxy.getAllPlayers()) {
                String ip = remoteIp(player.getRemoteAddress());
                Optional<PunishmentRecord> oldBan = previous.find(player.getUniqueId(), ip);
                Optional<PunishmentRecord> newBan = current.find(player.getUniqueId(), ip);
                if (newBan.isPresent() && (oldBan.isEmpty() || oldBan.get().id() != newBan.get().id())) {
                    player.disconnect(this.renderer.banScreen(newBan.get()));
                }
            }
        } catch (Exception error) {
            logDatabaseFailure("刷新全网封禁快照", error);
        }
    }

    private void queueRefresh() {
        if (!this.refreshRunning.compareAndSet(false, true)) {
            return;
        }
        CompletableFuture.runAsync(this::refreshSnapshot, this.databaseExecutor)
                .whenComplete((ignored, error) -> this.refreshRunning.set(false));
    }

    private void logDatabaseFailure(String operation, Exception error) {
        VelocityConfiguration currentConfiguration = this.configuration;
        long rateLimitSeconds = currentConfiguration == null ? 30L : currentConfiguration.databaseErrorRateLimitSeconds();
        long now = System.currentTimeMillis();
        long last = this.lastDatabaseErrorLogAt.get();
        if (now - last < rateLimitSeconds * 1000L || !this.lastDatabaseErrorLogAt.compareAndSet(last, now)) {
            return;
        }
        this.logger.warn("{}失败: {}", operation, error.getMessage());
    }

    private void logDenied(String username, String ip, PunishmentRecord punishment) {
        VelocityConfiguration currentConfiguration = this.configuration;
        if (currentConfiguration == null || !currentConfiguration.logDeniedLoginAttempts()) {
            return;
        }
        String key = ip == null ? username : ip;
        Instant now = Instant.now();
        Instant last = this.loginLogTimes.putIfAbsent(key, now);
        if (last == null || Duration.between(last, now).getSeconds() >= currentConfiguration.deniedLoginRateLimitSeconds()) {
            this.loginLogTimes.put(key, now);
            this.logger.info("拒绝玩家 {} 的登录: {}", username, punishment.type());
        }
    }

    private void close() {
        ScheduledTask task = this.syncTask;
        if (task != null) {
            task.cancel();
        }
        EmbeddedWebServer web = this.webServer;
        if (web != null) {
            web.close();
        }
        VelocityDatabase currentDatabase = this.database;
        if (currentDatabase != null) {
            currentDatabase.close();
        }
        this.databaseExecutor.shutdownNow();
    }

    private static String remoteIp(InetSocketAddress address) {
        return address == null || address.getAddress() == null
                ? null
                : PunishmentRecord.normalizeIp(address.getAddress().getHostAddress());
    }
}
