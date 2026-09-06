package me.simpmc.simpmcpunish.velocity;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class VelocityMessageRenderer {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final Pattern WHITESPACE = Pattern.compile("[\\t ]+");
    private static final DateTimeFormatter VANILLA_BAN_DATE = DateTimeFormatter
            .ofPattern("yyyy-MM-dd 'at' HH:mm:ss z")
            .withZone(ZoneId.systemDefault());
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final VelocityMessages messages;
    private final int maxReasonLength;
    private final boolean vanillaBanComponents;

    public VelocityMessageRenderer(VelocityMessages messages, int maxReasonLength) {
        this(messages, maxReasonLength, true);
    }

    public VelocityMessageRenderer(VelocityMessages messages, int maxReasonLength, boolean vanillaBanComponents) {
        this.messages = Objects.requireNonNull(messages, "messages");
        this.maxReasonLength = maxReasonLength;
        this.vanillaBanComponents = vanillaBanComponents;
    }

    public Component banScreen(PunishmentRecord punishment) {
        String reason = truncate(punishment.reason() == null ? "未填写原因" : punishment.reason());
        if (!this.vanillaBanComponents) {
            String player = punishment.targetName() == null ? "未知玩家" : punishment.targetName();
            String staff = punishment.staffName() == null ? "控制台" : punishment.staffName();
            return this.miniMessage.deserialize(this.messages.banScreen(),
                    Placeholder.unparsed("player", player),
                    Placeholder.unparsed("staff", staff),
                    Placeholder.unparsed("reason", reason),
                    Placeholder.unparsed("expires", formatRemaining(punishment.expiresAt())));
        }
        String keyPrefix = punishment.isIpBan()
                ? "multiplayer.disconnect.banned_ip"
                : "multiplayer.disconnect.banned";
        Component result = Component.translatable(keyPrefix + ".reason", Component.text(reason));
        if (punishment.expiresAt() != null) {
            result = result.append(Component.translatable(
                    keyPrefix + ".expiration",
                    Component.text(VANILLA_BAN_DATE.format(punishment.expiresAt()))));
        }
        return result;
    }

    public Component databaseError() {
        return this.miniMessage.deserialize(this.messages.databaseError());
    }

    public Component fallbackKick() {
        return this.miniMessage.deserialize(this.messages.kickFallback());
    }

    public String auditReason(Component reason) {
        String plain = reason == null ? "无理由" : PLAIN.serialize(reason);
        String oneLine = plain.replaceAll("\\R+", " | ");
        return truncate(WHITESPACE.matcher(oneLine).replaceAll(" ").trim());
    }

    public String auditReason(String reason) {
        return auditReason(reason == null ? Component.empty() : Component.text(reason));
    }

    public String truncate(String value) {
        if (value.length() <= this.maxReasonLength) {
            return value;
        }
        return value.substring(0, this.maxReasonLength - 1) + "…";
    }

    private static String formatRemaining(Instant expiresAt) {
        if (expiresAt == null) {
            return "永久";
        }
        long seconds = Math.max(0L, Duration.between(Instant.now(), expiresAt).toSeconds());
        if (seconds < 60L) {
            return seconds + "秒";
        }
        long minutes = seconds / 60L;
        if (minutes < 60L) {
            return minutes + "分钟";
        }
        long hours = minutes / 60L;
        if (hours < 24L) {
            return hours + "小时";
        }
        return (hours / 24L) + "天";
    }

}
