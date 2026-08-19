package me.simpmc.simpmcpunish.velocity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;

public final class EmbeddedWebServer implements AutoCloseable {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private final VelocityConfiguration configuration;
    private final VelocityPunishmentRepository repository;
    private Javalin app;

    public EmbeddedWebServer(VelocityConfiguration configuration, VelocityPunishmentRepository repository) {
        this.configuration = configuration;
        this.repository = repository;
    }

    public void start() {
        this.app = Javalin.create(javalin -> {
            javalin.showJavalinBanner = false;
            javalin.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
                staticFiles.location = Location.CLASSPATH;
                staticFiles.precompress = false;
            });
        });
        this.app.get("/api/config", context -> json(context, Map.of(
                "name", this.configuration.webServerName(),
                "database", "mysql",
                "startedAt", Instant.now().toEpochMilli())));
        this.app.get("/api/health", context -> json(context, Map.of("ok", true, "database", "mysql")));
        this.app.get("/api/stats", context -> json(context, this.repository.stats()));
        this.app.get("/api/punishments/recent", context -> json(context,
                Map.of("punishments", this.repository.recent(intQuery(context, "limit", 30)))));
        this.app.get("/api/punishments/search", context -> {
            String query = context.queryParam("query");
            if (query == null || query.isBlank()) {
                context.status(400);
                json(context, Map.of("error", true, "message", "请输入玩家名、UUID 或 IP。"));
                return;
            }
            json(context, Map.of("punishments", this.repository.search(query.trim(), intQuery(context, "limit", 50))));
        });
        this.app.exception(SQLException.class, (error, context) -> {
            context.status(500);
            json(context, Map.of("error", true, "message", "读取数据库失败。"));
        });
        this.app.exception(Exception.class, (error, context) -> {
            context.status(500);
            json(context, Map.of("error", true, "message", "服务暂时不可用。"));
        });
        this.app.start(this.configuration.webHost(), this.configuration.webPort());
    }

    @Override
    public void close() {
        if (this.app != null) {
            this.app.stop();
            this.app = null;
        }
    }

    private static void json(Context context, Object value) {
        context.contentType("application/json; charset=utf-8");
        context.result(GSON.toJson(value));
    }

    private static int intQuery(Context context, String name, int fallback) {
        String value = context.queryParam(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
