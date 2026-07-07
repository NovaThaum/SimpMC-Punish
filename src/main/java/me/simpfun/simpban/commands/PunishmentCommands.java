/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 */
package me.simpfun.simpban.commands;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.simpfun.simpban.SimpBan;
import me.simpfun.simpban.gui.HistoryGUI;
import me.simpfun.simpban.gui.PunishmentGUI;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import me.simpfun.simpban.model.Punishment;
import me.simpfun.simpban.model.PunishmentType;
import me.simpfun.simpban.util.MessageUtil;
import me.simpfun.simpban.util.MessagesManager;
import me.simpfun.simpban.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

@CommandAlias(value="simpban|sb")
public class PunishmentCommands
extends BaseCommand {
    private final SimpBan plugin;

    public PunishmentCommands(SimpBan plugin) {
        this.plugin = plugin;
    }

    private MessagesManager msg() {
        return this.plugin.getMessagesManager();
    }

    @CommandAlias(value="ban")
    @CommandPermission(value="simpban.ban")
    @CommandCompletion(value="@players")
    @Description(value="Permanently ban a player")
    @Syntax(value="<player> [reason]")
    public void onBan(CommandSender sender, String targetName, @Optional String reason) {
        this.executePunishment(sender, targetName, PunishmentType.BAN, -1L, reason);
    }

    @CommandAlias(value="tempban")
    @CommandPermission(value="simpban.tempban")
    @CommandCompletion(value="@players")
    @Description(value="Temporarily ban a player")
    @Syntax(value="<player> <duration> [reason]")
    public void onTempBan(CommandSender sender, String targetName, String duration, @Optional String reason) {
        long durationMs = TimeUtil.parseDuration(duration);
        if (durationMs <= 0L) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.invalid-duration")));
            return;
        }
        this.executePunishment(sender, targetName, PunishmentType.TEMPBAN, durationMs, reason);
    }

    @CommandAlias(value="unban")
    @CommandPermission(value="simpban.unban")
    @CommandCompletion(value="@players")
    @Description(value="Unban a player")
    @Syntax(value="<player>")
    public void onUnban(CommandSender sender, String targetName) {
        UUID uUID;
        OfflinePlayer target = Bukkit.getOfflinePlayer((String)targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.player-not-found")));
            return;
        }
        if (sender instanceof Player) {
            Player p = (Player)sender;
            uUID = p.getUniqueId();
        } else {
            uUID = null;
        }
        UUID staffUUID = uUID;
        String staffName = sender.getName();
        this.plugin.getPunishmentManager().unban(target.getUniqueId(), staffUUID, staffName, "Unbanned").thenAccept(success -> {
            if (success.booleanValue()) {
                if (this.msg().getConfig().getBoolean("punishments.unban.broadcast.enabled", true)) {
                    String broadcastMsg = this.msg().getMessage("punishments.unban.broadcast.message", "{staff}", staffName, "{player}", targetName);
                    this.broadcastStaff(broadcastMsg);
                }
                sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("punishments.unban.success", "{player}", targetName)));
            } else {
                sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.not-banned", "{player}", targetName)));
            }
        });
    }

    @CommandAlias(value="mute")
    @CommandPermission(value="simpban.mute")
    @CommandCompletion(value="@players")
    @Description(value="Permanently mute a player")
    @Syntax(value="<player> [reason]")
    public void onMute(CommandSender sender, String targetName, @Optional String reason) {
        this.executePunishment(sender, targetName, PunishmentType.MUTE, -1L, reason);
    }

    @CommandAlias(value="tempmute")
    @CommandPermission(value="simpban.tempmute")
    @CommandCompletion(value="@players")
    @Description(value="Temporarily mute a player")
    @Syntax(value="<player> <duration> [reason]")
    public void onTempMute(CommandSender sender, String targetName, String duration, @Optional String reason) {
        long durationMs = TimeUtil.parseDuration(duration);
        if (durationMs <= 0L) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.invalid-duration")));
            return;
        }
        this.executePunishment(sender, targetName, PunishmentType.TEMPMUTE, durationMs, reason);
    }

    @CommandAlias(value="unmute")
    @CommandPermission(value="simpban.unmute")
    @CommandCompletion(value="@players")
    @Description(value="Unmute a player")
    @Syntax(value="<player>")
    public void onUnmute(CommandSender sender, String targetName) {
        UUID uUID;
        OfflinePlayer target = Bukkit.getOfflinePlayer((String)targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.player-not-found")));
            return;
        }
        if (sender instanceof Player) {
            Player p = (Player)sender;
            uUID = p.getUniqueId();
        } else {
            uUID = null;
        }
        UUID staffUUID = uUID;
        String staffName = sender.getName();
        this.plugin.getPunishmentManager().unmute(target.getUniqueId(), staffUUID, staffName, "Unmuted").thenAccept(success -> {
            if (success.booleanValue()) {
                if (this.msg().getConfig().getBoolean("punishments.unmute.broadcast.enabled", true)) {
                    String broadcastMsg = this.msg().getMessage("punishments.unmute.broadcast.message", "{staff}", staffName, "{player}", targetName);
                    this.broadcastStaff(broadcastMsg);
                }
                sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("punishments.unmute.success", "{player}", targetName)));
                Player onlineTarget = Bukkit.getPlayer((UUID)target.getUniqueId());
                if (onlineTarget != null) {
                    String notifyMsg = this.msg().getMessage("punishments.unmute.notify");
                    this.plugin.getSchedulerManager().runForEntity((Entity)onlineTarget, () -> onlineTarget.sendMessage(MessageUtil.toComponent(notifyMsg)));
                }
            } else {
                sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.not-muted", "{player}", targetName)));
            }
        });
    }

    @CommandAlias(value="banip")
    @CommandPermission(value="simpban.banip")
    @CommandCompletion(value="@players")
    @Description(value="Permanently IP ban a player (auto-resolves IP)")
    @Syntax(value="<player> [reason]")
    public void onBanIp(CommandSender sender, String targetName, @Optional String reason) {
        this.executeIpPunishment(sender, targetName, PunishmentType.BANIP, -1L, reason);
    }

    @CommandAlias(value="tempbanip")
    @CommandPermission(value="simpban.tempbanip")
    @CommandCompletion(value="@players")
    @Description(value="Temporarily IP ban a player (auto-resolves IP)")
    @Syntax(value="<player> <duration> [reason]")
    public void onTempBanIp(CommandSender sender, String targetName, String duration, @Optional String reason) {
        long durationMs = TimeUtil.parseDuration(duration);
        if (durationMs <= 0L) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.invalid-duration")));
            return;
        }
        this.executeIpPunishment(sender, targetName, PunishmentType.TEMPBANIP, durationMs, reason);
    }

    @CommandAlias(value="unbanip")
    @CommandPermission(value="simpban.unbanip")
    @CommandCompletion(value="@players")
    @Description(value="Remove an IP ban for a player (auto-resolves IP)")
    @Syntax(value="<player>")
    public void onUnbanIp(CommandSender sender, String targetName) {
        UUID uUID;
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer((String)targetName);
        if (!offlineTarget.hasPlayedBefore() && !offlineTarget.isOnline()) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.player-not-found")));
            return;
        }
        UUID targetUUID = offlineTarget.getUniqueId();
        if (sender instanceof Player) {
            Player p = (Player)sender;
            uUID = p.getUniqueId();
        } else {
            uUID = null;
        }
        UUID staffUUID = uUID;
        String staffName = sender.getName();
        this.plugin.getPunishmentDAO().getActiveIpBanByUUID(targetUUID).thenAccept(optionalBannedIp -> {
            if (optionalBannedIp.isPresent()) {
                this.executeUnbanIp(sender, targetUUID, targetName, (String)optionalBannedIp.get(), staffUUID, staffName);
            } else {
                String ipAddress;
                Player onlineTarget = Bukkit.getPlayer((String)targetName);
                if (onlineTarget != null && (ipAddress = PunishmentCommands.getPlayerIp(onlineTarget)) != null) {
                    this.executeUnbanIp(sender, targetUUID, targetName, ipAddress, staffUUID, staffName);
                    return;
                }
                sender.sendMessage(MessageUtil.toComponent("&c" + targetName + "'s IP is not banned!"));
            }
        });
    }

    private void executeUnbanIp(CommandSender sender, UUID targetUUID, String targetName, String ipAddress, UUID staffUUID, String staffName) {
        this.plugin.getPunishmentManager().unbanIp(targetUUID, ipAddress, staffUUID, staffName, "Unbanned").thenAccept(success -> {
            if (success.booleanValue()) {
                if (this.msg().getConfig().getBoolean("punishments.unbanip.broadcast.enabled", true)) {
                    String broadcastMsg = this.msg().getMessage("punishments.unbanip.broadcast.message", "{staff}", staffName, "{player}", targetName, "{ip}", ipAddress);
                    this.broadcastStaff(broadcastMsg);
                }
                sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("punishments.unbanip.success", "{player}", targetName)));
            } else {
                sender.sendMessage(MessageUtil.toComponent("&c" + targetName + "'s IP is not banned!"));
            }
        });
    }

    @CommandAlias(value="muteip")
    @CommandPermission(value="simpban.muteip")
    @CommandCompletion(value="@players")
    @Description(value="Permanently IP mute a player (auto-resolves IP)")
    @Syntax(value="<player> [reason]")
    public void onMuteIp(CommandSender sender, String targetName, @Optional String reason) {
        this.executeIpPunishment(sender, targetName, PunishmentType.MUTEIP, -1L, reason);
    }

    @CommandAlias(value="tempmuteip")
    @CommandPermission(value="simpban.tempmuteip")
    @CommandCompletion(value="@players")
    @Description(value="Temporarily IP mute a player (auto-resolves IP)")
    @Syntax(value="<player> <duration> [reason]")
    public void onTempMuteIp(CommandSender sender, String targetName, String duration, @Optional String reason) {
        long durationMs = TimeUtil.parseDuration(duration);
        if (durationMs <= 0L) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.invalid-duration")));
            return;
        }
        this.executeIpPunishment(sender, targetName, PunishmentType.TEMPMUTEIP, durationMs, reason);
    }

    @CommandAlias(value="unmuteip")
    @CommandPermission(value="simpban.unmuteip")
    @CommandCompletion(value="@players")
    @Description(value="Remove an IP mute for a player (auto-resolves IP)")
    @Syntax(value="<player>")
    public void onUnmuteIp(CommandSender sender, String targetName) {
        UUID uUID;
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer((String)targetName);
        if (!offlineTarget.hasPlayedBefore() && !offlineTarget.isOnline()) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.player-not-found")));
            return;
        }
        UUID targetUUID = offlineTarget.getUniqueId();
        if (sender instanceof Player) {
            Player p = (Player)sender;
            uUID = p.getUniqueId();
        } else {
            uUID = null;
        }
        UUID staffUUID = uUID;
        String staffName = sender.getName();
        this.plugin.getPunishmentDAO().getActiveIpMuteByUUID(targetUUID).thenAccept(optionalMutedIp -> {
            if (optionalMutedIp.isPresent()) {
                this.executeUnmuteIp(sender, targetUUID, targetName, (String)optionalMutedIp.get(), staffUUID, staffName);
            } else {
                String ipAddress;
                Player onlineTarget = Bukkit.getPlayer((String)targetName);
                if (onlineTarget != null && (ipAddress = PunishmentCommands.getPlayerIp(onlineTarget)) != null) {
                    this.executeUnmuteIp(sender, targetUUID, targetName, ipAddress, staffUUID, staffName);
                    return;
                }
                sender.sendMessage(MessageUtil.toComponent("&c" + targetName + "'s IP is not muted!"));
            }
        });
    }

    private void executeUnmuteIp(CommandSender sender, UUID targetUUID, String targetName, String ipAddress, UUID staffUUID, String staffName) {
        this.plugin.getPunishmentManager().unmuteIp(targetUUID, ipAddress, staffUUID, staffName, "Unmuted").thenAccept(success -> {
            if (success.booleanValue()) {
                if (this.msg().getConfig().getBoolean("punishments.unmuteip.broadcast.enabled", true)) {
                    String broadcastMsg = this.msg().getMessage("punishments.unmuteip.broadcast.message", "{staff}", staffName, "{player}", targetName, "{ip}", ipAddress);
                    this.broadcastStaff(broadcastMsg);
                }
                sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("punishments.unmuteip.success", "{player}", targetName)));
            } else {
                sender.sendMessage(MessageUtil.toComponent("&c" + targetName + "'s IP is not muted!"));
            }
        });
    }

    @CommandAlias(value="kick")
    @CommandPermission(value="simpban.kick")
    @CommandCompletion(value="@players")
    @Description(value="Kick a player from the server")
    @Syntax(value="<player> [reason]")
    public void onKick(CommandSender sender, String targetName, @Optional String reason) {
        UUID uUID;
        Player target = Bukkit.getPlayer((String)targetName);
        if (target == null) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.player-not-online")));
            return;
        }
        String bypassPerm = this.plugin.getConfig().getString("bypass.permission", "simpban.bypass");
        if (target.hasPermission(bypassPerm)) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.cannot-punish")));
            return;
        }
        if (sender instanceof Player) {
            Player p = (Player)sender;
            uUID = p.getUniqueId();
        } else {
            uUID = null;
        }
        UUID staffUUID = uUID;
        String staffName = sender.getName();
        String finalReason = reason != null ? reason : "Kicked by staff";
        this.plugin.getPunishmentManager().kick(target.getUniqueId(), target.getName(), staffUUID, staffName, finalReason).thenAccept(success -> {
            if (success.booleanValue()) {
                if (this.msg().getConfig().getBoolean("punishments.kick.broadcast.enabled", true)) {
                    String broadcastMsg = this.msg().getMessage("punishments.kick.broadcast.message", "{staff}", staffName, "{player}", targetName, "{reason}", finalReason);
                    this.broadcastStaff(broadcastMsg);
                }
                sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("punishments.kick.success", "{player}", targetName)));
            } else {
                sender.sendMessage(MessageUtil.toComponent("&cFailed to kick " + targetName));
            }
        });
    }

    @CommandAlias(value="punish")
    @CommandPermission(value="simpban.punish")
    @CommandCompletion(value="@players")
    @Description(value="Open the punishment GUI for a player")
    @Syntax(value="<player>")
    public void onPunish(Player sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer((String)targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.player-not-found")));
            return;
        }
        PunishmentGUI.openAsync(this.plugin, sender, target);
    }

    @CommandAlias(value="history")
    @CommandPermission(value="simpban.history")
    @CommandCompletion(value="@players")
    @Description(value="View punishment history for a player")
    @Syntax(value="<player>")
    public void onHistory(Player sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer((String)targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.player-not-found")));
            return;
        }
        HistoryGUI.openAsync(this.plugin, sender, target);
    }

    @Subcommand(value="reload")
    @CommandPermission(value="simpban.admin")
    @Description(value="Reload the plugin configuration")
    public void onReload(CommandSender sender) {
        this.plugin.reloadConfig();
        this.plugin.getMessagesManager().reload();
        this.plugin.getCacheManager().clear();
        sender.sendMessage(MessageUtil.toComponent("&aSimpBan configuration reloaded!"));
    }

    @Subcommand(value="help")
    @CommandAlias(value="wghelp")
    @CommandPermission(value="simpban.help")
    @Description(value="Show all SimpBan commands")
    public void onHelp(CommandSender sender) {
        String[] helpLines;
        for (String line : helpLines = new String[]{"", "&8&m\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501&r &c&lSimpBan Help &8&m\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501", "", "&c/ban &7<player> [reason] &8- &fPermanently ban a player", "&c/tempban &7<player> <duration> [reason] &8- &fTemporarily ban", "&c/unban &7<player> &8- &fUnban a player", "", "&e/mute &7<player> [reason] &8- &fPermanently mute a player", "&e/tempmute &7<player> <duration> [reason] &8- &fTemporarily mute", "&e/unmute &7<player> &8- &fUnmute a player", "", "&d/banip &7<player> [reason] &8- &fIP ban a player", "&d/tempbanip &7<player> <duration> [reason] &8- &fTemp IP ban", "&d/unbanip &7<player> &8- &fRemove IP ban", "", "&b/muteip &7<player> [reason] &8- &fIP mute a player", "&b/tempmuteip &7<player> <duration> [reason] &8- &fTemp IP mute", "&b/unmuteip &7<player> &8- &fRemove IP mute", "", "&6/kick &7<player> [reason] &8- &fKick a player", "&a/punish &7<player> &8- &fOpen punishment GUI", "&a/history &7<player> &8- &fView punishment history", "", "&c/sb reload &8- &fReload configuration", "&c/sb help &8- &fShow this help menu", "", "&7Duration formats: &f1s, 30m, 6h, 7d, 4w, 1M, 1y", "&8&m\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501", ""}) {
            sender.sendMessage(MessageUtil.toComponent(line));
        }
    }

    private void executePunishment(CommandSender sender, String targetName, PunishmentType type, long durationMs, String reason) {
        CompletableFuture<Punishment> future;
        UUID uUID;
        OfflinePlayer target = Bukkit.getOfflinePlayer((String)targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.player-not-found")));
            return;
        }
        Player onlineTarget = target.getPlayer();
        String bypassPerm = this.plugin.getConfig().getString("bypass.permission", "simpban.bypass");
        if (onlineTarget != null && onlineTarget.hasPermission(bypassPerm)) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.cannot-punish")));
            return;
        }
        if (sender instanceof Player) {
            Player p = (Player)sender;
            uUID = p.getUniqueId();
        } else {
            uUID = null;
        }
        UUID staffUUID = uUID;
        String staffName = sender.getName();
        String finalReason = reason != null ? reason : "No reason specified";
        future = switch (type) {
            case BAN -> this.plugin.getPunishmentManager().ban(target.getUniqueId(), target.getName(), staffUUID, staffName, finalReason);
            case TEMPBAN -> this.plugin.getPunishmentManager().tempban(target.getUniqueId(), target.getName(), staffUUID, staffName, durationMs, finalReason);
            case MUTE -> this.plugin.getPunishmentManager().mute(target.getUniqueId(), target.getName(), staffUUID, staffName, finalReason);
            case TEMPMUTE -> this.plugin.getPunishmentManager().tempmute(target.getUniqueId(), target.getName(), staffUUID, staffName, durationMs, finalReason);
            default -> null;
        };
        if (future == null) {
            return;
        }
        future.thenAccept(punishment -> {
            String durationStr = durationMs > 0L ? TimeUtil.formatDuration(durationMs) : this.msg().getMessage("duration-permanent");
            String configPath = switch (type) {
                case BAN -> "punishments.ban";
                case TEMPBAN -> "punishments.tempban";
                case MUTE -> "punishments.mute";
                case TEMPMUTE -> "punishments.tempmute";
                default -> null;
            };
            if (configPath != null && this.msg().getConfig().getBoolean(configPath + ".broadcast.enabled", true)) {
                String broadcastMsg = this.msg().getMessage(configPath + ".broadcast.message", "{staff}", staffName, "{player}", targetName, "{reason}", finalReason, "{duration}", durationStr);
                this.broadcastStaff(broadcastMsg);
            }
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage(configPath + ".success", "{player}", targetName, "{duration}", durationStr)));
        }).exceptionally(e -> {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("input.reason.error")));
            this.plugin.getLogger().severe("Failed to apply punishment: " + e.getMessage());
            return null;
        });
    }

    private void broadcastStaff(String message) {
        String prefix = this.msg().getPrefix();
        String formattedMessage = prefix + message;
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        for (Player player : players) {
            if (!player.hasPermission("simpban.staff")) continue;
            player.sendMessage(MessageUtil.toComponent(formattedMessage));
        }
        Bukkit.getConsoleSender().sendMessage(MessageUtil.toComponent(formattedMessage));
    }

    private void executeIpPunishment(CommandSender sender, String targetName, PunishmentType type, long durationMs, String reason) {
        Player onlineTarget = Bukkit.getPlayer((String)targetName);
        if (onlineTarget != null) {
            String bypassPerm = this.plugin.getConfig().getString("bypass.permission", "simpban.bypass");
            if (onlineTarget.hasPermission(bypassPerm)) {
                sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.cannot-punish")));
                return;
            }
            String ipAddress = PunishmentCommands.getPlayerIp(onlineTarget);
            if (ipAddress == null) {
                sender.sendMessage(MessageUtil.toComponent("&cCould not resolve player's IP address!"));
                return;
            }
            this.applyIpPunishment(sender, onlineTarget.getUniqueId(), targetName, ipAddress, type, durationMs, reason);
        } else {
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer((String)targetName);
            if (!offlineTarget.hasPlayedBefore()) {
                sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.player-not-found")));
                return;
            }
            this.plugin.getPunishmentDAO().getPlayerIp(offlineTarget.getUniqueId()).thenAccept(optionalIp -> {
                if (optionalIp.isEmpty()) {
                    sender.sendMessage(MessageUtil.toComponent("&cNo stored IP found for " + targetName + "!"));
                    return;
                }
                String ipAddress = (String)optionalIp.get();
                this.applyIpPunishment(sender, offlineTarget.getUniqueId(), targetName, ipAddress, type, durationMs, reason);
            });
        }
    }

    private void applyIpPunishment(CommandSender sender, UUID targetUUID, String targetName, String ipAddress, PunishmentType type, long durationMs, String reason) {
        CompletableFuture<Punishment> future;
        UUID uUID;
        if (sender instanceof Player) {
            Player p = (Player)sender;
            uUID = p.getUniqueId();
        } else {
            uUID = null;
        }
        UUID staffUUID = uUID;
        String staffName = sender.getName();
        String finalReason = reason != null ? reason : "No reason specified";
        future = switch (type) {
            case BANIP -> this.plugin.getPunishmentManager().banIp(targetUUID, targetName, ipAddress, staffUUID, staffName, finalReason);
            case TEMPBANIP -> this.plugin.getPunishmentManager().tempbanIp(targetUUID, targetName, ipAddress, staffUUID, staffName, durationMs, finalReason);
            case MUTEIP -> this.plugin.getPunishmentManager().muteIp(targetUUID, targetName, ipAddress, staffUUID, staffName, finalReason);
            case TEMPMUTEIP -> this.plugin.getPunishmentManager().tempmuteIp(targetUUID, targetName, ipAddress, staffUUID, staffName, durationMs, finalReason);
            default -> null;
        };
        if (future == null) {
            return;
        }
        future.thenAccept(punishment -> {
            String durationStr = durationMs > 0L ? TimeUtil.formatDuration(durationMs) : this.msg().getMessage("duration-permanent");
            String configPath = switch (type) {
                case BANIP -> "punishments.banip";
                case TEMPBANIP -> "punishments.tempbanip";
                case MUTEIP -> "punishments.muteip";
                case TEMPMUTEIP -> "punishments.tempmuteip";
                default -> null;
            };
            if (configPath != null && this.msg().getConfig().getBoolean(configPath + ".broadcast.enabled", true)) {
                String broadcastMsg = this.msg().getMessage(configPath + ".broadcast.message", "{staff}", staffName, "{player}", targetName, "{ip}", ipAddress, "{reason}", finalReason, "{duration}", durationStr);
                this.broadcastStaff(broadcastMsg);
            }
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage(configPath + ".success", "{player}", targetName, "{ip}", ipAddress, "{duration}", durationStr)));
        }).exceptionally(e -> {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("input.reason.error")));
            this.plugin.getLogger().severe("Failed to apply IP punishment: " + e.getMessage());
            return null;
        });
    }

    private static String getPlayerIp(Player player) {
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


