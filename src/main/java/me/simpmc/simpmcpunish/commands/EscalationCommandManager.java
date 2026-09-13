package me.simpmc.simpmcpunish.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import me.simpmc.simpmcpunish.SimpMCPunish;
import me.simpmc.simpmcpunish.model.Punishment;
import me.simpmc.simpmcpunish.model.PunishmentType;
import me.simpmc.simpmcpunish.util.MessageUtil;
import me.simpmc.simpmcpunish.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

public final class EscalationCommandManager {
    private static final Pattern COMMAND_NAME = Pattern.compile("[a-z0-9_:-]+");
    private final SimpMCPunish plugin;
    private final Map<String, EscalationCommand> commands = new LinkedHashMap<>();

    public EscalationCommandManager(SimpMCPunish plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        this.unregisterAll();
        ConfigurationSection section = this.plugin.getConfig().getConfigurationSection("escalation.commands");
        if (section == null) {
            return;
        }
        CommandMap commandMap = Bukkit.getCommandMap();
        for (String configuredName : section.getKeys(false)) {
            String commandName = configuredName.toLowerCase(Locale.ROOT);
            ConfigurationSection commandSection = section.getConfigurationSection(configuredName);
            if (commandSection == null || !commandSection.getBoolean("enabled", true)) {
                continue;
            }
            if (!COMMAND_NAME.matcher(commandName).matches()) {
                this.plugin.getLogger().warning("忽略无效的自定义处罚命令名: " + configuredName);
                continue;
            }
            NavigableMap<Integer, String> steps = this.readSteps(commandSection);
            if (steps.isEmpty()) {
                this.plugin.getLogger().warning("自定义处罚命令 /" + commandName + " 没有配置 steps");
                continue;
            }
            if (commandMap.getCommand(commandName) != null) {
                this.plugin.getLogger().warning("自定义处罚命令 /" + commandName + " 已被其他命令占用");
                continue;
            }
            String permission = commandSection.getString("permission", "simpmc-punish.escalation." + commandName);
            this.registerPermission(permission);
            EscalationCommand command = new EscalationCommand(
                commandName,
                commandSection.getString("description", "按次数递进处罚"),
                commandSection.getString("usage", "/" + commandName + " <玩家> [原因]"),
                commandSection.getStringList("aliases"),
                permission,
                steps
            );
            if (commandMap.register(this.plugin.getName().toLowerCase(Locale.ROOT), command)) {
                this.commands.put(commandName, command);
                this.plugin.getLogger().info("已注册自定义处罚命令: /" + commandName);
            } else {
                this.plugin.getLogger().warning("注册自定义处罚命令失败: /" + commandName);
            }
        }
    }

    public void unregisterAll() {
        CommandMap commandMap = Bukkit.getCommandMap();
        for (EscalationCommand command : this.commands.values()) {
            command.unregister(commandMap);
            commandMap.getKnownCommands().entrySet().removeIf(entry -> entry.getValue() == command);
        }
        this.commands.clear();
    }

