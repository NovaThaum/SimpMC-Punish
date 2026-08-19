package me.simpmc.simpmcpunish.velocity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

public record VelocityPunishmentRecord(
        int id,
        String targetUuid,
        String targetName,
        String targetIp,
        String staffUuid,
        String staffName,
        String type,
        String typeName,
        String reason,
        long createdAt,
        Long expiresAt,
        boolean active,
        boolean expired,
        String removedByName,
        Long removedAt,
        String removeReason) {

    public static VelocityPunishmentRecord from(ResultSet result) throws SQLException {
        Long expiresAt = nullableLong(result, "expires_at");
        String type = result.getString("type");
        return new VelocityPunishmentRecord(
                result.getInt("id"),
                result.getString("target_uuid"),
                result.getString("target_name"),
                result.getString("target_ip"),
                result.getString("staff_uuid"),
                result.getString("staff_name"),
                type,
                typeName(type),
                result.getString("reason"),
                result.getLong("created_at"),
                expiresAt,
                result.getInt("active") == 1,
                expiresAt != null && expiresAt <= Instant.now().toEpochMilli(),
                result.getString("removed_by_name"),
                nullableLong(result, "removed_at"),
                result.getString("remove_reason"));
    }

    private static Long nullableLong(ResultSet result, String column) throws SQLException {
        long value = result.getLong(column);
        return result.wasNull() ? null : value;
    }

    private static String typeName(String type) {
        return switch (type) {
            case "BAN" -> "永久封禁";
            case "TEMPBAN" -> "临时封禁";
            case "BANIP" -> "IP 封禁";
            case "TEMPBANIP" -> "临时 IP 封禁";
            case "MUTE" -> "永久禁言";
            case "TEMPMUTE" -> "临时禁言";
            case "MUTEIP" -> "IP 禁言";
            case "TEMPMUTEIP" -> "临时 IP 禁言";
            case "KICK" -> "踢出";
            default -> type == null ? "未知" : type;
        };
    }
}
