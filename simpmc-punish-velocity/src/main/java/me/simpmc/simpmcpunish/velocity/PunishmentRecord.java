package me.simpmc.simpmcpunish.velocity;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record PunishmentRecord(
        int id,
        UUID targetUuid,
        String targetName,
        String targetIp,
        String staffName,
        String type,
        String reason,
        Instant createdAt,
        Instant expiresAt) {

    public boolean isPlayerBan() {
        return "BAN".equalsIgnoreCase(type) || "TEMPBAN".equalsIgnoreCase(type);
    }

    public boolean isIpBan() {
        return "BANIP".equalsIgnoreCase(type) || "TEMPBANIP".equalsIgnoreCase(type);
    }

    public boolean matches(UUID uuid, String ipAddress) {
        if (isPlayerBan() && targetUuid != null && targetUuid.equals(uuid)) {
            return true;
        }
        return isIpBan() && targetIp != null && targetIp.equals(normalizeIp(ipAddress));
    }

    public static String normalizeIp(String ipAddress) {
        if (ipAddress == null) {
            return null;
        }
        String value = ipAddress.toLowerCase(Locale.ROOT);
        return value.startsWith("::ffff:") ? value.substring(7) : value;
    }
}
