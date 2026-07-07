/*
 * Decompiled with CFR 0.152.
 */
package me.simpfun.simpban.web;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import me.simpfun.simpban.SimpBan;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import me.simpfun.simpban.web.WebAPIController;

public class WebServer {
    private final SimpBan plugin;
    private final Gson gson;
    private final ExecutorService webExecutor;
    private Javalin app;
    private WebAPIController apiController;

    public WebServer(SimpBan plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();
        this.webExecutor = Executors.newFixedThreadPool(8, r -> {
            Thread t2 = new Thread(r, "SimpBan-Web-Worker");
            t2.setDaemon(true);
            return t2;
        });
    }

    public ExecutorService getWebExecutor() {
        return this.webExecutor;
    }

    public void start() {
        if (!this.plugin.getConfig().getBoolean("web-dashboard.enabled", false)) {
            this.plugin.getLogger().info("Web dashboard is disabled in config.");
            return;
        }
        String host2 = this.plugin.getConfig().getString("web-dashboard.host", "0.0.0.0");
        int port = this.plugin.getConfig().getInt("web-dashboard.port", 8080);
        try {
            this.extractWebFiles();
            this.apiController = new WebAPIController(this.plugin, this.gson, this.webExecutor);
            this.app = Javalin.create(config -> {
                config.showJavalinBanner = false;
                File webFolder = new File(this.plugin.getDataFolder(), "web");
                config.staticFiles.add(staticFiles -> {
                    staticFiles.hostedPath = "/";
                    staticFiles.directory = webFolder.getAbsolutePath();
                    staticFiles.location = Location.EXTERNAL;
                });
            });
            this.app.before("/api/*", ctx -> {
                ctx.header("Access-Control-Allow-Origin", "*");
                ctx.header("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
                ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
            });
            this.app.options("/api/*", ctx -> ctx.status(200));
            this.registerAPIRoutes();
            this.app.start(host2, port);
            this.plugin.getLogger().info("============================================");
            this.plugin.getLogger().info("Web Dashboard started successfully!");
            this.plugin.getLogger().info("Access at: http://" + (host2.equals("0.0.0.0") ? "localhost" : host2) + ":" + port);
            this.plugin.getLogger().info("============================================");
        }
        catch (Exception e) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to start web dashboard", e);
        }
    }

    private void registerAPIRoutes() {
        this.app.get("/api/test", this.apiController::testEndpoint);
        this.app.get("/api/config", this.apiController::getConfig);
        this.app.get("/api/lookup/{username}", this.apiController::lookupPlayer);
        this.app.get("/api/stats", this.apiController::getStats);
        this.app.get("/api/recent", this.apiController::getRecentPunishments);
        this.app.before("/api/admin/*", this.apiController::authenticateAdmin);
        this.app.get("/api/admin/punishments", this.apiController::getAllActivePunishments);
        this.app.post("/api/admin/punish", this.apiController::createPunishment);
        this.app.post("/api/admin/unpunish", this.apiController::removePunishment);
    }

    private void extractWebFiles() {
        String[] files;
        File webFolder = new File(this.plugin.getDataFolder(), "web");
        if (!webFolder.exists()) {
            webFolder.mkdirs();
        }
        for (String fileName : files = new String[]{"index.html", "admin.html", "styles.css", "app.js"}) {
            File targetFile = new File(webFolder, fileName);
            try (InputStream in = this.plugin.getResource("web/" + fileName);){
                if (in != null) {
                    try (FileOutputStream out = new FileOutputStream(targetFile);){
                        int length;
                        byte[] buffer = new byte[1024];
                        while ((length = in.read(buffer)) > 0) {
                            ((OutputStream)out).write(buffer, 0, length);
                        }
                        continue;
                    }
                }
                this.plugin.getLogger().warning("Missing web resource: " + fileName);
            }
            catch (IOException e) {
                this.plugin.getLogger().log(Level.WARNING, "Failed to extract web file: " + fileName, e);
            }
        }
    }

    public void stop() {
        if (this.app != null) {
            try {
                this.app.stop();
                this.plugin.getLogger().info("Web dashboard stopped.");
            }
            catch (Exception e) {
                this.plugin.getLogger().log(Level.WARNING, "Error stopping web dashboard", e);
            }
        }
        if (this.webExecutor != null && !this.webExecutor.isShutdown()) {
            this.webExecutor.shutdown();
            try {
                if (!this.webExecutor.awaitTermination(5L, TimeUnit.SECONDS)) {
                    this.webExecutor.shutdownNow();
                }
            }
            catch (InterruptedException e) {
                this.webExecutor.shutdownNow();
            }
        }
    }

    public WebAPIController getApiController() {
        return this.apiController;
    }
}


