package me.simpmc.simpmcpunish.velocity;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;

public final class VelocityDatabase implements AutoCloseable {
    private static final String BAN_TYPES = "('BAN', 'TEMPBAN', 'BANIP', 'TEMPBANIP')";
    private final HikariDataSource dataSource;
    private final Logger logger;

    public VelocityDatabase(VelocityConfiguration configuration, Logger logger) {
        this.logger = logger;
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl("jdbc:mysql://" + configuration.databaseHost() + ":"
                + configuration.databasePort() + "/" + configuration.databaseName());
        hikari.setUsername(configuration.databaseUsername());
        hikari.setPassword(configuration.databasePassword());
        hikari.setMaximumPoolSize(configuration.databasePoolSize());
        hikari.setMinimumIdle(1);
        hikari.setConnectionTimeout(configuration.databaseConnectionTimeoutMs());
        hikari.setInitializationFailTimeout(-1L);
        hikari.setReadOnly(true);
        hikari.setPoolName("SimpMC-Punish-Velocity-ReadOnly");
        hikari.addDataSourceProperty("useSSL", "false");
        hikari.addDataSourceProperty("allowPublicKeyRetrieval", "true");
        hikari.addDataSourceProperty("serverTimezone", "UTC");
        hikari.addDataSourceProperty("characterEncoding", "utf8");
        configuration.databaseProperties().forEach(hikari::addDataSourceProperty);
        this.dataSource = new HikariDataSource(hikari);
    }

    public void validateSchema() throws SQLException {
        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, target_uuid, target_name, target_ip, staff_name, type, reason, created_at, expires_at "
                             + "FROM punishments WHERE 1 = 0")) {
            statement.executeQuery();
        }
    }

    public Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

    public BanSnapshot loadActiveBans() throws SQLException {
        String sql = "SELECT id, target_uuid, target_name, target_ip, staff_name, type, reason, created_at, expires_at "
                + "FROM punishments WHERE active = 1 AND type IN " + BAN_TYPES
                + " AND (expires_at IS NULL OR expires_at > ?) ORDER BY created_at DESC, id DESC";
        List<PunishmentRecord> records = new ArrayList<>();
        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, Instant.now().toEpochMilli());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    records.add(read(result));
                }
            }
        }
        return new BanSnapshot(records);
    }

    public Optional<PunishmentRecord> findActiveBan(UUID uuid, String ipAddress) throws SQLException {
        String sql = "SELECT id, target_uuid, target_name, target_ip, staff_name, type, reason, created_at, expires_at "
                + "FROM punishments WHERE active = 1 AND ("
                + "(type IN ('BAN', 'TEMPBAN') AND target_uuid = ?) OR "
                + "(type IN ('BANIP', 'TEMPBANIP') AND target_ip = ?)) "
                + "AND (expires_at IS NULL OR expires_at > ?) ORDER BY created_at DESC, id DESC LIMIT 1";
        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (uuid == null) {
                statement.setNull(1, Types.VARCHAR);
            } else {
                statement.setString(1, uuid.toString());
            }
            statement.setString(2, PunishmentRecord.normalizeIp(ipAddress));
            statement.setLong(3, Instant.now().toEpochMilli());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(read(result)) : Optional.empty();
            }
        }
    }

    private PunishmentRecord read(ResultSet result) throws SQLException {
        String uuidValue = result.getString("target_uuid");
        UUID uuid = null;
        if (uuidValue != null) {
            try {
                uuid = UUID.fromString(uuidValue);
            } catch (IllegalArgumentException exception) {
                this.logger.warn("处罚记录 {} 的 target_uuid 无效: {}", result.getInt("id"), uuidValue);
            }
        }
        long createdAt = result.getLong("created_at");
        long expiresAt = result.getLong("expires_at");
        return new PunishmentRecord(
                result.getInt("id"),
                uuid,
                result.getString("target_name"),
                PunishmentRecord.normalizeIp(result.getString("target_ip")),
                result.getString("staff_name"),
                result.getString("type"),
                result.getString("reason"),
                Instant.ofEpochMilli(createdAt),
                result.wasNull() ? null : Instant.ofEpochMilli(expiresAt));
    }

    @Override
    public void close() {
        this.dataSource.close();
    }
}
