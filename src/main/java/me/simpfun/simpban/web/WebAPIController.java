/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 */
package me.simpfun.simpban.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import me.simpfun.simpban.SimpBan;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import me.simpfun.simpban.model.Punishment;
import me.simpfun.simpban.model.PunishmentType;
import me.simpfun.simpban.util.DurationParser;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class WebAPIController {
    private final SimpBan plugin;
    private final Gson gson;
    private final String adminApiKey;
    private final ExecutorService webExecutor;

    public WebAPIController(SimpBan plugin, Gson gson, ExecutorService webExecutor) {
        this.plugin = plugin;
        this.gson = gson;
        this.adminApiKey = plugin.getConfig().getString("web-dashboard.admin-api-key", "");
        this.webExecutor = webExecutor;
    }

    private void runAsync(Context ctx, CompletableFuture<String> future) {
        ctx.future(() -> future.thenApply(result -> {
            ctx.result((String)result);
            return result;
        }));
    }

    public void getConfig(Context ctx) {
        HashMap<String, String> config = new HashMap<String, String>();
        config.put("serverName", this.plugin.getConfig().getString("web-dashboard.branding.server-name", "My Server"));
        config.put("accentColor", this.plugin.getConfig().getString("web-dashboard.branding.accent-color", "#ff5555"));
        config.put("footerText", this.plugin.getConfig().getString("web-dashboard.branding.footer-text", "Powered by SimpBan"));
        ctx.contentType("application/json");
        ctx.result(this.gson.toJson(config));
    }

    public void lookupPlayer(Context ctx) {
        String username = ctx.pathParam("username");
        if (username == null || username.trim().isEmpty()) {
            this.sendError(ctx, HttpStatus.BAD_REQUEST, "Username is required");
            return;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer((String)username);
        if (player.getUniqueId() == null || !player.hasPlayedBefore() && !player.isOnline()) {
            this.sendError(ctx, HttpStatus.NOT_FOUND, "Player not found");
            return;
        }
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();
        ctx.contentType("application/json");
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                List<Punishment> punishments = this.plugin.getPunishmentManager().getHistory(uuid).join();
                HashMap<String, Object> response = new HashMap<String, Object>();
                response.put("username", playerName);
                response.put("uuid", uuid.toString());
                response.put("punishments", punishments.stream().map(this::punishmentToMap).collect(Collectors.toList()));
                List active = punishments.stream().filter(Punishment::isActive).filter(p -> !p.isExpired()).map(this::punishmentToMap).collect(Collectors.toList());
                response.put("activePunishments", active);
                return this.gson.toJson(response);
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("Failed to lookup player: " + e.getMessage());
                return this.gson.toJson(Map.of("error", true, "message", "Failed to fetch punishments"));
            }
        }, this.webExecutor);
        this.runAsync(ctx, future);
    }

    public void getStats(Context ctx) {
        HashMap<String, Object> stats = new HashMap<String, Object>();
        stats.put("serverName", this.plugin.getConfig().getString("web-dashboard.branding.server-name", "My Server"));
        stats.put("online", Bukkit.getOnlinePlayers().size());
        stats.put("maxPlayers", Bukkit.getMaxPlayers());
        ctx.contentType("application/json");
        ctx.result(this.gson.toJson(stats));
    }

    public void authenticateAdmin(Context ctx) {
        if (this.adminApiKey.isEmpty() || this.adminApiKey.equals("CHANGE-ME-TO-A-SECURE-KEY")) {
            this.sendError(ctx, HttpStatus.SERVICE_UNAVAILABLE, "Admin API key not configured");
            return;
        }
        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            this.sendError(ctx, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
            return;
        }
        String providedKey = authHeader.substring(7);
        if (!providedKey.equals(this.adminApiKey)) {
            this.sendError(ctx, HttpStatus.FORBIDDEN, "Invalid API key");
            return;
        }
    }

    public void getRecentPunishments(Context ctx) {
        int limit = 10;
        try {
            String limitParam = ctx.queryParam("limit");
            if (limitParam != null) {
                limit = Math.min(50, Math.max(1, Integer.parseInt(limitParam)));
            }
        }
        catch (NumberFormatException limitParam) {
            // empty catch block
        }
        int finalLimit = limit;
        ctx.contentType("application/json");
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                List<Punishment> punishments = this.plugin.getPunishmentDAO().getRecentPunishments(finalLimit).join();
                HashMap<String, Object> response = new HashMap<String, Object>();
                response.put("punishments", punishments.stream().map(this::punishmentToMap).collect(Collectors.toList()));
                response.put("count", punishments.size());
                return this.gson.toJson(response);
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("[Web] Failed to get recent punishments: " + e.getMessage());
                return this.gson.toJson(Map.of("error", true, "message", "Failed to fetch recent punishments: " + e.getMessage()));
            }
        }, this.webExecutor);
        this.runAsync(ctx, future);
    }

    public void getAllActivePunishments(Context ctx) {
        ctx.contentType("application/json");
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                List<Punishment> punishments = this.plugin.getPunishmentDAO().getRecentPunishments(50).join();
                HashMap<String, Object> response = new HashMap<String, Object>();
                response.put("punishments", punishments.stream().map(this::punishmentToMap).collect(Collectors.toList()));
                response.put("count", punishments.size());
                return this.gson.toJson(response);
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("[Web] Failed to get punishments: " + e.getMessage());
                return this.gson.toJson(Map.of("error", true, "message", "Failed to fetch punishments"));
            }
        }, this.webExecutor);
        this.runAsync(ctx, future);
    }

    public void createPunishment(Context ctx) {
        PunishmentType punishmentType;
        JsonObject body2;
        try {
            body2 = this.gson.fromJson(ctx.body(), JsonObject.class);
        }
        catch (Exception e) {
            this.sendError(ctx, HttpStatus.BAD_REQUEST, "Invalid request body");
            return;
        }
        String targetName = this.getJsonString(body2, "target");
        String type = this.getJsonString(body2, "type");
        String reason = this.getJsonString(body2, "reason");
        String duration = this.getJsonString(body2, "duration");
        String staffName = this.getJsonString(body2, "staffName");
        if (targetName == null || type == null) {
            this.sendError(ctx, HttpStatus.BAD_REQUEST, "Missing required fields: target, type");
            return;
        }
        try {
            punishmentType = PunishmentType.valueOf(type.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            this.sendError(ctx, HttpStatus.BAD_REQUEST, "Invalid punishment type. Valid: BAN, MUTE, KICK");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer((String)targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            this.sendError(ctx, HttpStatus.NOT_FOUND, "Player not found");
            return;
        }
        UUID targetUUID = target.getUniqueId();
        String finalTargetName = target.getName();
        String finalReason = reason != null ? reason : "Punished via Web Dashboard";
        String finalStaffName = staffName != null ? staffName : "WebAdmin";
        String finalDuration = duration;
        ctx.contentType("application/json");
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                HashMap<String, Object> response = new HashMap<String, Object>();
                if (punishmentType == PunishmentType.KICK) {
                    Boolean success = this.plugin.getPunishmentManager().kick(targetUUID, finalTargetName, null, finalStaffName, finalReason).join();
                    this.plugin.getLogger().info("[Web] " + finalStaffName + " kicked " + finalTargetName + " via web dashboard. Reason: " + finalReason);
                    response.put("success", success);
                    response.put("message", success != false ? "Player kicked successfully" : "Failed to kick player");
                    return this.gson.toJson(response);
                } else if (finalDuration != null && !finalDuration.isEmpty()) {
                    long durationMs = DurationParser.parse(finalDuration);
                    if (durationMs <= 0L) {
                        return this.gson.toJson(Map.of("error", true, "message", "Invalid duration format"));
                    }
                    Punishment punishment = switch (punishmentType) {
                        case BAN -> this.plugin.getPunishmentManager().tempban(targetUUID, finalTargetName, null, finalStaffName, durationMs, finalReason).join();
                        case MUTE -> this.plugin.getPunishmentManager().tempmute(targetUUID, finalTargetName, null, finalStaffName, durationMs, finalReason).join();
                        default -> null;
                    };
                    if (punishment == null) return this.gson.toJson(Map.of("error", true, "message", "Unsupported punishment type"));
                    this.plugin.getLogger().info("[Web] " + finalStaffName + " temp-" + punishmentType.name().toLowerCase() + "ned " + finalTargetName + " via web dashboard. Duration: " + finalDuration + ", Reason: " + finalReason);
                    response.put("success", true);
                    response.put("message", "Punishment applied successfully");
                    response.put("punishment", this.punishmentToMap(punishment));
                    return this.gson.toJson(response);
                } else {
                    Punishment punishment = switch (punishmentType) {
                        case BAN -> this.plugin.getPunishmentManager().ban(targetUUID, finalTargetName, null, finalStaffName, finalReason).join();
                        case MUTE -> this.plugin.getPunishmentManager().mute(targetUUID, finalTargetName, null, finalStaffName, finalReason).join();
                        default -> null;
                    };
                    if (punishment == null) return this.gson.toJson(Map.of("error", true, "message", "Unsupported punishment type"));
                    this.plugin.getLogger().info("[Web] " + finalStaffName + " permanently " + punishmentType.name().toLowerCase() + "ned " + finalTargetName + " via web dashboard. Reason: " + finalReason);
                    response.put("success", true);
                    response.put("message", "Punishment applied successfully");
                    response.put("punishment", this.punishmentToMap(punishment));
                }
                return this.gson.toJson(response);
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("[Web] Failed to apply punishment: " + e.getMessage());
                return this.gson.toJson(Map.of("error", true, "message", "Failed to apply punishment: " + e.getMessage()));
            }
        }, this.webExecutor);
        this.runAsync(ctx, future);
    }

    public void removePunishment(Context ctx) {
        PunishmentType punishmentType;
        JsonObject body2;
        try {
            body2 = this.gson.fromJson(ctx.body(), JsonObject.class);
        }
        catch (Exception e) {
            this.sendError(ctx, HttpStatus.BAD_REQUEST, "Invalid request body");
            return;
        }
        String targetName = this.getJsonString(body2, "target");
        String type = this.getJsonString(body2, "type");
        String reason = this.getJsonString(body2, "reason");
        String staffName = this.getJsonString(body2, "staffName");
        if (targetName == null || type == null) {
            this.sendError(ctx, HttpStatus.BAD_REQUEST, "Missing required fields: target, type");
            return;
        }
        try {
            punishmentType = PunishmentType.valueOf(type.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            this.sendError(ctx, HttpStatus.BAD_REQUEST, "Invalid punishment type. Valid: BAN, MUTE");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer((String)targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            this.sendError(ctx, HttpStatus.NOT_FOUND, "Player not found");
            return;
        }
        UUID targetUUID = target.getUniqueId();
        String finalTargetName = target.getName();
        String finalReason = reason != null ? reason : "Removed via Web Dashboard";
        String finalStaffName = staffName != null ? staffName : "WebAdmin";
        ctx.contentType("application/json");
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                Boolean success = switch (punishmentType) {
                    case BAN -> this.plugin.getPunishmentManager().unban(targetUUID, null, finalStaffName, finalReason).join();
                    case MUTE -> this.plugin.getPunishmentManager().unmute(targetUUID, null, finalStaffName, finalReason).join();
                    default -> Boolean.FALSE;
                };
                if (success.booleanValue()) {
                    String action = punishmentType == PunishmentType.BAN ? "unbanned" : "unmuted";
                    this.plugin.getLogger().info("[Web] " + finalStaffName + " " + action + " " + finalTargetName + " via web dashboard. Reason: " + finalReason);
                }
                HashMap<String, Object> response = new HashMap<String, Object>();
                response.put("success", success);
                response.put("message", success != false ? "Punishment removed successfully" : "No active punishment found");
                return this.gson.toJson(response);
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("[Web] Failed to remove punishment: " + e.getMessage());
                return this.gson.toJson(Map.of("error", true, "message", "Failed to remove punishment: " + e.getMessage()));
            }
        }, this.webExecutor);
        this.runAsync(ctx, future);
    }

    public void testEndpoint(Context ctx) {
        HashMap<String, Object> response = new HashMap<String, Object>();
        response.put("status", "ok");
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "Web server is working!");
        ctx.contentType("application/json");
        ctx.result(this.gson.toJson(response));
    }

    private Map<String, Object> punishmentToMap(Punishment p) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("id", p.getId());
        map.put("targetUuid", p.getTargetUUID() != null ? p.getTargetUUID().toString() : null);
        map.put("targetName", p.getTargetName());
        map.put("staffName", p.getStaffName());
        map.put("type", p.getType().name());
        map.put("reason", p.getReason());
        map.put("createdAt", p.getCreatedAt() != null ? Long.valueOf(p.getCreatedAt().toEpochMilli()) : null);
        map.put("expiresAt", p.getExpiresAt() != null ? Long.valueOf(p.getExpiresAt().toEpochMilli()) : null);
        map.put("active", p.isActive());
        map.put("expired", p.isExpired());
        map.put("permanent", p.isPermanent());
        if (!p.isActive() && p.getRemovedByName() != null) {
            map.put("removedByName", p.getRemovedByName());
            map.put("removedAt", p.getRemovedAt() != null ? Long.valueOf(p.getRemovedAt().toEpochMilli()) : null);
            map.put("removeReason", p.getRemoveReason());
        }
        return map;
    }

    private String getJsonString(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return null;
    }

    private void sendError(Context ctx, HttpStatus status, String message) {
        HashMap<String, Object> error = new HashMap<String, Object>();
        error.put("error", true);
        error.put("message", message);
        ctx.status(status);
        ctx.contentType("application/json");
        ctx.result(this.gson.toJson(error));
    }
}


