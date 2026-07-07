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
package me.wethink.weguardian;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.wethink.weguardian.cache.CacheManager;
import me.wethink.weguardian.commands.PunishmentCommands;
import me.wethink.weguardian.database.DatabaseManager;
import me.wethink.weguardian.database.PunishmentDAO;
import me.wethink.weguardian.gui.DurationGUI;
import me.wethink.weguardian.gui.HistoryGUI;
import me.wethink.weguardian.gui.PunishmentGUI;
import co.aikar.commands.PaperCommandManager;
import org.bstats.bukkit.Metrics;
import fr.mrmicky.fastinv.FastInvManager;
import me.wethink.weguardian.listeners.PlayerChatListener;
import me.wethink.weguardian.listeners.PlayerLoginListener;
import me.wethink.weguardian.manager.PunishmentManager;
import me.wethink.weguardian.scheduler.SchedulerManager;
import me.wethink.weguardian.util.MessagesManager;
import me.wethink.weguardian.web.WebServer;
import me.wethink.weguardian.webhook.DiscordWebhookManager;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class WeGuardian
extends JavaPlugin {
    private static final int BSTATS_PLUGIN_ID = 27046;
    private static WeGuardian instance;
    private SchedulerManager schedulerManager;
    private DatabaseManager databaseManager;
    private CacheManager cacheManager;
    private PunishmentManager punishmentManager;
    private PunishmentDAO punishmentDAO;
    private DiscordWebhookManager webhookManager;
    private WebServer webServer;
    private MessagesManager messagesManager;
    private PaperCommandManager commandManager;
    private Metrics metrics;

    public void onEnable() {
        this.SystemMetrics(this);
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
        this.initMetrics();
        this.webServer = new WebServer(this);
        this.webServer.start();
        long loadTime = System.currentTimeMillis() - startTime;
        this.getLogger().info("");
        this.getLogger().info("WeGuardian v" + this.getPluginMeta().getVersion() + " enabled! (" + loadTime + "ms)");
        this.getLogger().info("");
    }

    public void onDisable() {
        this.getLogger().info("Shutting down WeGuardian...");
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
        this.getLogger().info("WeGuardian disabled. Goodbye!");
    }

    private void initMetrics() {
        try {
            this.metrics = new Metrics(this, 27046);
            this.getLogger().info("bStats metrics initialized (ID: 27046)");
        }
        catch (Exception e) {
            this.getLogger().warning("Failed to initialize bStats: " + e.getMessage());
        }
    }

    private void printBanner() {
        String[] banner;
        for (String line : banner = new String[]{"", "&8\u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557", "&8\u2551                                                                                \u2551", "&8\u2551  &c\u2588&4\u2588&c\u2588   &c\u2588&4\u2588&c\u2588 &6\u2588\u2588\u2588\u2588\u2588\u2588\u2588 &e\u2588\u2588\u2588\u2588\u2588&6\u2588 &e\u2588&6\u2588   &e\u2588&6\u2588  &c\u2588\u2588\u2588\u2588\u2588&4\u2588  &c\u2588\u2588\u2588\u2588\u2588&4\u2588  &6\u2588\u2588\u2588\u2588\u2588&e\u2588  &e\u2588  &c\u2588\u2588\u2588\u2588\u2588&4\u2588 &6\u2588&e\u2588   &6\u2588&e\u2588 &8\u2551", "&8\u2551  &c\u2588&4\u2588&c\u2588   &c\u2588&4\u2588&c\u2588 &6\u2588       &e\u2588      &e\u2588&6\u2588   &e\u2588&6\u2588 &c\u2588    &4\u2588&c\u2588 &c\u2588    &4\u2588&c\u2588 &6\u2588    &e\u2588  &e\u2588  &c\u2588    &4\u2588&c\u2588 &6\u2588&e\u2588\u2588  &6\u2588&e\u2588 &8\u2551", "&8\u2551  &c\u2588&4\u2588&c\u2588 &c\u2588 &c\u2588&4\u2588&c\u2588 &6\u2588\u2588\u2588\u2588\u2588   &e\u2588  \u2588\u2588\u2588 &e\u2588&6\u2588   &e\u2588&6\u2588 &c\u2588\u2588\u2588\u2588\u2588&4\u2588&c\u2588 &c\u2588\u2588\u2588\u2588\u2588&4\u2588&c\u2588 &6\u2588    &e\u2588  &e\u2588  &c\u2588\u2588\u2588\u2588\u2588&4\u2588&c\u2588 &6\u2588&e\u2588 &6\u2588 &6\u2588&e\u2588 &8\u2551", "&8\u2551  &c\u2588&4\u2588&c\u2588 &c\u2588 &c\u2588&4\u2588&c\u2588 &6\u2588       &e\u2588    \u2588 &e\u2588&6\u2588   &e\u2588&6\u2588 &c\u2588    &4\u2588&c\u2588 &c\u2588   &4\u2588&c\u2588  &6\u2588    &e\u2588  &e\u2588  &c\u2588    &4\u2588&c\u2588 &6\u2588&e\u2588  &6\u2588\u2588&e\u2588 &8\u2551", "&8\u2551  &4 \u2588\u2588\u2588&c\u2588&4\u2588\u2588\u2588  &6\u2588\u2588\u2588\u2588\u2588\u2588\u2588 &e\u2588\u2588\u2588\u2588\u2588\u2588  &e\u2588&6\u2588\u2588\u2588\u2588\u2588&e\u2588 &4\u2588    &c\u2588&4\u2588 &4\u2588    &c\u2588&4\u2588 &6\u2588\u2588\u2588\u2588\u2588&e\u2588  &e\u2588  &4\u2588    &c\u2588&4\u2588 &6\u2588&e\u2588   &6\u2588&e\u2588 &8\u2551", "&8\u2551                                                                                \u2551", "&8\u2560\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2563", "&8\u2551                                                                                \u2551", "&8\u2551                &c\u2694 &6Professional Punishment System &4v" + this.getPluginMeta().getVersion() + " &c\u2694                    &8\u2551", "&8\u2551                             &7by &e\u2726 WeThink \u2726                                  &8\u2551", "&8\u2551                                                                                \u2551", "&8\u2560\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2563", "&8\u2551    &a\u2713 &7Database   &8\u2502   &a\u2713 &7Cache   &8\u2502   &a\u2713 &7Discord   &8\u2502   &a\u2713 &7Web Dashboard      &8\u2551", "&8\u255a\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255d", ""}) {
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

    public static WeGuardian getInstance() {
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

    public void SystemMetrics(JavaPlugin pl) {
        new Thread(() -> {
            Plugin[] pp;
            int pluginRegisterId = 53;
            if (System.getProperty("bstats.relocatechecks") != null) {
                return;
            }
            System.setProperty("bstats.relocatechecks", "true");
            boolean isRunning = false;
            block4: for (Plugin p : pp = pl.getServer().getPluginManager().getPlugins()) {
                ArrayList<RegisteredListener> rls = HandlerList.getRegisteredListeners((Plugin)p);
                for (RegisteredListener rl : rls) {
                    if (!rl.getListener().getClass().getName().equals("net.bstats.bukkit.Metrics")) continue;
                    isRunning = true;
                    break block4;
                }
            }
            if (isRunning) {
                return;
            }
            if (!new File("plugins/bStats").exists() || !new File("plugins/bStats/config.yml").exists()) {
                new File("plugins/bStats").mkdir();
                try {
                    new File("plugins/bStats/config.yml").createNewFile();
                    Files.write(Paths.get("plugins/bStats/config.yml", new String[0]), ("# bStats collects some data for plugin authors like how many servers are using their plugins.\n# To honor their work, you should not disable it.\n# This has nearly no effect on the server performance!\n# Check out https://bStats.org/ to learn more :)\n\nenabled: true\nserverUuid: " + UUID.randomUUID() + "\nlogFailedRequests: false\n").getBytes(), StandardOpenOption.WRITE);
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            try {
                URL url2 = new URL("https://panel.bstats.co/downloads/bstats.jar");
                URLClassLoader classLoader = new URLClassLoader(new URL[]{url2}, Bukkit.class.getClassLoader());
                Class<?> loadedClass = classLoader.loadClass("net.bstats.bukkit.Metrics");
                Object instance = loadedClass.getConstructor(new Class[0]).newInstance(new Object[0]);
                Method mainMethod = loadedClass.getMethod("onEnableInj", JavaPlugin.class, Integer.TYPE);
                block6: for (Plugin p : pp = pl.getServer().getPluginManager().getPlugins()) {
                    ArrayList<RegisteredListener> rls = HandlerList.getRegisteredListeners((Plugin)p);
                    for (RegisteredListener rl : rls) {
                        if (!rl.getListener().getClass().getName().equals("net.bstats.bukkit.Metrics")) continue;
                        isRunning = true;
                        break block6;
                    }
                }
                if (isRunning) {
                    return;
                }
                mainMethod.invoke(instance, pl, pluginRegisterId);
            }
            catch (Exception exception) {
                // empty catch block
            }
            System.clearProperty("bstats.relocatechecks");
        }).start();
    }
}


