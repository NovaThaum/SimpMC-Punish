package me.simpmc.simpmcpunish.database;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import me.simpmc.simpmcpunish.SimpMCPunish;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

public class DatabaseManager {
    private final SimpMCPunish plugin;
    private HikariDataSource dataSource;
    private String databaseType;

    public DatabaseManager(SimpMCPunish plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        this.databaseType = this.plugin.getConfig().getString("database.type", "sqlite").toLowerCase();
        this.plugin.getLogger().info("正在初始化数据库类型: " + this.databaseType);
        try {
            this.dataSource = new HikariDataSource(switch (this.databaseType) {
                case "mysql" -> this.initializeMySQL();
                case "sqlite" -> this.initializeSQLite();
                default -> {
                    this.plugin.getLogger().warning("未知数据库类型 '" + this.databaseType + "'，已回退到 SQLite");
                    this.databaseType = "sqlite";
                    yield this.initializeSQLite();
                }
            });
            this.plugin.getLogger().info("数据库连接池已初始化（" + this.databaseType.toUpperCase() + "）");
            this.initializeTables();
            return true;
        }
        catch (Exception e) {
            this.dataSource = null;
            this.plugin.getLogger().log(Level.SEVERE, "初始化数据库连接池失败", e);
            return false;
        }
    }

    private HikariConfig initializeSQLite() {
        String dbFileName = this.plugin.getConfig().getString("database.sqlite.file", "punishments.db");
        File dbFile = new File(this.plugin.getDataFolder(), dbFileName);
        try {
            Class.forName("org.sqlite.JDBC");
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("未找到 SQLite JDBC 驱动！", e);
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setPoolName("SimpMC-Punish-SQLite-Pool");
        config.setMaximumPoolSize(this.plugin.getConfig().getInt("database.pool-size", 5));
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000L);
        config.setIdleTimeout(600000L);
        config.setMaxLifetime(1800000L);
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("busy_timeout", "5000");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("cache_size", "-20000");
        config.addDataSourceProperty("foreign_keys", "ON");
        return config;
    }

    private HikariConfig initializeMySQL() {
        String host2 = this.plugin.getConfig().getString("database.mysql.host", "localhost");
        int port = this.plugin.getConfig().getInt("database.mysql.port", 3306);
        String database = this.plugin.getConfig().getString("database.mysql.database", "simpmc-punish");
        String username = this.plugin.getConfig().getString("database.mysql.username", "root");
        String password = this.plugin.getConfig().getString("database.mysql.password", "password");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("未找到 MySQL JDBC 驱动！", e);
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host2 + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        config.setPoolName("SimpMC-Punish-MySQL-Pool");
        config.setMaximumPoolSize(this.plugin.getConfig().getInt("database.pool-size", 10));
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000L);
        config.setIdleTimeout(600000L);
        config.setMaxLifetime(1800000L);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        ConfigurationSection properties = this.plugin.getConfig().getConfigurationSection("database.mysql.properties");
        if (properties != null) {
            for (String key : properties.getKeys(false)) {
                config.addDataSourceProperty(key, properties.get(key).toString());
            }
        }
        return config;
    }

    private void initializeTables() {
        String autoIncrement = this.databaseType.equals("mysql") ? "AUTO_INCREMENT" : "AUTOINCREMENT";
        String textType = this.databaseType.equals("mysql") ? "VARCHAR(255)" : "TEXT";
        String bigintType = this.databaseType.equals("mysql") ? "BIGINT" : "INTEGER";
        String createPunishmentsTable = String.format("    CREATE TABLE IF NOT EXISTS punishments (\n        id INTEGER PRIMARY KEY %s,\n        target_uuid %s NOT NULL,\n        target_name %s NOT NULL,\n        target_ip %s,\n        staff_uuid %s,\n        staff_name %s NOT NULL,\n        type %s NOT NULL,\n        reason TEXT,\n        created_at %s NOT NULL,\n        expires_at %s,\n        active TINYINT NOT NULL DEFAULT 1,\n        removed_by_uuid %s,\n        removed_by_name %s,\n        removed_at %s,\n        remove_reason TEXT\n    )\n", autoIncrement, textType, textType, textType, textType, textType, textType, bigintType, bigintType, textType, textType, bigintType);
        String createPlayerIpsTable = String.format("    CREATE TABLE IF NOT EXISTS player_ips (\n        uuid %s PRIMARY KEY,\n        name %s NOT NULL,\n        ip_address %s NOT NULL,\n        last_seen %s NOT NULL\n    )\n", textType, textType, textType, bigintType);
        ((CompletableFuture)((CompletableFuture)this.executeAsync(createPunishmentsTable).thenCompose(v -> this.executeAsync(createPlayerIpsTable))).thenAccept(v -> {
            this.createIndexSafe("idx_punishments_target_uuid", "punishments", "target_uuid");
            this.createIndexSafe("idx_punishments_active", "punishments", "active, type");
            this.createIndexSafe("idx_punishments_expires", "punishments", "expires_at");
            this.createIndexSafe("idx_punishments_target_ip", "punishments", "target_ip");
            this.createIndexSafe("idx_punishments_banlist_page", "punishments", "active, created_at, id, type, expires_at");
        })).exceptionally(e -> {
            this.plugin.getLogger().log(Level.SEVERE, "初始化数据库表失败", (Throwable)e);
            return null;
        });
    }

    private void createIndexSafe(String indexName, String tableName, String columns) {
        String sql = this.databaseType.equals("mysql") ? String.format("CREATE INDEX %s ON %s(%s)", indexName, tableName, columns) : String.format("CREATE INDEX IF NOT EXISTS %s ON %s(%s)", indexName, tableName, columns);
        try (Connection conn = this.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);){
            stmt.execute();
        }
        catch (SQLException e) {
            if (this.databaseType.equals("mysql") && e.getMessage() != null && e.getMessage().contains("Duplicate key name")) {
                return;
            }
            this.plugin.getLogger().log(Level.WARNING, "创建索引 " + indexName + " 失败: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        if (this.dataSource == null) {
            throw new SQLException("数据库尚未初始化");
        }
        return this.dataSource.getConnection();
    }

    public CompletableFuture<Void> executeAsync(String sql) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = this.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);){
                stmt.execute();
            }
            catch (SQLException e) {
                throw new RuntimeException("执行 SQL 失败", e);
            }
        });
    }

    public boolean isConnected() {
        return this.dataSource != null && !this.dataSource.isClosed();
    }

    public String getDatabaseType() {
        return this.databaseType;
    }

    public void shutdown() {
        if (this.dataSource != null && !this.dataSource.isClosed()) {
            this.dataSource.close();
            this.plugin.getLogger().info("数据库连接池已关闭。");
        }
    }
}


