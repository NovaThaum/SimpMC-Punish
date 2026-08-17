package me.simpmc.simpmcpunish.webhook;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import me.simpmc.simpmcpunish.SimpMCPunish;
import me.simpmc.simpmcpunish.model.Punishment;
import me.simpmc.simpmcpunish.model.PunishmentType;
import me.simpmc.simpmcpunish.util.TimeUtil;

public class DiscordWebhookManager {
    private static final int MAX_EMBEDS_PER_MESSAGE = 10;
    private static final int BATCH_INTERVAL_SECONDS = 5;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"));
    private static final int COLOR_BAN = 0xFF0000;
    private static final int COLOR_TEMPBAN = 0xFF6600;
    private static final int COLOR_MUTE = 0xFFFF00;
    private static final int COLOR_TEMPMUTE = 65280;
    private static final int COLOR_KICK = 39423;
    private static final int COLOR_UNBAN = 65416;
    private static final int COLOR_UNMUTE = 0x88FF00;
    private static final int COLOR_IP_BAN = 0xCC0000;
    private static final int COLOR_IP_MUTE = 0xCCCC00;
    private final SimpMCPunish plugin;
    private final ConcurrentLinkedQueue<WebhookEmbed> pendingEmbeds;
    private String webhookUrl;
    private boolean enabled;

    public DiscordWebhookManager(SimpMCPunish plugin) {
        this.plugin = plugin;
        this.pendingEmbeds = new ConcurrentLinkedQueue();
        this.reload();
        this.startBatchTask();
    }

    public void reload() {
        this.enabled = this.plugin.getConfig().getBoolean("discord.enabled", false);
        this.webhookUrl = this.plugin.getConfig().getString("discord.notification-url",
                this.plugin.getConfig().getString("discord.webhook-url", ""));
        if (this.enabled && (this.webhookUrl == null || this.webhookUrl.isEmpty()
                || this.webhookUrl.equals("请填写 Discord 通知地址"))) {
            this.plugin.getLogger().warning("Discord 通知已启用，但未配置有效 URL！");
            this.enabled = false;
        }
        if (this.enabled) {
            this.plugin.getLogger().info("Discord 通知日志已启用。");
        }
    }

    private void startBatchTask() {
        this.plugin.getSchedulerManager().runAsyncRepeating(this::sendBatch, 5L, 5L, TimeUnit.SECONDS);
    }

    public void logPunishment(Punishment punishment) {
        if (!this.enabled) {
            return;
        }
        WebhookEmbed embed = this.createPunishmentEmbed(punishment);
        this.pendingEmbeds.offer(embed);
    }

    public void logUnban(String targetName, String staffName) {
        if (!this.enabled) {
            return;
        }
        WebhookEmbed embed = new WebhookEmbed("\ud83d\udd13 玩家封禁已解除", String.format("**%s** 已被 **%s** 解除封禁", targetName, staffName), 65416, Instant.now());
        this.pendingEmbeds.offer(embed);
    }

    public void logUnmute(String targetName, String staffName) {
        if (!this.enabled) {
            return;
        }
        WebhookEmbed embed = new WebhookEmbed("\ud83d\udd0a 玩家禁言已解除", String.format("**%s** 已被 **%s** 解除禁言", targetName, staffName), 0x88FF00, Instant.now());
        this.pendingEmbeds.offer(embed);
    }

    public void logUnbanIp(String ipAddress, String targetName, String staffName) {
        if (!this.enabled) {
            return;
        }
        WebhookEmbed embed = new WebhookEmbed("\ud83d\udd13 IP 封禁已解除", String.format("IP `%s`（**%s**）已被 **%s** 解除封禁", ipAddress, targetName, staffName), 65416, Instant.now());
        this.pendingEmbeds.offer(embed);
    }

    public void logUnmuteIp(String ipAddress, String targetName, String staffName) {
        if (!this.enabled) {
            return;
        }
        WebhookEmbed embed = new WebhookEmbed("\ud83d\udd0a IP 禁言已解除", String.format("IP `%s`（**%s**）已被 **%s** 解除禁言", ipAddress, targetName, staffName), 0x88FF00, Instant.now());
        this.pendingEmbeds.offer(embed);
    }

