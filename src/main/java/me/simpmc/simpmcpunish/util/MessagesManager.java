package me.simpmc.simpmcpunish.util;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class MessagesManager {
    private static final String V2_BAN_SCREEN = "&c你已被封禁: &f{reason} &8| &7到期: &f{expires}";
    private static final String V2_KICK_SCREEN = "&6你已被踢出服务器: &f{reason}";
    private static final String V2_MUTE_BLOCKED = "&c&l你已被禁言\n&7执行者: &f{staff}\n&7原因: &f{reason}\n&7到期: &f{expires}";
    private final JavaPlugin plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public MessagesManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.loadMessages();
    }

    public void loadMessages() {
        this.messagesFile = new File(this.plugin.getDataFolder(), "messages.yml");
        if (!this.messagesFile.exists()) {
            this.plugin.saveResource("messages.yml", false);
        }
        this.messagesConfig = YamlConfiguration.loadConfiguration((File)this.messagesFile);
        InputStream defaultStream = this.plugin.getResource("messages.yml");
        YamlConfiguration defaultConfig = null;
        if (defaultStream != null) {
            defaultConfig = YamlConfiguration.loadConfiguration((Reader)new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            this.messagesConfig.setDefaults((Configuration)defaultConfig);
        }
        this.migrateLegacyValues(defaultConfig);
    }

    public void reload() {
        this.loadMessages();
    }

    public String getMessage(String path) {
        return this.messagesConfig.getString(path, "");
    }

    public String getMessage(String path, String defaultValue) {
        return this.messagesConfig.getString(path, defaultValue);
    }

    public String getMessage(String path, String ... replacements) {
        String message = this.getMessage(path);
        if (message == null || replacements.length == 0) {
            return message;
        }
        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return message;
    }

    public List<String> getMessageList(String path) {
        return this.messagesConfig.getStringList(path);
    }

    public List<String> getMessageList(String path, String ... replacements) {
        List<String> messages = this.getMessageList(path);
        if (messages.isEmpty() || replacements.length == 0) {
            return messages;
        }
        return messages.stream().map(msg -> {
            String result = msg;
            for (int i = 0; i < replacements.length - 1; i += 2) {
                result = result.replace(replacements[i], replacements[i + 1]);
            }
            return result;
        }).toList();
    }

    public String getPrefix() {
        return this.getMessage("prefix", "&8[&c&lSimpMC-Punish&8]&r ");
    }

    public FileConfiguration getConfig() {
        return this.messagesConfig;
    }

    private void migrateLegacyValues(YamlConfiguration defaults) {
        boolean changed = false;
        for (String path : this.messagesConfig.getKeys(true)) {
            if (!this.messagesConfig.isString(path)) {
                continue;
            }
            String value = this.messagesConfig.getString(path);
            if (value == null || !value.contains("SimpBan")) {
                continue;
            }
            this.messagesConfig.set(path, value.replace("SimpBan", "SimpMC-Punish"));
            changed = true;
        }
        if (defaults != null && V2_BAN_SCREEN.equals(this.messagesConfig.getString("punishments.ban.screen"))) {
            this.messagesConfig.set("punishments.ban.screen", defaults.getString("punishments.ban.screen"));
            changed = true;
        }
        if (defaults != null && V2_KICK_SCREEN.equals(this.messagesConfig.getString("punishments.kick.screen"))) {
            this.messagesConfig.set("punishments.kick.screen", defaults.getString("punishments.kick.screen"));
            changed = true;
        }
        if (defaults != null && V2_MUTE_BLOCKED.equals(this.messagesConfig.getString("punishments.mute.blocked", "").trim())) {
            this.messagesConfig.set("punishments.mute.blocked", defaults.getString("punishments.mute.blocked"));
            changed = true;
        }
        if (!changed) {
            return;
        }
        try {
            this.messagesConfig.save(this.messagesFile);
            this.plugin.getLogger().info("已迁移 messages.yml 中的旧品牌或 2.x 断开页面默认值。");
        } catch (IOException exception) {
            this.plugin.getLogger().warning("无法保存 messages.yml 品牌迁移结果: " + exception.getMessage());
        }
    }
}


