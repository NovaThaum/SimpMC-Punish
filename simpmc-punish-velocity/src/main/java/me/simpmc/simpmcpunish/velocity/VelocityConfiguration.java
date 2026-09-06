package me.simpmc.simpmcpunish.velocity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public record VelocityConfiguration(
        String databaseHost,
        int databasePort,
        String databaseName,
        String databaseUsername,
        String databasePassword,
        int databasePoolSize,
        long databaseConnectionTimeoutMs,
        long syncIntervalMs,
        boolean disconnectBackendKicks,
        boolean denyLoginOnDatabaseError,
        boolean vanillaBanComponents,
        boolean logBackendKicks,
        boolean logDeniedLoginAttempts,
        long deniedLoginRateLimitSeconds,
        long databaseErrorRateLimitSeconds,
        int maxReasonLength,
        Map<String, String> databaseProperties,
        boolean webEnabled,
        String webHost,
        int webPort,
        String webServerName) {

    public static VelocityConfiguration load(Path dataDirectory, Logger logger) throws IOException {
        Files.createDirectories(dataDirectory);
        Path file = dataDirectory.resolve("config.yml");
        if (!Files.exists(file)) {
            try (InputStream input = VelocityConfiguration.class.getResourceAsStream("/config.yml")) {
                if (input == null) {
                    throw new IOException("缺少内置 config.yml");
                }
                Files.copy(input, file);
            }
        }
        Map<String, Object> root = loadYaml(file);
        Map<String, Object> database = map(root, "database");
        Map<String, Object> sync = map(root, "sync");
        Map<String, Object> behavior = map(root, "behavior");
        Map<String, Object> logging = map(root, "logging");
        Map<String, Object> web = map(root, "web");
        Map<String, Object> rawProperties = map(database, "properties");
        Map<String, String> databaseProperties = new LinkedHashMap<>();
        rawProperties.forEach((key, value) -> databaseProperties.put(key, String.valueOf(value)));
        VelocityConfiguration result = new VelocityConfiguration(
                string(database, "host", "localhost"),
                integer(database, "port", 3306),
                string(database, "database", "simpmc_punish"),
                string(database, "username", "root"),
                string(database, "password", "password"),
                Math.max(1, integer(database, "pool-size", 4)),
                Math.max(1000L, number(database, "connection-timeout-ms", 5000L)),
                Math.max(250L, number(sync, "interval-ms", 500L)),
                bool(behavior, "disconnect-backend-kicks", true),
                bool(behavior, "deny-login-on-database-error", false),
                bool(behavior, "vanilla-ban-components", false),
                bool(logging, "backend-kicks", true),
                bool(logging, "denied-login-attempts", false),
                Math.max(1L, number(logging, "denied-login-rate-limit-seconds", 30L)),
                Math.max(1L, number(logging, "database-error-rate-limit-seconds", 30L)),
                Math.max(40, integer(logging, "max-reason-length", 240)),
                Map.copyOf(databaseProperties),
                bool(web, "enabled", true),
                string(web, "host", "127.0.0.1"),
                Math.min(65535, Math.max(1, integer(web, "port", 8080))),
                string(web, "name", "SimpMC-Punish 管理面板"));
        logger.info("已加载 Velocity 配置，数据库: {}:{}/{}，同步周期: {}ms",
                result.databaseHost, result.databasePort, result.databaseName, result.syncIntervalMs);
        return result;
    }

    private static Map<String, Object> loadYaml(Path file) throws IOException {
        LoaderOptions options = new LoaderOptions();
        Yaml yaml = new Yaml(new SafeConstructor(options));
        try (InputStream input = Files.newInputStream(file)) {
            Object loaded = yaml.load(input);
            if (!(loaded instanceof Map<?, ?> map)) {
                return Collections.emptyMap();
            }
            return normalizeMap(map);
        }
    }

    private static Map<String, Object> normalizeMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null) {
                result.put(String.valueOf(key), value);
            }
        });
        return result;
    }

    private static Map<String, Object> map(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value instanceof Map<?, ?> nested ? normalizeMap(nested) : Collections.emptyMap();
    }

    private static String string(Map<String, Object> source, String key, String fallback) {
        Object value = source.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static int integer(Map<String, Object> source, String key, int fallback) {
        Object value = source.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static long number(Map<String, Object> source, String key, long fallback) {
        Object value = source.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? fallback : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static boolean bool(Map<String, Object> source, String key, boolean fallback) {
        Object value = source.get(key);
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }
}
