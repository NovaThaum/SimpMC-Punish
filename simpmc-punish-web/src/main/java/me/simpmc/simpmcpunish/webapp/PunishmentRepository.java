package me.simpmc.simpmcpunish.webapp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PunishmentRepository {
    private final Database database;

    public PunishmentRepository(Database database) {
        this.database = database;
    }

    public List<PunishmentRecord> recent(int limit) throws SQLException {
        String sql = "SELECT * FROM punishments ORDER BY created_at DESC LIMIT ?";
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, clampLimit(limit));
            return records(statement);
        }
    }

    public List<PunishmentRecord> search(String query, int limit) throws SQLException {
        String sql = """
                SELECT * FROM punishments
                WHERE LOWER(target_name) LIKE LOWER(?)
                   OR target_uuid = ?
                   OR target_ip LIKE ?
                ORDER BY created_at DESC
                LIMIT ?
                """;
        String like = "%" + query + "%";
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, like);
            statement.setString(2, query);
            statement.setString(3, like);
            statement.setInt(4, clampLimit(limit));
            return records(statement);
        }
    }

    public Map<String, Object> stats() throws SQLException {
        String sql = """
                SELECT
                  COUNT(*) AS total,
                  SUM(CASE WHEN active = 1 THEN 1 ELSE 0 END) AS active_count,
                  SUM(CASE WHEN type IN ('BAN', 'TEMPBAN', 'BANIP', 'TEMPBANIP') THEN 1 ELSE 0 END) AS ban_count,
                  SUM(CASE WHEN type IN ('MUTE', 'TEMPMUTE', 'MUTEIP', 'TEMPMUTEIP') THEN 1 ELSE 0 END) AS mute_count,
                  SUM(CASE WHEN type = 'KICK' THEN 1 ELSE 0 END) AS kick_count,
                  MAX(created_at) AS latest_at
                FROM punishments
                """;
        try (Connection connection = this.database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            Map<String, Object> stats = new LinkedHashMap<>();
            if (result.next()) {
                stats.put("total", result.getLong("total"));
                stats.put("active", result.getLong("active_count"));
                stats.put("bans", result.getLong("ban_count"));
                stats.put("mutes", result.getLong("mute_count"));
                stats.put("kicks", result.getLong("kick_count"));
                long latestAt = result.getLong("latest_at");
                stats.put("latestAt", result.wasNull() ? null : latestAt);
            }
            return stats;
        }
    }

    private static List<PunishmentRecord> records(PreparedStatement statement) throws SQLException {
        ArrayList<PunishmentRecord> records = new ArrayList<>();
        try (ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                records.add(PunishmentRecord.from(result));
            }
        }
        return records;
    }

    private static int clampLimit(int limit) {
        return Math.max(1, Math.min(limit, 100));
    }
}
