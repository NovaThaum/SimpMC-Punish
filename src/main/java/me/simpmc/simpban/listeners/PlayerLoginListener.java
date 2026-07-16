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
package me.simpmc.simpban.listeners;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import me.simpmc.simpban.SimpBan;
import me.simpmc.simpban.model.Punishment;
import me.simpmc.simpban.util.MessageUtil;
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
                    this.plugin.getLogger().info(name + " 尝试加入服务器，但当前处于封禁状态。");
                    return;
                }
                this.plugin.getCacheManager().invalidateBan(uuid);
            }
            if ((activeIpBan = this.plugin.getPunishmentManager().getActiveIpBan(ipAddress).get(5L, TimeUnit.SECONDS)).isPresent()) {
                Punishment punishment = activeIpBan.get();
                if (!punishment.isExpired()) {
                    String kickMessage = this.plugin.getPunishmentManager().buildKickMessage(punishment.getTargetName(), punishment.getStaffName(), punishment.getType(), punishment.getReason(), punishment.getExpiresAt());
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, MessageUtil.toComponent(kickMessage));
                    this.plugin.getLogger().info(name + " (" + ipAddress + ") 尝试加入服务器，但其 IP 当前处于封禁状态。");
                    return;
                }
                this.plugin.getCacheManager().invalidateIpBan(ipAddress);
            }
            this.plugin.getPunishmentDAO().savePlayerIp(uuid, name, ipAddress);
        }
        catch (InterruptedException | ExecutionException e) {
            this.plugin.getLogger().warning("检查 " + name + " 的封禁状态失败: " + e.getMessage());
        }
        catch (TimeoutException e) {
            this.plugin.getLogger().warning("检查 " + name + " 的封禁状态超时，已允许其登录。");
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


