/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.RegisteredListener
 *  org.bukkit.plugin.java.JavaPlugin
 */
package me.simpfun.simpban;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.simpfun.simpban.cache.CacheManager;
import me.simpfun.simpban.commands.PunishmentCommands;
import me.simpfun.simpban.database.DatabaseManager;
import me.simpfun.simpban.database.PunishmentDAO;
import me.simpfun.simpban.gui.DurationGUI;
import me.simpfun.simpban.gui.HistoryGUI;
import me.simpfun.simpban.gui.PunishmentGUI;
import co.aikar.commands.PaperCommandManager;
import fr.mrmicky.fastinv.FastInvManager;
import me.simpfun.simpban.listeners.PlayerChatListener;
import me.simpfun.simpban.listeners.PlayerLoginListener;
import me.simpfun.simpban.manager.PunishmentManager;
import me.simpfun.simpban.scheduler.SchedulerManager;
import me.simpfun.simpban.util.MessagesManager;
import me.simpfun.simpban.web.WebServer;
import me.simpfun.simpban.webhook.DiscordWebhookManager;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class SimpBan
extends JavaPlugin {
    private static SimpBan instance;
    private SchedulerManager schedulerManager;
    private DatabaseManager databaseManager;
    private CacheManager cacheManager;
    private PunishmentManager punishmentManager;
    private PunishmentDAO punishmentDAO;
    private DiscordWebhookManager webhookManager;
    private WebServer webServer;
    private MessagesManager messagesManager;
    private PaperCommandManager commandManager;

    public void onEnable() {
        instance = this;
        long startTime = System.currentTimeMillis();
        this.printBanner();
        this.saveDefaultConfig();
        this.messagesManager = new MessagesManager(this);
        this.getLogger().info("Messages loaded");
        FastInvManager.register((Plugin)this);
        PunishmentGUI.initializeIcons();
        DurationGUI.initializeIcons();
        HistoryGUI.initializeIcons();
        this.getLogger().info("GUI icons pre-cached");
        this.getLogger().info("Initializing managers...");
        this.schedulerManager = new SchedulerManager(this);
        this.getLogger().info("Scheduler initialized" + (this.schedulerManager.isFolia() ? " (Folia mode)" : " (Bukkit mode)"));
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.initialize();
        this.getLogger().info("Database initialized");
        this.punishmentDAO = new PunishmentDAO(this, this.databaseManager);
        this.cacheManager = new CacheManager(this);
        this.getLogger().info("Cache initialized");
        this.punishmentManager = new PunishmentManager(this, this.punishmentDAO, this.cacheManager);
        this.getLogger().info("Punishment manager initialized");
        this.webhookManager = new DiscordWebhookManager(this);
        this.getLogger().info("Discord webhook manager initialized");
        this.registerCommands();
        this.getLogger().info("Commands registered");
        this.registerListeners();
        this.getLogger().info("Listeners registered");
        this.webServer = new WebServer(this);
        this.webServer.start();
        long loadTime = System.currentTimeMillis() - startTime;
        this.getLogger().info("");
        this.getLogger().info("SimpBan v" + this.getPluginMeta().getVersion() + " enabled! (" + loadTime + "ms)");
        this.getLogger().info("");
    }

    public void onDisable() {
        this.getLogger().info("Shutting down SimpBan...");
        if (this.webServer != null) {
            this.webServer.stop();
        }
        if (this.schedulerManager != null) {
            this.schedulerManager.shutdown();
        }
        if (this.databaseManager != null) {
            this.databaseManager.shutdown();
        }
        if (this.cacheManager != null) {
            this.cacheManager.clear();
        }
        this.getLogger().info("SimpBan disabled. Goodbye!");
    }

    private void printBanner() {
        String[] banner;
        for (String line : banner = new String[]{"", "&8\u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557", "&8\u2551                                                                                \u2551", "&8\u2551  &c\u2588&4\u2588&c\u2588   &c\u2588&4\u2588&c\u2588 &6\u2588\u2588\u2588\u2588\u2588\u2588\u2588 &e\u2588\u2588\u2588\u2588\u2588&6\u2588 &e\u2588&6\u2588   &e\u2588&6\u2588  &c\u2588\u2588\u2588\u2588\u2588&4\u2588  &c\u2588\u2588\u2588\u2588\u2588&4\u2588  &6\u2588\u2588\u2588\u2588\u2588&e\u2588  &e\u2588  &c\u2588\u2588\u2588\u2588\u2588&4\u2588 &6\u2588&e\u2588   &6\u2588&e\u2588 &8\u2551", "&8\u2551  &c\u2588&4\u2588&c\u2588   &c\u2588&4\u2588&c\u2588 &6\u2588       &e\u2588      &e\u2588&6\u2588   &e\u2588&6\u2588 &c\u2588    &4\u2588&c\u2588 &c\u2588    &4\u2588&c\u2588 &6\u2588    &e\u2588  &e\u2588  &c\u2588    &4\u2588&c\u2588 &6\u2588&e\u2588\u2588  &6\u2588&e\u2588 &8\u2551", "&8\u2551  &c\u2588&4\u2588&c\u2588 &c\u2588 &c\u2588&4\u2588&c\u2588 &6\u2588\u2588\u2588\u2588\u2588   &e\u2588  \u2588\u2588\u2588 &e\u2588&6\u2588   &e\u2588&6\u2588 &c\u2588\u2588\u2588\u2588\u2588&4\u2588&c\u2588 &c\u2588\u2588\u2588\u2588\u2588&4\u2588&c\u2588 &6\u2588    &e\u2588  &e\u2588  &c\u2588\u2588\u2588\u2588\u2588&4\u2588&c\u2588 &6\u2588&e\u2588 &6\u2588 &6\u2588&e\u2588 &8\u2551", "&8\u2551  &c\u2588&4\u2588&c\u2588 &c\u2588 &c\u2588&4\u2588&c\u2588 &6\u2588       &e\u2588    \u2588 &e\u2588&6\u2588   &e\u2588&6\u2588 &c\u2588    &4\u2588&c\u2588 &c\u2588   &4\u2588&c\u2588  &6\u2588    &e\u2588  &e\u2588  &c\u2588    &4\u2588&c\u2588 &6\u2588&e\u2588  &6\u2588\u2588&e\u2588 &8\u2551", "&8\u2551  &4 \u2588\u2588\u2588&c\u2588&4\u2588\u2588\u2588  &6\u2588\u2588\u2588\u2588\u2588\u2588\u2588 &e\u2588\u2588\u2588\u2588\u2588\u2588  &e\u2588&6\u2588\u2588\u2588\u2588\u2588&e\u2588 &4\u2588    &c\u2588&4\u2588 &4\u2588    &c\u2588&4\u2588 &6\u2588\u2588\u2588\u2588\u2588&e\u2588  &e\u2588  &4\u2588    &c\u2588&4\u2588 &6\u2588&e\u2588   &6\u2588&e\u2588 &8\u2551", "&8\u2551                                                                                \u2551", "&8\u2560\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2563", "&8\u2551                                                                                \u2551", "&8\u2551                &c\u2694 &6Professional Punishment System &4v" + this.getPluginMeta().getVersion() + " &c\u2694                    &8\u2551", "&8\u2551                             &7by &e\u2726 GPT5.5, Minecraft0122, SimpFun \u2726                                  &8\u2551", "&8\u2551                                                                                \u2551", "&8\u2560\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2563", "&8\u2551    &a\u2713 &7Database   &8\u2502   &a\u2713 &7Cache   &8\u2502   &a\u2713 &7Discord   &8\u2502   &a\u2713 &7Web Dashboard      &8\u2551", "&8\u255a\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255d", ""}) {
            this.getServer().getConsoleSender().sendMessage(this.colorize(line));
        }
    }

    private String colorize(String message) {
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("\u00a7x");
            for (char c : hex.toCharArray()) {
                replacement.append("\u00a7").append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return buffer.toString().replace("&", "\u00a7");
    }

    private void registerCommands() {
        this.commandManager = new PaperCommandManager((Plugin)this);
        try {
            this.commandManager.enableUnstableAPI("help");
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.commandManager.getCommandCompletions().registerAsyncCompletion("players", c -> this.getServer().getOnlinePlayers().stream().map(p -> p.getName()).toList());
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

    public WebServer getWebServer() {
        return this.webServer;
    }

    public MessagesManager getMessagesManager() {
        return this.messagesManager;
    }

}