    private WebhookEmbed createPunishmentEmbed(Punishment punishment) {
        String title = this.getEmojiForType(punishment.getType()) + " " + punishment.getType().getDisplayName();
        StringBuilder description = new StringBuilder();
        description.append("**玩家:** ").append(punishment.getTargetName()).append("\n");
        description.append("**执行者:** ").append(punishment.getStaffName()).append("\n");
        description.append("**原因:** ").append(punishment.getReason() != null ? punishment.getReason() : "未填写原因").append("\n");
        if (punishment.getTargetIp() != null && !punishment.getTargetIp().isEmpty()) {
            description.append("**IP:** `").append(punishment.getTargetIp()).append("`\n");
        }
        if (punishment.getType().isTemporary() && punishment.getExpiresAt() != null) {
            description.append("**时长:** ").append(TimeUtil.formatRemaining(punishment.getExpiresAt())).append("\n");
            description.append("**到期:** <t:").append(punishment.getExpiresAt().getEpochSecond()).append(":R>");
        } else if (punishment.getType() != PunishmentType.KICK) {
            description.append("**时长:** 永久");
        }
        return new WebhookEmbed(title, description.toString(), this.getColorForType(punishment.getType()), punishment.getCreatedAt());
    }

    private String getEmojiForType(PunishmentType type) {
        return switch (type) {
            default -> throw new MatchException(null, null);
            case PunishmentType.BAN -> "\ud83d\udd28";
            case PunishmentType.TEMPBAN -> "\u23f0";
            case PunishmentType.BANIP -> "\ud83c\udf10";
            case PunishmentType.TEMPBANIP -> "\ud83c\udf0d";
            case PunishmentType.MUTE -> "\ud83d\udd07";
            case PunishmentType.TEMPMUTE -> "\u23f1\ufe0f";
            case PunishmentType.MUTEIP -> "\ud83d\udcf5";
            case PunishmentType.TEMPMUTEIP -> "\ud83d\udcf4";
            case PunishmentType.KICK -> "\ud83d\udc62";
        };
    }

    private int getColorForType(PunishmentType type) {
        return switch (type) {
            default -> throw new MatchException(null, null);
            case PunishmentType.BAN -> 0xFF0000;
            case PunishmentType.TEMPBAN -> 0xFF6600;
            case PunishmentType.BANIP, PunishmentType.TEMPBANIP -> 0xCC0000;
            case PunishmentType.MUTE -> 0xFFFF00;
            case PunishmentType.TEMPMUTE -> 65280;
            case PunishmentType.MUTEIP, PunishmentType.TEMPMUTEIP -> 0xCCCC00;
            case PunishmentType.KICK -> 39423;
        };
    }

    private void sendBatch() {
        WebhookEmbed embed;
        if (!this.enabled || this.pendingEmbeds.isEmpty()) {
            return;
        }
        ArrayList<WebhookEmbed> batch = new ArrayList<WebhookEmbed>(10);
        while ((embed = this.pendingEmbeds.poll()) != null && batch.size() < 10) {
            batch.add(embed);
        }
        if (batch.isEmpty()) {
            return;
        }
        String json = this.buildJsonPayload(batch);
        try {
            this.sendWebhook(json);
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("发送 Discord 通知失败: " + e.getMessage());
        }
    }

    private String buildJsonPayload(List<WebhookEmbed> embeds) {
        StringBuilder json = new StringBuilder();
        json.append("{\"embeds\":[");
        for (int i = 0; i < embeds.size(); ++i) {
            WebhookEmbed embed = embeds.get(i);
            if (i > 0) {
                json.append(",");
            }
            json.append("{");
            json.append("\"title\":\"").append(this.escapeJson(embed.title)).append("\",");
            json.append("\"description\":\"").append(this.escapeJson(embed.description)).append("\",");
            json.append("\"color\":").append(embed.color).append(",");
            json.append("\"timestamp\":\"").append(ISO_FORMATTER.format(embed.timestamp)).append("\"");
            json.append("}");
        }
        json.append("]}");
        return json.toString();
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private void sendWebhook(String json) throws IOException {
        URL url2 = URI.create(this.webhookUrl).toURL();
        HttpURLConnection connection = (HttpURLConnection)url2.openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "SimpMC-Punish/" + this.plugin.getPluginMeta().getVersion());
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            try (OutputStream os = connection.getOutputStream();){
                os.write(json.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            int responseCode = connection.getResponseCode();
            if (responseCode == 429) {
                this.plugin.getLogger().warning("Discord 通知触发限流，请考虑增大批量发送间隔。");
            } else if (responseCode < 200 || responseCode >= 300) {
                this.plugin.getLogger().warning("Discord 通知返回状态码: " + responseCode);
            }
        }
        finally {
            connection.disconnect();
        }
    }

    private record WebhookEmbed(String title, String description, int color, Instant timestamp) {
    }
}


