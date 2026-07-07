/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.AsyncPlayerPreLoginEvent
 *  org.bukkit.event.player.AsyncPlayerPreLoginEvent$Result
 */
package me.simpfun.simpban.listeners;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import me.simpfun.simpban.SimpBan;
import me.simpfun.simpban.model.Punishment;
import me.simpfun.simpban.util.MessageUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class PlayerLoginListener
implements Listener {
    private final SimpBan plugin;

    public PlayerLoginListener(SimpBan plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String name = event.getName();
        String rawIp = event.getAddress().getHostAddress();
        String ipAddress = this.normalizeIp(rawIp);
        try {
            Optional<Punishment> activeIpBan;
            Optional<Punishment> activeBan = this.plugin.getPunishmentManager().getActiveBan(uuid).get(5L, TimeUnit.SECONDS);
            if (activeBan.isPresent()) {
                Punishment punishment = activeBan.get();
                if (!punishment.isExpired()) {
                    String kickMessage = this.plugin.getPunishmentManager().buildKickMessage(punishment.getTargetName(), punishment.getStaffName(), punishment.getType(), punishment.getReason(), punishment.getExpiresAt());
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, MessageUtil.toComponent(kickMessage));
                    this.plugin.getLogger().info(name + " attempted to join but is banned.");
                    return;
                }
                this.plugin.getCacheManager().invalidateBan(uuid);
            }
            if ((activeIpBan = this.plugin.getPunishmentManager().getActiveIpBan(ipAddress).get(5L, TimeUnit.SECONDS)).isPresent()) {
                Punishment punishment = activeIpBan.get();
                if (!punishment.isExpired()) {
                    String kickMessage = this.plugin.getPunishmentManager().buildKickMessage(punishment.getTargetName(), punishment.getStaffName(), punishment.getType(), punishment.getReason(), punishment.getExpiresAt());
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, MessageUtil.toComponent(kickMessage));
                    this.plugin.getLogger().info(name + " (" + ipAddress + ") attempted to join but their IP is banned.");
                    return;
                }
                this.plugin.getCacheManager().invalidateIpBan(ipAddress);
            }
            this.plugin.getPunishmentDAO().savePlayerIp(uuid, name, ipAddress);
        }
        catch (InterruptedException | ExecutionException e) {
            this.plugin.getLogger().warning("Failed to check ban status for " + name + ": " + e.getMessage());
        }
        catch (TimeoutException e) {
            this.plugin.getLogger().warning("Ban check timed out for " + name + ", allowing login.");
        }
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
}