    private NavigableMap<Integer, String> readSteps(ConfigurationSection commandSection) {
        List<String> steps = commandSection.getStringList("steps");
        if (!steps.isEmpty()) {
            NavigableMap<Integer, String> result = new TreeMap<>();
            int index = 1;
            for (String step : steps) {
                if (step != null && !step.isBlank()) {
                    result.put(index, step);
                }
                index++;
            }
            return result;
        }
        ConfigurationSection numberedSteps = commandSection.getConfigurationSection("steps");
        if (numberedSteps == null) {
            return new TreeMap<>();
        }
        NavigableMap<Integer, String> result = new TreeMap<>();
        for (String key : numberedSteps.getKeys(false)) {
            try {
                int index = Integer.parseInt(key);
                String step = numberedSteps.getString(key);
                if (index > 0 && step != null && !step.isBlank()) {
                    result.put(index, step);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    private void registerPermission(String permissionName) {
        if (permissionName == null || permissionName.isBlank() || Bukkit.getPluginManager().getPermission(permissionName) != null) {
            return;
        }
        Bukkit.getPluginManager().addPermission(new Permission(permissionName, PermissionDefault.OP));
    }

    private final class EscalationCommand extends Command {
        private final String commandName;
        private final String permission;
        private final NavigableMap<Integer, String> steps;

        private EscalationCommand(String commandName, String description, String usage, List<String> aliases,
                                   String permission, NavigableMap<Integer, String> steps) {
            super(commandName, description, usage, aliases != null ? aliases : List.of());
            this.commandName = commandName;
            this.permission = permission;
            this.steps = Collections.unmodifiableNavigableMap(new TreeMap<>(steps));
            this.setPermission(permission);
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            if (this.permission != null && !this.permission.isBlank() && !sender.hasPermission(this.permission)) {
                sender.sendMessage(MessageUtil.toComponent("&c你没有权限执行该处罚命令。"));
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage(MessageUtil.toComponent(this.getUsage()));
                return true;
            }
            String targetName = args[0];
            String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null;
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            UUID staffUUID = sender instanceof org.bukkit.entity.Player player ? player.getUniqueId() : null;
            String staffName = sender.getName();
                EscalationCommandManager.this.plugin.getPunishmentManager().countBySourceCommand(target.getUniqueId(), this.commandName)
                .thenCompose(count -> {
                    Map.Entry<Integer, String> step = this.steps.floorEntry(count + 1);
                    if (step == null) {
                        step = this.steps.firstEntry();
                    }
                    String template = step != null ? step.getValue() : null;
                    ParsedAction action = this.parseAction(template, targetName, reason);
                    if (action == null) {
                        return java.util.concurrent.CompletableFuture.failedFuture(new IllegalArgumentException("自定义处罚步骤格式无效"));
                    }
                    return EscalationCommandManager.this.plugin.getPunishmentManager().executeConfiguredPunishment(
                        target.getUniqueId(), targetName, staffUUID, staffName, action.type(), action.durationMillis(), action.reason(), this.commandName
                    ).thenApply(punishment -> {
                        this.sendResult(sender, targetName, count + 1, punishment);
                        return punishment;
                    });
                })
                .exceptionally(exception -> {
                    Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
                    EscalationCommandManager.this.plugin.getSchedulerManager().runSync(() -> sender.sendMessage(MessageUtil.toComponent(
                        cause instanceof IllegalStateException ? "&c" + cause.getMessage() : "&c执行自定义处罚失败，请查看控制台日志。"
                    )));
                    EscalationCommandManager.this.plugin.getLogger().warning("执行自定义处罚命令 /" + this.commandName + " 失败: " + cause.getMessage());
                    return null;
                });
            return true;
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
            if (args.length != 1) {
                return List.of();
            }
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                .map(player -> player.getName())
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted()
                .toList();
        }

        private ParsedAction parseAction(String template, String targetName, String reason) {
            if (template == null || template.isBlank()) {
                return null;
            }
            String rendered;
            if (template.contains("{player}") || template.contains("<player>")) {
                rendered = template.replace("{player}", targetName).replace("<player>", targetName)
                    .replace("{reason}", reason != null ? reason : "").replace("<reason>", reason != null ? reason : "");
            } else {
                rendered = template + " " + targetName + (reason != null && !reason.isBlank() ? " " + reason : "");
            }
            String[] tokens = rendered.trim().split("\\s+");
            if (tokens.length == 0 || tokens[0].isBlank()) {
                return null;
            }
            String actionName = tokens[0].toLowerCase(Locale.ROOT);
            PunishmentType type = switch (actionName) {
                case "kick" -> PunishmentType.KICK;
                case "ban" -> PunishmentType.BAN;
                case "tempban" -> PunishmentType.TEMPBAN;
                case "mute" -> PunishmentType.MUTE;
                case "tempmute" -> PunishmentType.TEMPMUTE;
                case "warn" -> PunishmentType.WARN;
                case "tempwarn" -> PunishmentType.TEMPWARN;
                default -> null;
            };
            if (type == null) {
                return null;
            }
            int targetIndex = 1;
            long durationMillis = -1L;
            if (type.isTemporary()) {
                if (tokens.length <= targetIndex) {
                    return null;
                }
                durationMillis = TimeUtil.parseDuration(tokens[targetIndex++]);
                if (durationMillis <= 0L) {
                    return null;
                }
            }
            if (tokens.length <= targetIndex || !tokens[targetIndex].equals(targetName)) {
                return null;
            }
            String parsedReason = tokens.length > targetIndex + 1
                ? String.join(" ", Arrays.copyOfRange(tokens, targetIndex + 1, tokens.length)) : null;
            return new ParsedAction(type, durationMillis, parsedReason);
        }

        private void sendResult(CommandSender sender, String targetName, int count, Punishment punishment) {
            String message = plugin.getMessagesManager().getMessage("escalation.applied",
                "{staff}", sender.getName(),
                "{command}", this.commandName,
                "{player}", targetName,
                "{count}", String.valueOf(count),
                "{punishment}", punishment.getType().getDisplayName());
            plugin.getSchedulerManager().runSync(() -> {
                for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(MessageUtil.toComponent(message));
                }
                Bukkit.getConsoleSender().sendMessage(MessageUtil.toComponent(message));
            });
        }
    }

    private record ParsedAction(PunishmentType type, long durationMillis, String reason) {
    }
}
