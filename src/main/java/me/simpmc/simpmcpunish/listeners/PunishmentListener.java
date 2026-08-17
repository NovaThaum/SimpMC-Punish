package me.simpmc.simpmcpunish.listeners;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import me.simpmc.simpmcpunish.SimpMCPunish;
import me.simpmc.simpmcpunish.model.Punishment;
import me.simpmc.simpmcpunish.util.MessageUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerKickEvent;

/**
 * Keeps player-facing punishment messages separate from the server's normal
 * connection lifecycle output.
 */
public final class PunishmentListener implements Listener {
    private final SimpMCPunish plugin;

    public PunishmentListener(SimpMCPunish plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        // Match EssentialsX: customize this plugin's result without replacing another system's result.
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        UUID uuid = event.getUniqueId();
        String name = event.getName();
        String ipAddress = normalizeIp(event.getAddress().getHostAddress());
        try {
            Optional<Punishment> activeBan = this.plugin.getPunishmentManager()
                    .getActiveBan(uuid)
                    .get(5L, TimeUnit.SECONDS);
            if (activeBan.isPresent()) {
                Punishment punishment = activeBan.get();
                if (!punishment.isExpired()) {
                    event.disallow(
                            AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                            MessageUtil.toComponent(this.plugin.getPunishmentManager().buildKickMessage(
                                    punishment.getTargetName(),
                                    punishment.getStaffName(),
                                    punishment.getType(),
                                    punishment.getReason(),
                                    punishment.getExpiresAt())));
                    return;
                }
                this.plugin.getCacheManager().invalidateBan(uuid);
            }

            Optional<Punishment> activeIpBan = this.plugin.getPunishmentManager()
                    .getActiveIpBan(ipAddress)
                    .get(5L, TimeUnit.SECONDS);
            if (activeIpBan.isPresent()) {
                Punishment punishment = activeIpBan.get();
                if (!punishment.isExpired()) {
                    event.disallow(
                            AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                            MessageUtil.toComponent(this.plugin.getPunishmentManager().buildKickMessage(
                                    punishment.getTargetName(),
                                    punishment.getStaffName(),
                                    punishment.getType(),
                                    punishment.getReason(),
                                    punishment.getExpiresAt())));
                    return;
                }
                this.plugin.getCacheManager().invalidateIpBan(ipAddress);
            }

            this.plugin.getPunishmentDAO().savePlayerIp(uuid, name, ipAddress);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            this.plugin.getLogger().warning("检查 " + name + " 的封禁状态时被中断。");
        } catch (ExecutionException exception) {
            this.plugin.getLogger().warning("检查 " + name + " 的封禁状态失败: " + exception.getMessage());
        } catch (TimeoutException exception) {
            this.plugin.getLogger().warning("检查 " + name + " 的封禁状态超时，已允许其登录。");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerKick(PlayerKickEvent event) {
        if (this.plugin.getPunishmentManager().consumeKickLeaveMessageSuppression(event.getPlayer().getUniqueId())) {
            event.leaveMessage(null);
        }
    }

    private static String normalizeIp(String ipAddress) {
        if (ipAddress == null || !ipAddress.startsWith("::ffff:")) {
            return ipAddress;
        }
        return ipAddress.substring(7);
    }
}
