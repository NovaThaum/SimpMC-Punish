/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 */
package me.simpmc.simpban.manager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import me.simpmc.simpban.SimpBan;
import me.simpmc.simpban.cache.CacheManager;
import me.simpmc.simpban.database.PunishmentDAO;
import me.simpmc.simpban.model.Punishment;
import me.simpmc.simpban.model.PunishmentType;
import me.simpmc.simpban.util.MessageUtil;
import me.simpmc.simpban.util.MessagesManager;
import me.simpmc.simpban.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class PunishmentManager {
    private final SimpBan plugin;
    private final PunishmentDAO punishmentDAO;
    private final CacheManager cacheManager;

    public PunishmentManager(SimpBan plugin, PunishmentDAO punishmentDAO, CacheManager cacheManager) {
        this.plugin = plugin;
        this.punishmentDAO = punishmentDAO;
        this.cacheManager = cacheManager;
        this.startExpiryTask();
    }

    private MessagesManager msg() {
        return this.plugin.getMessagesManager();
    }

    private void startExpiryTask() {
        int interval = this.plugin.getConfig().getInt("cache.expiry-check-interval", 60);
        this.plugin.getSchedulerManager().runAsyncRepeating(() -> this.punishmentDAO.cleanupExpired().thenAccept(count -> {
            if (count > 0) {
                this.plugin.getLogger().info("已清理 " + count + " 条过期处罚。");
                this.cacheManager.clear();
            }
        }), interval, interval, TimeUnit.SECONDS);
    }

    public CompletableFuture<Punishment> ban(UUID targetUUID, String targetName, UUID staffUUID, String staffName, String reason) {
        return this.createPunishment(targetUUID, targetName, staffUUID, staffName, PunishmentType.BAN, reason, null);
    }

    public CompletableFuture<Punishment> tempban(UUID targetUUID, String targetName, UUID staffUUID, String staffName, long durationMillis, String reason) {
        Instant expiresAt = TimeUtil.getExpiryInstant(durationMillis);
        return this.createPunishment(targetUUID, targetName, staffUUID, staffName, PunishmentType.TEMPBAN, reason, expiresAt);
    }

    public CompletableFuture<Boolean> unban(UUID targetUUID, UUID staffUUID, String staffName, String reason) {
        String targetName = Bukkit.getOfflinePlayer((UUID)targetUUID).getName();
        return this.punishmentDAO.deactivate(targetUUID, staffUUID, staffName, reason != null ? reason : "已解除封禁", PunishmentType.BAN, PunishmentType.TEMPBAN).thenApply(count -> {
            if (count > 0) {
                this.cacheManager.invalidateBan(targetUUID);
                this.plugin.getWebhookManager().logUnban(targetName != null ? targetName : targetUUID.toString(), staffName);
                return true;
            }
            return false;
        });
    }

    public CompletableFuture<Punishment> banIp(UUID targetUUID, String targetName, String ipAddress, UUID staffUUID, String staffName, String reason) {
        Instant expiresAt = null;
        return this.createBothBans(targetUUID, targetName, ipAddress, staffUUID, staffName, PunishmentType.BAN, PunishmentType.BANIP, reason, expiresAt);
    }

    public CompletableFuture<Punishment> tempbanIp(UUID targetUUID, String targetName, String ipAddress, UUID staffUUID, String staffName, long durationMillis, String reason) {
        Instant expiresAt = TimeUtil.getExpiryInstant(durationMillis);
        return this.createBothBans(targetUUID, targetName, ipAddress, staffUUID, staffName, PunishmentType.TEMPBAN, PunishmentType.TEMPBANIP, reason, expiresAt);
    }

    public CompletableFuture<Boolean> unbanIp(UUID targetUUID, String ipAddress, UUID staffUUID, String staffName, String reason) {
        String finalReason = reason != null ? reason : "已解除封禁";
        String targetName = Bukkit.getOfflinePlayer((UUID)targetUUID).getName();
        CompletableFuture<Integer> uuidUnban = this.punishmentDAO.deactivate(targetUUID, staffUUID, staffName, finalReason, PunishmentType.BAN, PunishmentType.TEMPBAN);
        CompletableFuture<Integer> ipUnban = this.punishmentDAO.deactivateIp(ipAddress, staffUUID, staffName, finalReason, PunishmentType.BANIP, PunishmentType.TEMPBANIP);
        return uuidUnban.thenCombine(ipUnban, (uuidCount, ipCount) -> {
            if (uuidCount > 0 || ipCount > 0) {
                this.cacheManager.invalidateBan(targetUUID);
                this.cacheManager.invalidateIpBan(ipAddress);
                this.plugin.getWebhookManager().logUnbanIp(ipAddress, targetName != null ? targetName : targetUUID.toString(), staffName);
                return true;
            }
            return false;
        });
    }

    private CompletableFuture<Punishment> createBothBans(UUID targetUUID, String targetName, String ipAddress, UUID staffUUID, String staffName, PunishmentType uuidType, PunishmentType ipType, String reason, Instant expiresAt) {
        String normalizedIp = this.normalizeIp(ipAddress);
        Punishment uuidPunishment = new Punishment(targetUUID, targetName, staffUUID, staffName, uuidType, reason, Instant.now(), expiresAt);
        Punishment ipPunishment = new Punishment(targetUUID, targetName, normalizedIp, staffUUID, staffName, ipType, reason, Instant.now(), expiresAt);
        return ((CompletableFuture)this.punishmentDAO.insert(uuidPunishment).thenCompose(uuidId -> {
            uuidPunishment.setId((int)uuidId);
            this.cacheManager.cacheBan(targetUUID, Optional.of(uuidPunishment));
            return this.punishmentDAO.insert(ipPunishment);
        })).thenApply(ipId -> {
            ipPunishment.setId((int)ipId);
            this.cacheManager.cacheIpBan(normalizedIp, Optional.of(ipPunishment));
            this.kickIfOnline(targetUUID, targetName, staffName, ipType, reason, expiresAt);
            this.plugin.getWebhookManager().logPunishment(ipPunishment);
            return ipPunishment;
        });
    }

    public CompletableFuture<Punishment> muteIp(UUID targetUUID, String targetName, String ipAddress, UUID staffUUID, String staffName, String reason) {
        Instant expiresAt = null;
        return this.createBothMutes(targetUUID, targetName, ipAddress, staffUUID, staffName, PunishmentType.MUTE, PunishmentType.MUTEIP, reason, expiresAt);
    }

    public CompletableFuture<Punishment> tempmuteIp(UUID targetUUID, String targetName, String ipAddress, UUID staffUUID, String staffName, long durationMillis, String reason) {
        Instant expiresAt = TimeUtil.getExpiryInstant(durationMillis);
        return this.createBothMutes(targetUUID, targetName, ipAddress, staffUUID, staffName, PunishmentType.TEMPMUTE, PunishmentType.TEMPMUTEIP, reason, expiresAt);
    }

    private CompletableFuture<Punishment> createBothMutes(UUID targetUUID, String targetName, String ipAddress, UUID staffUUID, String staffName, PunishmentType uuidType, PunishmentType ipType, String reason, Instant expiresAt) {
        String normalizedIp = this.normalizeIp(ipAddress);
        Punishment uuidPunishment = new Punishment(targetUUID, targetName, staffUUID, staffName, uuidType, reason, Instant.now(), expiresAt);
        Punishment ipPunishment = new Punishment(targetUUID, targetName, normalizedIp, staffUUID, staffName, ipType, reason, Instant.now(), expiresAt);
        return ((CompletableFuture)this.punishmentDAO.insert(uuidPunishment).thenCompose(uuidId -> {
            uuidPunishment.setId((int)uuidId);
            this.cacheManager.cacheMute(targetUUID, Optional.of(uuidPunishment));
            return this.punishmentDAO.insert(ipPunishment);
        })).thenApply(ipId -> {
            ipPunishment.setId((int)ipId);
            this.cacheManager.cacheIpMute(normalizedIp, Optional.of(ipPunishment));
            this.notifyMute(targetUUID, targetName, staffName, reason, expiresAt);
            this.plugin.getWebhookManager().logPunishment(ipPunishment);
            return ipPunishment;
        });
    }

    public CompletableFuture<Boolean> unmuteIp(UUID targetUUID, String ipAddress, UUID staffUUID, String staffName, String reason) {
        String finalReason = reason != null ? reason : "已解除禁言";
        String targetName = Bukkit.getOfflinePlayer((UUID)targetUUID).getName();
        CompletableFuture<Integer> uuidUnmute = this.punishmentDAO.deactivate(targetUUID, staffUUID, staffName, finalReason, PunishmentType.MUTE, PunishmentType.TEMPMUTE);
        CompletableFuture<Integer> ipUnmute = this.punishmentDAO.deactivateIp(ipAddress, staffUUID, staffName, finalReason, PunishmentType.MUTEIP, PunishmentType.TEMPMUTEIP);
        return uuidUnmute.thenCombine(ipUnmute, (uuidCount, ipCount) -> {
            if (uuidCount > 0 || ipCount > 0) {
                this.cacheManager.invalidateMute(targetUUID);
                this.cacheManager.invalidateIpMute(ipAddress);
                this.plugin.getWebhookManager().logUnmuteIp(ipAddress, targetName != null ? targetName : targetUUID.toString(), staffName);
                return true;
            }
            return false;
        });
    }

    public CompletableFuture<Punishment> mute(UUID targetUUID, String targetName, UUID staffUUID, String staffName, String reason) {
        return this.createPunishment(targetUUID, targetName, staffUUID, staffName, PunishmentType.MUTE, reason, null);
    }

    public CompletableFuture<Punishment> tempmute(UUID targetUUID, String targetName, UUID staffUUID, String staffName, long durationMillis, String reason) {
        Instant expiresAt = TimeUtil.getExpiryInstant(durationMillis);
        return this.createPunishment(targetUUID, targetName, staffUUID, staffName, PunishmentType.TEMPMUTE, reason, expiresAt);
    }

    public CompletableFuture<Boolean> unmute(UUID targetUUID, UUID staffUUID, String staffName, String reason) {
        String targetName = Bukkit.getOfflinePlayer((UUID)targetUUID).getName();
        return this.punishmentDAO.deactivate(targetUUID, staffUUID, staffName, reason != null ? reason : "已解除禁言", PunishmentType.MUTE, PunishmentType.TEMPMUTE).thenApply(count -> {
            if (count > 0) {
                this.cacheManager.invalidateMute(targetUUID);
                this.plugin.getWebhookManager().logUnmute(targetName != null ? targetName : targetUUID.toString(), staffName);
                return true;
            }
            return false;
        });
    }

    public CompletableFuture<Boolean> kick(UUID targetUUID, String targetName, UUID staffUUID, String staffName, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            Player target = Bukkit.getPlayer((UUID)targetUUID);
            if (target == null || !target.isOnline()) {
                return false;
            }
            Punishment punishment = new Punishment(targetUUID, targetName, staffUUID, staffName, PunishmentType.KICK, reason, Instant.now(), Instant.now());
            punishment.setActive(false);
            this.punishmentDAO.insert(punishment);
            this.plugin.getWebhookManager().logPunishment(punishment);
            String kickMessage = this.buildKickMessage(targetName, staffName, PunishmentType.KICK, reason, null);
            this.plugin.getSchedulerManager().runForEntity((Entity)target, () -> target.kick(MessageUtil.toComponent(kickMessage)));
            return true;
        });
    }

    public CompletableFuture<Optional<Punishment>> getActiveBan(UUID targetUUID) {
        Optional<Punishment> cached = this.cacheManager.getActiveBan(targetUUID);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return this.punishmentDAO.getActiveBan(targetUUID).thenApply(opt -> {
            this.cacheManager.cacheBan(targetUUID, (Optional<Punishment>)opt);
            return opt;
        });
    }

    public CompletableFuture<Optional<Punishment>> getActiveMute(UUID targetUUID) {
        Optional<Punishment> cached = this.cacheManager.getActiveMute(targetUUID);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return this.punishmentDAO.getActiveMute(targetUUID).thenApply(opt -> {
            this.cacheManager.cacheMute(targetUUID, (Optional<Punishment>)opt);
            return opt;
        });
    }

    public CompletableFuture<List<Punishment>> getHistory(UUID targetUUID) {
        return this.punishmentDAO.getHistory(targetUUID);
    }

    public CompletableFuture<Optional<Punishment>> getActiveIpBan(String ipAddress) {
        String normalizedIp = this.normalizeIp(ipAddress);
        Optional<Punishment> cached = this.cacheManager.getActiveIpBan(normalizedIp);
        if (cached != null && cached.isPresent()) {
            return CompletableFuture.completedFuture(cached);
        }
        return this.punishmentDAO.getActiveIpBan(normalizedIp).thenApply(opt -> {
            if (opt.isPresent()) {
                this.cacheManager.cacheIpBan(normalizedIp, (Optional<Punishment>)opt);
            }
            return opt;
        });
    }

    public CompletableFuture<Optional<Punishment>> getActiveIpMute(String ipAddress) {
        String normalizedIp = this.normalizeIp(ipAddress);
        Optional<Punishment> cached = this.cacheManager.getActiveIpMute(normalizedIp);
        if (cached != null && cached.isPresent()) {
            return CompletableFuture.completedFuture(cached);
        }
        return this.punishmentDAO.getActiveIpMute(normalizedIp).thenApply(opt -> {
            if (opt.isPresent()) {
                this.cacheManager.cacheIpMute(normalizedIp, (Optional<Punishment>)opt);
            }
            return opt;
        });
    }

    private String normalizeIp(String ip2) {
        if (ip2 == null) {
            return null;
        }
        if (ip2.startsWith("::ffff:")) {
            return ip2.substring(7);
        }
        return ip2;
    }

    private CompletableFuture<Punishment> createIpPunishment(UUID targetUUID, String targetName, String ipAddress, UUID staffUUID, String staffName, PunishmentType type, String reason, Instant expiresAt) {
        Punishment punishment = new Punishment(targetUUID, targetName, ipAddress, staffUUID, staffName, type, reason, Instant.now(), expiresAt);
        return this.punishmentDAO.insert(punishment).thenApply(id -> {
            punishment.setId((int)id);
            if (type.isIpBan()) {
                this.cacheManager.cacheIpBan(ipAddress, Optional.of(punishment));
                this.kickIfOnline(targetUUID, targetName, staffName, type, reason, expiresAt);
            } else if (type.isIpMute()) {
                this.cacheManager.cacheIpMute(ipAddress, Optional.of(punishment));
                this.notifyMute(targetUUID, targetName, staffName, reason, expiresAt);
            }
            this.plugin.getWebhookManager().logPunishment(punishment);
            return punishment;
        });
    }

    private CompletableFuture<Punishment> createPunishment(UUID targetUUID, String targetName, UUID staffUUID, String staffName, PunishmentType type, String reason, Instant expiresAt) {
        Punishment punishment = new Punishment(targetUUID, targetName, staffUUID, staffName, type, reason, Instant.now(), expiresAt);
        return this.punishmentDAO.insert(punishment).thenApply(id -> {
            punishment.setId((int)id);
            if (type.isBan()) {
                this.cacheManager.cacheBan(targetUUID, Optional.of(punishment));
                this.kickIfOnline(targetUUID, targetName, staffName, type, reason, expiresAt);
            } else if (type.isMute()) {
                this.cacheManager.cacheMute(targetUUID, Optional.of(punishment));
                this.notifyMute(targetUUID, targetName, staffName, reason, expiresAt);
            }
            this.plugin.getWebhookManager().logPunishment(punishment);
            return punishment;
        });
    }

    private void kickIfOnline(UUID targetUUID, String playerName, String staffName, PunishmentType type, String reason, Instant expiresAt) {
        Player target = Bukkit.getPlayer((UUID)targetUUID);
        if (target != null && target.isOnline()) {
            String kickMessage = this.buildKickMessage(playerName, staffName, type, reason, expiresAt);
            this.plugin.getSchedulerManager().runForEntity((Entity)target, () -> target.kick(MessageUtil.toComponent(kickMessage)));
        }
    }

    private void notifyMute(UUID targetUUID, String playerName, String staffName, String reason, Instant expiresAt) {
        Player target = Bukkit.getPlayer((UUID)targetUUID);
        if (target != null && target.isOnline()) {
            String muteMsg = this.msg().getMessage("punishments.mute.applied", "{reason}", reason != null ? reason : "未填写原因", "{expires}", TimeUtil.formatRemaining(expiresAt), "{player}", playerName != null ? playerName : "未知玩家", "{staff}", staffName != null ? staffName : "控制台");
            target.sendMessage(MessageUtil.toComponent(muteMsg));
        }
    }

    public String buildKickMessage(String playerName, String staffName, PunishmentType type, String reason, Instant expiresAt) {
        String template = type == PunishmentType.KICK ? this.msg().getMessage("punishments.kick.screen") : this.msg().getMessage("punishments.ban.screen");
        return template.replace("{reason}", reason != null ? reason : "未填写原因").replace("{expires}", TimeUtil.formatRemaining(expiresAt)).replace("{player}", playerName != null ? playerName : "未知玩家").replace("{staff}", staffName != null ? staffName : "控制台");
    }
}


