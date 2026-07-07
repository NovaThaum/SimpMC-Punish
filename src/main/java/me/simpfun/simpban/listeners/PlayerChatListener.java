/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.event.player.AsyncChatEvent
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 */
package me.simpfun.simpban.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;
import me.simpfun.simpban.SimpBan;
import me.simpfun.simpban.model.Punishment;
import me.simpfun.simpban.util.MessageUtil;
import me.simpfun.simpban.util.TimeUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class PlayerChatListener
implements Listener {
    private final SimpBan plugin;

    public PlayerChatListener(SimpBan plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    public void onAsyncChat(AsyncChatEvent event) {
        Optional<Punishment> activeIpMute;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String bypassPerm = this.plugin.getConfig().getString("bypass.permission", "simpban.bypass");
        if (player.hasPermission(bypassPerm)) {
            return;
        }
        String ipAddress = this.getPlayerIp(player);
        Optional<Punishment> activeMute = this.plugin.getCacheManager().getActiveMute(uuid);
        if (activeMute != null && activeMute.isPresent()) {
            Punishment punishment = activeMute.get();
            if (!punishment.isExpired()) {
                this.handleMute(event, player, punishment);
                return;
            }
            this.plugin.getCacheManager().invalidateMute(uuid);
        }
        if (ipAddress != null && (activeIpMute = this.plugin.getCacheManager().getActiveIpMute(ipAddress)) != null && activeIpMute.isPresent()) {
            Punishment punishment = activeIpMute.get();
            if (!punishment.isExpired()) {
                this.handleMute(event, player, punishment);
                return;
            }
            this.plugin.getCacheManager().invalidateIpMute(ipAddress);
        }
    }

    private void handleMute(AsyncChatEvent event, Player player, Punishment punishment) {
        event.setCancelled(true);
        event.viewers().clear();
        String muteMessage = this.plugin.getMessagesManager().getMessage("punishments.mute.blocked", "{reason}", punishment.getReason() != null ? punishment.getReason() : "No reason specified", "{expires}", TimeUtil.formatRemaining(punishment.getExpiresAt()), "{player}", punishment.getTargetName() != null ? punishment.getTargetName() : player.getName(), "{staff}", punishment.getStaffName() != null ? punishment.getStaffName() : "Console");
        player.sendMessage(MessageUtil.toComponent(muteMessage));
    }

    private String getPlayerIp(Player player) {
        InetSocketAddress address = player.getAddress();
        if (address == null) {
            return null;
        }
        String ip2 = address.getAddress().getHostAddress();
        if (ip2 != null && ip2.startsWith("::ffff:")) {
            return ip2.substring(7);
        }
        return ip2;
    }
}


