package me.simpmc.simpmcpunish.webapp;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

public record PunishmentRecord(
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
        String removeReason
) {
    public static PunishmentRecord from(ResultSet rs) throws SQLException {
        Long expiresAt = nullableLong(rs, "expires_at");
        long now = Instant.now().toEpochMilli();
        String type = rs.getString("type");
        return new PunishmentRecord(
                rs.getInt("id"),
                rs.getString("target_uuid"),
                rs.getString("target_name"),
                rs.getString("target_ip"),
                rs.getString("staff_uuid"),
                rs.getString("staff_name"),
                type,
                typeName(type),
                rs.getString("reason"),
                rs.getLong("created_at"),
                expiresAt,
                rs.getInt("active") == 1,
                expiresAt != null && expiresAt <= now,
                rs.getString("removed_by_name"),
                nullableLong(rs, "removed_at"),
                rs.getString("remove_reason")
        );
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
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
            default -> type;
        };
    }
}
