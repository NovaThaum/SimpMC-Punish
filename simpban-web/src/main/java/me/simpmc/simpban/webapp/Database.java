package me.simpmc.simpban.webapp;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

public final class Database implements AutoCloseable {
    private final HikariDataSource dataSource;
    private final String type;

    public Database(WebConfig webConfig) {
        this.type = webConfig.databaseType();
        this.dataSource = new HikariDataSource(this.type.equals("mysql")
                ? mysqlConfig(webConfig)
                : sqliteConfig(webConfig));
    }

    public Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

    public String type() {
        return this.type;
    }

    @Override
    public void close() {
        this.dataSource.close();
    }

    private static HikariConfig sqliteConfig(WebConfig webConfig) {
        Path sqlitePath = Path.of(webConfig.sqlitePath()).toAbsolutePath().normalize();
        if (Files.isDirectory(sqlitePath) || looksLikeDirectory(sqlitePath)) {
            sqlitePath = sqlitePath.resolve("punishments.db");
        }
        Path parent = sqlitePath.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (Exception e) {
                throw new IllegalStateException("创建 SQLite 目录失败: " + parent, e);
            }
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + sqlitePath);
        config.setPoolName("SimpBan-Web-SQLite");
        config.setMaximumPoolSize(Math.max(1, webConfig.poolSize()));
        config.setMinimumIdle(1);
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("busy_timeout", "5000");
        return config;
    }

    private static boolean looksLikeDirectory(Path path) {
        if (Files.exists(path)) {
            return false;
        }
        Path fileName = path.getFileName();
        if (fileName == null) {
            return false;
        }
        String name = fileName.toString().toLowerCase();
        return !name.endsWith(".db") && !name.endsWith(".sqlite") && !name.endsWith(".sqlite3");
    }

    private static HikariConfig mysqlConfig(WebConfig webConfig) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + webConfig.mysqlHost() + ":" + webConfig.mysqlPort() + "/" + webConfig.mysqlDatabase());
        config.setUsername(webConfig.mysqlUsername());
        config.setPassword(webConfig.mysqlPassword());
        config.setPoolName("SimpBan-Web-MySQL");
        config.setMaximumPoolSize(Math.max(1, webConfig.poolSize()));
        config.setMinimumIdle(1);
        webConfig.mysqlProperties().forEach(config::addDataSourceProperty);
        return config;
    }
}
