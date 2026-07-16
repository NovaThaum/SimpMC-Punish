/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.event.player.AsyncChatEvent
 *  net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 */
package me.simpmc.simpban.gui;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import me.simpmc.simpban.SimpBan;
import me.simpmc.simpban.model.PunishmentType;
import me.simpmc.simpban.util.MessageUtil;
import me.simpmc.simpban.util.MessagesManager;
import me.simpmc.simpban.util.TimeUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class ReasonInputHandler
implements Listener {
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();
    private final SimpBan plugin;
    private final Player staff;
    private final OfflinePlayer target;
    private final PunishmentType type;
    private final long durationMs;
    private volatile boolean completed = false;

    public ReasonInputHandler(SimpBan plugin, Player staff, OfflinePlayer target, PunishmentType type, long durationMs) {
        this.plugin = plugin;
        this.staff = staff;
        this.target = target;
        this.type = type;
        this.durationMs = durationMs;
    }

    private MessagesManager msg() {
        return this.plugin.getMessagesManager();
    }

    public void start() {
        if (!this.staff.hasPermission(this.type.getPermission())) {
            this.staff.sendMessage(MessageUtil.toComponent(this.msg().getMessage("input.reason.no-permission")));
            return;
        }
        this.plugin.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)this.plugin);
        String typeDisplay = this.type.getColor() + this.type.getDisplayName();
        String durationDisplay = this.durationMs > 0L ? TimeUtil.formatDuration(this.durationMs) : this.msg().getMessage("duration-permanent");
        this.staff.sendMessage(MessageUtil.toComponent(""));
        this.staff.sendMessage(MessageUtil.toComponent(this.msg().getMessage("input.reason.header")));
        this.staff.sendMessage(MessageUtil.toComponent(this.msg().getMessage("input.reason.title")));
        this.staff.sendMessage(MessageUtil.toComponent(""));
        this.staff.sendMessage(MessageUtil.toComponent(this.msg().getMessage("input.reason.target", "{player}", this.target.getName())));
        this.staff.sendMessage(MessageUtil.toComponent(this.msg().getMessage("input.reason.type", "{type}", typeDisplay)));
        this.staff.sendMessage(MessageUtil.toComponent(this.msg().getMessage("input.reason.duration", "{duration}", durationDisplay)));
        this.staff.sendMessage(MessageUtil.toComponent(""));
        this.staff.sendMessage(MessageUtil.toComponent(this.msg().getMessage("input.reason.instruction")));
        this.staff.sendMessage(MessageUtil.toComponent(this.msg().getMessage("input.reason.header")));
        this.plugin.getSchedulerManager().runAsyncLater(this::handleTimeout, 60L, TimeUnit.SECONDS);
    }

    private void handleTimeout() {
        if (!this.completed) {
            this.cleanup();
            this.plugin.getSchedulerManager().runForEntity((Entity)this.staff, () -> this.staff.sendMessage(MessageUtil.toComponent(this.msg().getMessage("input.reason.timeout"))));
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {
        if (!event.getPlayer().getUniqueId().equals(this.staff.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        String message = PLAIN_SERIALIZER.serialize(event.message());
        if (message.equalsIgnoreCase("cancel") || message.equals("取消")) {
            this.cleanup();
            this.staff.sendMessage(MessageUtil.toComponent(this.msg().getMessage("input.reason.cancelled")));
            return;
        }
        this.completed = true;
        this.cleanup();
        this.executePunishment(message);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event.getPlayer().getUniqueId().equals(this.staff.getUniqueId())) {
            this.cleanup();
        }
    }

    private void executePunishment(String reason) {
        if (!this.staff.hasPermission(this.type.getPermission())) {
            this.plugin.getSchedulerManager().runForEntity((Entity)this.staff, () -> this.staff.sendMessage(MessageUtil.toComponent(this.msg().getMessage("input.reason.no-permission"))));
            return;
        }
        UUID staffUUID = this.staff.getUniqueId();
        String staffName = this.staff.getName();
        CompletableFuture<?> future = switch (this.type) {
            case BAN -> this.plugin.getPunishmentManager().ban(this.target.getUniqueId(), this.target.getName(), staffUUID, staffName, reason);
            case TEMPBAN -> this.plugin.getPunishmentManager().tempban(this.target.getUniqueId(), this.target.getName(), staffUUID, staffName, this.durationMs, reason);
            case MUTE -> this.plugin.getPunishmentManager().mute(this.target.getUniqueId(), this.target.getName(), staffUUID, staffName, reason);
            case TEMPMUTE -> this.plugin.getPunishmentManager().tempmute(this.target.getUniqueId(), this.target.getName(), staffUUID, staffName, this.durationMs, reason);
            case KICK -> this.plugin.getPunishmentManager().kick(this.target.getUniqueId(), this.target.getName(), staffUUID, staffName, reason);
            case BANIP, TEMPBANIP, MUTEIP, TEMPMUTEIP -> throw new UnsupportedOperationException("IP 类处罚必须通过命令执行，不能通过 GUI 执行");
        };
        future.thenAccept(result -> {
            String durationStr = this.durationMs > 0L ? TimeUtil.formatDuration(this.durationMs) : this.msg().getMessage("duration-permanent");
            String configPath = switch (this.type) {
                case BAN -> "punishments.ban";
                case TEMPBAN -> "punishments.tempban";
                case MUTE -> "punishments.mute";
                case TEMPMUTE -> "punishments.tempmute";
                case KICK -> "punishments.kick";
                default -> null;
            };
            this.plugin.getSchedulerManager().runSync(() -> {
                this.staff.sendMessage(MessageUtil.toComponent(this.msg().getMessage(configPath + ".success", "{player}", this.target.getName(), "{duration}", durationStr)));
                if (configPath != null && this.msg().getConfig().getBoolean(configPath + ".broadcast.enabled", true)) {
                    String broadcastMsg = this.msg().getMessage(configPath + ".broadcast.message", "{staff}", staffName, "{player}", this.target.getName(), "{reason}", reason, "{duration}", durationStr);
                    String prefix = this.msg().getPrefix();
                    this.broadcastToStaff(prefix + broadcastMsg);
                }
            });
        }).exceptionally(e -> {
            this.plugin.getSchedulerManager().runForEntity((Entity)this.staff, () -> this.staff.sendMessage(MessageUtil.toComponent(this.msg().getMessage("input.reason.error"))));
            this.plugin.getLogger().severe("执行处罚失败: " + e.getMessage());
            return null;
        });
    }

    private void broadcastToStaff(String message) {
        Collection<? extends Player> players = this.plugin.getServer().getOnlinePlayers();
        for (Player p : players) {
            if (!p.hasPermission("simpban.staff")) continue;
            p.sendMessage(MessageUtil.toComponent(message));
        }
        this.plugin.getServer().getConsoleSender().sendMessage(MessageUtil.toComponent(message));
    }

    private void cleanup() {
        this.completed = true;
        HandlerList.unregisterAll((Listener)this);
    }
}


