package me.simpmc.simpban.webapp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;

public final class SimpBanWebApplication {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private SimpBanWebApplication() {
    }

    public static void main(String[] args) throws Exception {
        configureLogging();
        WebConfig config = WebConfig.load(args);
        Database database = new Database(config);
        PunishmentRepository repository = new PunishmentRepository(database);

        Javalin app = Javalin.create(javalinConfig -> {
            javalinConfig.showJavalinBanner = false;
            javalinConfig.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
                staticFiles.location = Location.CLASSPATH;
                staticFiles.precompress = false;
            });
        });

        app.get("/api/config", ctx -> json(ctx, Map.of(
                "name", config.serverName(),
                "database", config.databaseType(),
                "startedAt", Instant.now().toEpochMilli()
        )));
        app.get("/api/health", ctx -> json(ctx, Map.of("ok", true, "database", database.type())));
        app.get("/api/stats", ctx -> json(ctx, repository.stats()));
        app.get("/api/punishments/recent", ctx -> json(ctx, Map.of("punishments", repository.recent(intQuery(ctx, "limit", 30)))));
        app.get("/api/punishments/search", ctx -> {
            String query = ctx.queryParam("query");
            if (query == null || query.isBlank()) {
                ctx.status(400);
                json(ctx, Map.of("error", true, "message", "请输入玩家名、UUID 或 IP。"));
                return;
            }
            json(ctx, Map.of("punishments", repository.search(query.trim(), intQuery(ctx, "limit", 50))));
        });

        app.exception(SQLException.class, (error, ctx) -> {
            ctx.status(500);
            json(ctx, Map.of("error", true, "message", "读取数据库失败: " + error.getMessage()));
        });
        app.exception(Exception.class, (error, ctx) -> {
            ctx.status(500);
            json(ctx, Map.of("error", true, "message", "服务异常: " + error.getMessage()));
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            database.close();
        }));

        app.start(config.host(), config.port());
        System.out.println("SimpBan 网页服务已启动: http://" + displayHost(config.host()) + ":" + config.port());
        System.out.println("数据库类型: " + config.databaseType());
    }

    private static void configureLogging() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel",
                System.getProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn"));
        System.setProperty("org.slf4j.simpleLogger.log.io.javalin",
                System.getProperty("org.slf4j.simpleLogger.log.io.javalin", "warn"));
        System.setProperty("org.slf4j.simpleLogger.log.org.eclipse.jetty",
                System.getProperty("org.slf4j.simpleLogger.log.org.eclipse.jetty", "warn"));
        System.setProperty("org.slf4j.simpleLogger.log.com.zaxxer.hikari",
                System.getProperty("org.slf4j.simpleLogger.log.com.zaxxer.hikari", "warn"));
    }

    private static void json(Context ctx, Object value) {
        ctx.contentType("application/json; charset=utf-8");
        ctx.result(GSON.toJson(value));
    }

    private static int intQuery(Context ctx, String name, int fallback) {
        String value = ctx.queryParam(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String displayHost(String host) {
        return host.equals("0.0.0.0") ? "localhost" : host;
    }
}
