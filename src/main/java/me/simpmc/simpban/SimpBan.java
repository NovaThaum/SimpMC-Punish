package me.simpmc.simpban;

import co.aikar.commands.PaperCommandManager;
import fr.mrmicky.fastinv.FastInvManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.simpmc.simpban.cache.CacheManager;
import me.simpmc.simpban.commands.PunishmentCommands;
import me.simpmc.simpban.database.DatabaseManager;
import me.simpmc.simpban.database.PunishmentDAO;
import me.simpmc.simpban.gui.DurationGUI;
import me.simpmc.simpban.gui.HistoryGUI;
import me.simpmc.simpban.gui.PunishmentGUI;
import me.simpmc.simpban.listeners.PlayerChatListener;
import me.simpmc.simpban.listeners.PlayerLoginListener;
import me.simpmc.simpban.manager.PunishmentManager;
import me.simpmc.simpban.scheduler.SchedulerManager;
import me.simpmc.simpban.util.MessagesManager;
import me.simpmc.simpban.webhook.DiscordWebhookManager;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class SimpBan extends JavaPlugin {
    private static SimpBan instance;
    private SchedulerManager schedulerManager;
    private DatabaseManager databaseManager;
    private CacheManager cacheManager;
    private PunishmentManager punishmentManager;
    private PunishmentDAO punishmentDAO;
    private DiscordWebhookManager webhookManager;
    private MessagesManager messagesManager;
    private PaperCommandManager commandManager;

    @Override
    public void onEnable() {
        instance = this;
        long startTime = System.currentTimeMillis();
        this.printBanner();
        this.saveDefaultConfig();

        this.messagesManager = new MessagesManager(this);
        this.getLogger().info("消息文件已加载");

        FastInvManager.register((Plugin)this);
        PunishmentGUI.initializeIcons();
        DurationGUI.initializeIcons();
        HistoryGUI.initializeIcons();
        this.getLogger().info("菜单图标已预缓存");

        this.getLogger().info("正在初始化管理器...");
        this.schedulerManager = new SchedulerManager(this);
        this.getLogger().info("调度器已初始化" + (this.schedulerManager.isFolia() ? " (Folia 模式)" : " (Bukkit 模式)"));
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.initialize();
        this.getLogger().info("数据库已初始化");
        this.punishmentDAO = new PunishmentDAO(this, this.databaseManager);
        this.cacheManager = new CacheManager(this);
        this.getLogger().info("缓存已初始化");
        this.punishmentManager = new PunishmentManager(this, this.punishmentDAO, this.cacheManager);
        this.getLogger().info("处罚管理器已初始化");
        this.webhookManager = new DiscordWebhookManager(this);
        this.getLogger().info("Discord 通知管理器已初始化");

        this.registerCommands();
        this.getLogger().info("命令已注册");
        this.registerListeners();
        this.getLogger().info("监听器已注册");

        long loadTime = System.currentTimeMillis() - startTime;
        this.getLogger().info("");
        this.getLogger().info("SimpBan v" + this.getPluginMeta().getVersion() + " 已启用！(" + loadTime + "ms)");
        this.getLogger().info("");
    }

    @Override
    public void onDisable() {
        this.getLogger().info("正在关闭 SimpBan...");
        if (this.schedulerManager != null) {
            this.schedulerManager.shutdown();
        }
        if (this.databaseManager != null) {
            this.databaseManager.shutdown();
        }
        if (this.cacheManager != null) {
            this.cacheManager.clear();
        }
        this.getLogger().info("SimpBan 已关闭。");
    }

    private void printBanner() {
        String[] banner = new String[]{
                "",
                "&8========================================",
                "&cSimpBan &7v" + this.getPluginMeta().getVersion(),
                "&7专业 Minecraft 处罚管理系统",
                "&7作者: &eGPT5.5, Minecraft0122, SimpMC",
                "&8模块: &a数据库 &8| &a缓存 &8| &a通知",
                "&8========================================",
                ""
        };
        for (String line : banner) {
            this.getServer().getConsoleSender().sendMessage(this.colorize(line));
        }
    }

    private String colorize(String message) {
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append("§").append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return buffer.toString().replace("&", "§");
    }

    private void registerCommands() {
        this.commandManager = new PaperCommandManager((Plugin)this);
        try {
            this.commandManager.enableUnstableAPI("help");
        } catch (Exception ignored) {
        }
        this.commandManager.getCommandCompletions()
                .registerAsyncCompletion("players", c -> this.getServer().getOnlinePlayers().stream().map(p -> p.getName()).toList());
        this.commandManager.registerCommand(new PunishmentCommands(this));
    }

    private void registerListeners() {
        this.getServer().getPluginManager().registerEvents((Listener)new PlayerLoginListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new PlayerChatListener(this), (Plugin)this);
    }

    public static SimpBan getInstance() {
        return instance;
    }

    public SchedulerManager getSchedulerManager() {
        return this.schedulerManager;
    }

    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    public CacheManager getCacheManager() {
        return this.cacheManager;
    }

    public PunishmentManager getPunishmentManager() {
        return this.punishmentManager;
    }

    public PunishmentDAO getPunishmentDAO() {
        return this.punishmentDAO;
    }

    public PaperCommandManager getCommandManager() {
        return this.commandManager;
    }

    public DiscordWebhookManager getWebhookManager() {
        return this.webhookManager;
    }

    public MessagesManager getMessagesManager() {
        return this.messagesManager;
    }
}
