package me.simpmc.simpmcpunish.commands;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.simpmc.simpmcpunish.SimpMCPunish;
import me.simpmc.simpmcpunish.database.PunishmentDAO.PunishmentPage;
import me.simpmc.simpmcpunish.gui.HistoryGUI;
import me.simpmc.simpmcpunish.gui.PunishmentGUI;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import me.simpmc.simpmcpunish.model.Punishment;
import me.simpmc.simpmcpunish.model.PunishmentType;
import me.simpmc.simpmcpunish.util.MessageUtil;
import me.simpmc.simpmcpunish.util.MessagesManager;
import me.simpmc.simpmcpunish.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

@CommandAlias(value="simpmc-punish|smp")
public class PunishmentCommands
extends BaseCommand {
    private static final int BAN_LIST_PAGE_SIZE = 10;
    private final SimpMCPunish plugin;

    public PunishmentCommands(SimpMCPunish plugin) {
        this.plugin = plugin;
    }

    private MessagesManager msg() {
        return this.plugin.getMessagesManager();
    }

    @CommandAlias(value="ban")
    @CommandPermission(value="simpmc-punish.ban")
    @CommandCompletion(value="@players")
    @Description(value="永久封禁玩家")
    @Syntax(value="<玩家> [原因]")
    public void onBan(CommandSender sender, String targetName, @Optional String reason) {
        this.executePunishment(sender, targetName, PunishmentType.BAN, -1L, reason);
    }

    @CommandAlias(value="tempban")
    @CommandPermission(value="simpmc-punish.tempban")
    @CommandCompletion(value="@players")
    @Description(value="临时封禁玩家")
    @Syntax(value="<玩家> <时长> [原因]")
    public void onTempBan(CommandSender sender, String targetName, String duration, @Optional String reason) {
        long durationMs = TimeUtil.parseDuration(duration);
        if (durationMs <= 0L) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.invalid-duration")));
            return;
        }
        this.executePunishment(sender, targetName, PunishmentType.TEMPBAN, durationMs, reason);
    }

    @CommandAlias(value="unban")
    @CommandPermission(value="simpmc-punish.unban")
    @CommandCompletion(value="@players")
    @Description(value="解除玩家封禁")
    @Syntax(value="<玩家>")
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
        this.plugin.getPunishmentManager().unban(target.getUniqueId(), staffUUID, staffName, "已解除封禁").thenAccept(success -> {
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
    @CommandPermission(value="simpmc-punish.mute")
    @CommandCompletion(value="@players")
    @Description(value="永久禁言玩家")
    @Syntax(value="<玩家> [原因]")
    public void onMute(CommandSender sender, String targetName, @Optional String reason) {
        this.executePunishment(sender, targetName, PunishmentType.MUTE, -1L, reason);
    }

    @CommandAlias(value="tempmute")
    @CommandPermission(value="simpmc-punish.tempmute")
    @CommandCompletion(value="@players")
    @Description(value="临时禁言玩家")
    @Syntax(value="<玩家> <时长> [原因]")
    public void onTempMute(CommandSender sender, String targetName, String duration, @Optional String reason) {
        long durationMs = TimeUtil.parseDuration(duration);
        if (durationMs <= 0L) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.invalid-duration")));
            return;
        }
        this.executePunishment(sender, targetName, PunishmentType.TEMPMUTE, durationMs, reason);
    }

    @CommandAlias(value="unmute")
    @CommandPermission(value="simpmc-punish.unmute")
    @CommandCompletion(value="@players")
    @Description(value="解除玩家禁言")
    @Syntax(value="<玩家>")
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
        this.plugin.getPunishmentManager().unmute(target.getUniqueId(), staffUUID, staffName, "已解除禁言").thenAccept(success -> {
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
    @CommandPermission(value="simpmc-punish.banip")
    @CommandCompletion(value="@players")
    @Description(value="永久封禁玩家 IP（自动解析 IP）")
    @Syntax(value="<玩家> [原因]")
    public void onBanIp(CommandSender sender, String targetName, @Optional String reason) {
        this.executeIpPunishment(sender, targetName, PunishmentType.BANIP, -1L, reason);
    }

    @CommandAlias(value="tempbanip")
    @CommandPermission(value="simpmc-punish.tempbanip")
    @CommandCompletion(value="@players")
    @Description(value="临时封禁玩家 IP（自动解析 IP）")
    @Syntax(value="<玩家> <时长> [原因]")
    public void onTempBanIp(CommandSender sender, String targetName, String duration, @Optional String reason) {
        long durationMs = TimeUtil.parseDuration(duration);
        if (durationMs <= 0L) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.invalid-duration")));
            return;
        }
        this.executeIpPunishment(sender, targetName, PunishmentType.TEMPBANIP, durationMs, reason);
    }

    @CommandAlias(value="unbanip")
    @CommandPermission(value="simpmc-punish.unbanip")
    @CommandCompletion(value="@players")
    @Description(value="解除玩家 IP 封禁（自动解析 IP）")
    @Syntax(value="<玩家>")
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
                sender.sendMessage(MessageUtil.toComponent("&c" + targetName + " 的 IP 当前没有被封禁。"));
            }
        });
    }

    private void executeUnbanIp(CommandSender sender, UUID targetUUID, String targetName, String ipAddress, UUID staffUUID, String staffName) {
        this.plugin.getPunishmentManager().unbanIp(targetUUID, ipAddress, staffUUID, staffName, "已解除封禁").thenAccept(success -> {
            if (success.booleanValue()) {
                if (this.msg().getConfig().getBoolean("punishments.unbanip.broadcast.enabled", true)) {
                    String broadcastMsg = this.msg().getMessage("punishments.unbanip.broadcast.message", "{staff}", staffName, "{player}", targetName, "{ip}", ipAddress);
                    this.broadcastStaff(broadcastMsg);
                }
                sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("punishments.unbanip.success", "{player}", targetName)));
            } else {
                sender.sendMessage(MessageUtil.toComponent("&c" + targetName + " 的 IP 当前没有被封禁。"));
            }
        });
    }

    @CommandAlias(value="muteip")
    @CommandPermission(value="simpmc-punish.muteip")
    @CommandCompletion(value="@players")
    @Description(value="永久禁言玩家 IP（自动解析 IP）")
    @Syntax(value="<玩家> [原因]")
    public void onMuteIp(CommandSender sender, String targetName, @Optional String reason) {
        this.executeIpPunishment(sender, targetName, PunishmentType.MUTEIP, -1L, reason);
    }

    @CommandAlias(value="tempmuteip")
    @CommandPermission(value="simpmc-punish.tempmuteip")
    @CommandCompletion(value="@players")
    @Description(value="临时禁言玩家 IP（自动解析 IP）")
    @Syntax(value="<玩家> <时长> [原因]")
    public void onTempMuteIp(CommandSender sender, String targetName, String duration, @Optional String reason) {
        long durationMs = TimeUtil.parseDuration(duration);
        if (durationMs <= 0L) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.invalid-duration")));
            return;
        }
        this.executeIpPunishment(sender, targetName, PunishmentType.TEMPMUTEIP, durationMs, reason);
    }

    @CommandAlias(value="unmuteip")
    @CommandPermission(value="simpmc-punish.unmuteip")
    @CommandCompletion(value="@players")
    @Description(value="解除玩家 IP 禁言（自动解析 IP）")
    @Syntax(value="<玩家>")
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
                sender.sendMessage(MessageUtil.toComponent("&c" + targetName + " 的 IP 当前没有被禁言。"));
            }
        });
    }

    private void executeUnmuteIp(CommandSender sender, UUID targetUUID, String targetName, String ipAddress, UUID staffUUID, String staffName) {
        this.plugin.getPunishmentManager().unmuteIp(targetUUID, ipAddress, staffUUID, staffName, "已解除禁言").thenAccept(success -> {
            if (success.booleanValue()) {
                if (this.msg().getConfig().getBoolean("punishments.unmuteip.broadcast.enabled", true)) {
                    String broadcastMsg = this.msg().getMessage("punishments.unmuteip.broadcast.message", "{staff}", staffName, "{player}", targetName, "{ip}", ipAddress);
                    this.broadcastStaff(broadcastMsg);
                }
                sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("punishments.unmuteip.success", "{player}", targetName)));
            } else {
                sender.sendMessage(MessageUtil.toComponent("&c" + targetName + " 的 IP 当前没有被禁言。"));
            }
        });
    }

    @CommandAlias(value="kick")
    @CommandPermission(value="simpmc-punish.kick")
    @CommandCompletion(value="@players")
    @Description(value="将玩家踢出服务器")
    @Syntax(value="<玩家> [原因]")
    public void onKick(CommandSender sender, String targetName, @Optional String reason) {
        UUID uUID;
        Player target = Bukkit.getPlayer((String)targetName);
        if (target == null) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.player-not-online")));
            return;
        }
        String bypassPerm = this.plugin.getConfig().getString("bypass.permission", "simpmc-punish.bypass");
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
        String finalReason = reason != null ? reason : "由管理员踢出";
        this.plugin.getPunishmentManager().kick(target.getUniqueId(), target.getName(), staffUUID, staffName, finalReason).thenAccept(success -> {
            if (success.booleanValue()) {
                if (this.msg().getConfig().getBoolean("punishments.kick.broadcast.enabled", true)) {
                    String broadcastMsg = this.msg().getMessage("punishments.kick.broadcast.message", "{staff}", staffName, "{player}", targetName, "{reason}", finalReason);
                    this.broadcastStaff(broadcastMsg);
                }
                sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("punishments.kick.success", "{player}", targetName)));
            } else {
                sender.sendMessage(MessageUtil.toComponent("&c踢出 " + targetName + " 失败。"));
            }
        });
    }

    @CommandAlias(value="punish")
    @CommandPermission(value="simpmc-punish.punish")
    @CommandCompletion(value="@players")
    @Description(value="打开玩家处罚菜单界面")
    @Syntax(value="<玩家>")
    public void onPunish(Player sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer((String)targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.player-not-found")));
            return;
        }
        PunishmentGUI.openAsync(this.plugin, sender, target);
    }

    @CommandAlias(value="history")
    @CommandPermission(value="simpmc-punish.history")
    @CommandCompletion(value="@players")
    @Description(value="查看玩家处罚历史")
    @Syntax(value="<玩家>")
    public void onHistory(Player sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer((String)targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.player-not-found")));
            return;
        }
        HistoryGUI.openAsync(this.plugin, sender, target);
    }

    @CommandAlias(value="banlist")
    @CommandPermission(value="simpmc-punish.banlist")
    @Description(value="查看当前生效中的封禁列表")
    @Syntax(value="[页码]")
    public void onBanList(CommandSender sender, @Optional String pageInput) {
        int page = 1;
        if (pageInput != null && !pageInput.isBlank()) {
            try {
                page = Integer.parseInt(pageInput);
            } catch (NumberFormatException ignored) {
                sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("banlist.invalid-page")));
                return;
            }
        }
        if (page < 1) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("banlist.invalid-page")));
            return;
        }

        int requestedPage = page;
        long offset = (requestedPage - 1L) * BAN_LIST_PAGE_SIZE;
        this.plugin.getPunishmentDAO().getActiveBansPage(offset, BAN_LIST_PAGE_SIZE)
            .whenComplete((result, error) -> this.runForSender(sender, () -> {
                if (error != null) {
                    sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("banlist.error")));
                    this.plugin.getLogger().severe("读取封禁列表失败: " + error.getMessage());
                    return;
                }
                this.sendBanList(sender, requestedPage, result);
            }));
    }

    @Subcommand(value="reload|重载")
    @CommandPermission(value="simpmc-punish.admin")
    @Description(value="重载插件配置")
    public void onReload(CommandSender sender) {
        this.plugin.reloadConfig();
        this.plugin.getMessagesManager().reload();
        this.plugin.getCacheManager().clear();
        sender.sendMessage(MessageUtil.toComponent("&aSimpMC-Punish 配置已重载。"));
    }

    @Subcommand(value="help|帮助")
    @CommandAlias(value="simpmc-punishhelp|smphelp|simpmc-punish帮助|smp帮助")
    @CommandPermission(value="simpmc-punish.help")
    @Description(value="显示 SimpMC-Punish 命令帮助")
    public void onHelp(CommandSender sender) {
        String[] helpLines = new String[]{
            "",
            "&8&m━━━━━━━━━━━━━━━━━━━━&r &c&lSimpMC-Punish 帮助 &8&m━━━━━━━━━━━━━━━━━━━━",
            "",
            "&c/ban &7<玩家> [原因] &8- &f永久封禁玩家",
            "&c/tempban &7<玩家> <时长> [原因] &8- &f临时封禁玩家",
            "&c/unban &7<玩家> &8- &f解除玩家封禁",
            "&c/banlist &7[页码] &8- &f查看当前封禁列表",
            "",
            "&e/mute &7<玩家> [原因] &8- &f永久禁言玩家",
            "&e/tempmute &7<玩家> <时长> [原因] &8- &f临时禁言玩家",
            "&e/unmute &7<玩家> &8- &f解除玩家禁言",
            "",
            "&d/banip &7<玩家> [原因] &8- &f封禁玩家 IP",
            "&d/tempbanip &7<玩家> <时长> [原因] &8- &f临时封禁玩家 IP",
            "&d/unbanip &7<玩家> &8- &f解除玩家 IP 封禁",
            "",
            "&b/muteip &7<玩家> [原因] &8- &f禁言玩家 IP",
            "&b/tempmuteip &7<玩家> <时长> [原因] &8- &f临时禁言玩家 IP",
            "&b/unmuteip &7<玩家> &8- &f解除玩家 IP 禁言",
            "",
            "&6/kick &7<玩家> [原因] &8- &f踢出玩家",
            "&a/punish &7<玩家> &8- &f打开处罚菜单界面",
            "&a/history &7<玩家> &8- &f查看处罚历史",
            "",
            "&c/simpmc-punish 重载 &8- &f重载配置",
            "&c/simpmc-punish 帮助 &8- &f显示帮助菜单",
            "",
            "&7时长格式: &f1s, 30m, 6h, 7d, 4w, 1M, 1y",
            "&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
            ""
        };
        for (String line : helpLines) {
            sender.sendMessage(MessageUtil.toComponent(line));
        }
    }

    private void sendBanList(CommandSender sender, int page, PunishmentPage result) {
        int totalPages = result.total() == 0 ? 1 : (result.total() - 1) / BAN_LIST_PAGE_SIZE + 1;
        if (page > totalPages) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("banlist.page-out-of-range",
                "{pages}", String.valueOf(totalPages))));
            return;
        }

        sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("banlist.header",
            "{page}", String.valueOf(page),
            "{pages}", String.valueOf(totalPages),
            "{total}", String.valueOf(result.total()))));
        if (result.punishments().isEmpty()) {
            sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("banlist.empty")));
        } else {
            int index = (page - 1) * BAN_LIST_PAGE_SIZE + 1;
            for (Punishment punishment : result.punishments()) {
                String target = punishment.getTargetName();
                if (target == null || target.isBlank()) {
                    target = punishment.getTargetIp() != null ? punishment.getTargetIp() : "未知目标";
                } else if (punishment.getTargetIp() != null && !punishment.getTargetIp().isBlank()) {
                    target += " (IP: " + punishment.getTargetIp() + ")";
                }
                List<String> lines = this.msg().getMessageList("banlist.entry",
                    "{index}", String.valueOf(index++),
                    "{target}", target,
                    "{type}", punishment.getType().getDisplayName(),
                    "{staff}", punishment.getStaffName() != null ? punishment.getStaffName() : "控制台",
                    "{reason}", punishment.getReason() != null ? punishment.getReason() : "未填写原因",
                    "{created}", TimeUtil.formatDate(punishment.getCreatedAt()),
                    "{expires}", TimeUtil.formatRemaining(punishment.getExpiresAt()));
                for (String line : lines) {
                    sender.sendMessage(MessageUtil.toComponent(line));
                }
            }
        }
        sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("banlist.footer",
            "{page}", String.valueOf(page),
            "{pages}", String.valueOf(totalPages))));
    }

    private void runForSender(CommandSender sender, Runnable task) {
        if (sender instanceof Player player) {
            this.plugin.getSchedulerManager().runForEntity(player, task);
        } else {
            this.plugin.getSchedulerManager().runSync(task);
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
        String bypassPerm = this.plugin.getConfig().getString("bypass.permission", "simpmc-punish.bypass");
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
        String finalReason = reason != null ? reason : "未填写原因";
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
            this.plugin.getLogger().severe("执行处罚失败: " + e.getMessage());
            return null;
        });
    }

    private void broadcastStaff(String message) {
        String prefix = this.msg().getPrefix();
        String formattedMessage = prefix + message;
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        for (Player player : players) {
            if (!player.hasPermission("simpmc-punish.staff")) continue;
            player.sendMessage(MessageUtil.toComponent(formattedMessage));
        }
        Bukkit.getConsoleSender().sendMessage(MessageUtil.toComponent(formattedMessage));
    }

    private void executeIpPunishment(CommandSender sender, String targetName, PunishmentType type, long durationMs, String reason) {
        Player onlineTarget = Bukkit.getPlayer((String)targetName);
        if (onlineTarget != null) {
            String bypassPerm = this.plugin.getConfig().getString("bypass.permission", "simpmc-punish.bypass");
            if (onlineTarget.hasPermission(bypassPerm)) {
                sender.sendMessage(MessageUtil.toComponent(this.msg().getMessage("errors.cannot-punish")));
                return;
            }
            String ipAddress = PunishmentCommands.getPlayerIp(onlineTarget);
            if (ipAddress == null) {
                sender.sendMessage(MessageUtil.toComponent("&c无法解析该玩家的 IP 地址。"));
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
                    sender.sendMessage(MessageUtil.toComponent("&c没有找到 " + targetName + " 的历史 IP 记录。"));
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
        String finalReason = reason != null ? reason : "未填写原因";
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
            this.plugin.getLogger().severe("执行 IP 处罚失败: " + e.getMessage());
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


