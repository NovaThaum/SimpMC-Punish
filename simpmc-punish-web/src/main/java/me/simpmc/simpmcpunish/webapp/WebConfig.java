package me.simpmc.simpmcpunish.webapp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public record WebConfig(
        String host,
        int port,
        String serverName,
        String databaseType,
        String sqlitePath,
        String mysqlHost,
        int mysqlPort,
        String mysqlDatabase,
        String mysqlUsername,
        String mysqlPassword,
        Map<String, String> mysqlProperties,
        int poolSize
) {
    public static WebConfig load(String[] args) throws IOException {
        Builder builder = new Builder();
        Path configPath = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--help", "-h" -> {
                    printHelp();
                    System.exit(0);
                }
                case "--config" -> configPath = Path.of(requireValue(args, ++i, arg));
                case "--sqlite" -> {
                    builder.databaseType = "sqlite";
                    builder.sqlitePath = requireValue(args, ++i, arg);
                }
                case "--mysql" -> builder.databaseType = "mysql";
                case "--host" -> builder.host = requireValue(args, ++i, arg);
                case "--port" -> builder.port = Integer.parseInt(requireValue(args, ++i, arg));
                case "--name" -> builder.serverName = requireValue(args, ++i, arg);
                case "--mysql-host" -> builder.mysqlHost = requireValue(args, ++i, arg);
                case "--mysql-port" -> builder.mysqlPort = Integer.parseInt(requireValue(args, ++i, arg));
                case "--mysql-database" -> builder.mysqlDatabase = requireValue(args, ++i, arg);
                case "--mysql-user" -> builder.mysqlUsername = requireValue(args, ++i, arg);
                case "--mysql-password" -> builder.mysqlPassword = requireValue(args, ++i, arg);
                default -> throw new IllegalArgumentException("未知参数: " + arg);
            }
        }

        if (configPath == null && Files.exists(Path.of("config.yml"))) {
            configPath = Path.of("config.yml");
        }
        if (configPath != null) {
            builder.applyYaml(configPath);
            // 命令行参数优先级高于配置文件。
            return WebConfig.load(args, builder);
        }
        return builder.build();
    }

    private static WebConfig load(String[] args, Builder builder) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--help", "-h" -> {
                    printHelp();
                    System.exit(0);
                }
                case "--config" -> i++;
                case "--sqlite" -> {
                    builder.databaseType = "sqlite";
                    builder.sqlitePath = requireValue(args, ++i, arg);
                }
                case "--mysql" -> builder.databaseType = "mysql";
                case "--host" -> builder.host = requireValue(args, ++i, arg);
                case "--port" -> builder.port = Integer.parseInt(requireValue(args, ++i, arg));
                case "--name" -> builder.serverName = requireValue(args, ++i, arg);
                case "--mysql-host" -> builder.mysqlHost = requireValue(args, ++i, arg);
                case "--mysql-port" -> builder.mysqlPort = Integer.parseInt(requireValue(args, ++i, arg));
                case "--mysql-database" -> builder.mysqlDatabase = requireValue(args, ++i, arg);
                case "--mysql-user" -> builder.mysqlUsername = requireValue(args, ++i, arg);
                case "--mysql-password" -> builder.mysqlPassword = requireValue(args, ++i, arg);
                default -> throw new IllegalArgumentException("未知参数: " + arg);
            }
        }
        return builder.build();
    }

    private static String requireValue(String[] args, int index, String arg) {
        if (index >= args.length) {
            throw new IllegalArgumentException(arg + " 缺少参数值");
        }
        return args[index];
    }

    private static void printHelp() {
        System.out.println("""
                SimpMC-Punish 网页服务 2.0.0

                用法:
                  java -jar simpmc-punish-web-2.0.0.jar --config config.yml
                  java -jar simpmc-punish-web-2.0.0.jar --sqlite <SQLite文件或目录>
                  java -jar simpmc-punish-web-2.0.0.jar --mysql --mysql-host localhost --mysql-database simpmc_punish

                参数:
                  --host <地址>              监听地址，默认 0.0.0.0
                  --port <端口>              监听端口，默认 8080
                  --name <名称>              页面标题
                  --config <文件>            配置文件
                  --sqlite <文件或目录>      使用指定 SQLite 文件；如果是目录则使用 punishments.db
                  --mysql                    使用 MySQL
                  --mysql-host <地址>        MySQL 地址
                  --mysql-port <端口>        MySQL 端口
                  --mysql-database <库名>    MySQL 数据库
                  --mysql-user <用户名>      MySQL 用户名
                  --mysql-password <密码>    MySQL 密码
                """);
    }

    private static final class Builder {
        private String host = "0.0.0.0";
        private int port = 8080;
        private String serverName = "SimpMC-Punish 管理面板";
        private String databaseType = "sqlite";
        private String sqlitePath = "punishments.db";
        private String mysqlHost = "localhost";
        private int mysqlPort = 3306;
        private String mysqlDatabase = "simpmc_punish";
        private String mysqlUsername = "root";
        private String mysqlPassword = "password";
        private Map<String, String> mysqlProperties = new HashMap<>();
        private int poolSize = 5;

        @SuppressWarnings("unchecked")
        private void applyYaml(Path path) throws IOException {
            Yaml yaml = new Yaml();
            Map<String, Object> root;
            try (InputStream input = Files.newInputStream(path)) {
                root = yaml.load(input);
            }
            if (root == null) {
                return;
            }
            Map<String, Object> server = map(root.get("server"));
            this.host = string(server.get("host"), this.host);
            this.port = integer(server.get("port"), this.port);
            this.serverName = string(server.get("name"), this.serverName);

            Map<String, Object> database = map(root.get("database"));
            this.databaseType = string(database.get("type"), this.databaseType).toLowerCase();
            this.poolSize = integer(database.get("pool-size"), this.poolSize);

            Map<String, Object> sqlite = map(database.get("sqlite"));
            this.sqlitePath = string(sqlite.get("path"), string(sqlite.get("file"), this.sqlitePath));

            Map<String, Object> mysql = map(database.get("mysql"));
            this.mysqlHost = string(mysql.get("host"), this.mysqlHost);
            this.mysqlPort = integer(mysql.get("port"), this.mysqlPort);
            this.mysqlDatabase = string(mysql.get("database"), this.mysqlDatabase);
            this.mysqlUsername = string(mysql.get("username"), this.mysqlUsername);
            this.mysqlPassword = string(mysql.get("password"), this.mysqlPassword);
            Map<String, Object> properties = map(mysql.get("properties"));
            properties.forEach((key, value) -> this.mysqlProperties.put(key, String.valueOf(value)));
        }

        private WebConfig build() {
            if (!this.databaseType.equals("sqlite") && !this.databaseType.equals("mysql")) {
                throw new IllegalArgumentException("数据库类型只能是 sqlite 或 mysql");
            }
            return new WebConfig(this.host, this.port, this.serverName, this.databaseType,
                    this.sqlitePath, this.mysqlHost, this.mysqlPort, this.mysqlDatabase,
                    this.mysqlUsername, this.mysqlPassword, Map.copyOf(this.mysqlProperties), this.poolSize);
        }

        private static Map<String, Object> map(Object value) {
            if (value instanceof Map<?, ?> source) {
                Map<String, Object> result = new HashMap<>();
                source.forEach((key, item) -> result.put(String.valueOf(key), item));
                return result;
            }
            return Map.of();
        }

        private static String string(Object value, String fallback) {
            return value == null ? fallback : String.valueOf(value);
        }

        private static int integer(Object value, int fallback) {
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value != null) {
                return Integer.parseInt(String.valueOf(value));
            }
            return fallback;
        }
    }
}
