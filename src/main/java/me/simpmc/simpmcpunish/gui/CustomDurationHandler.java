package me.simpmc.simpmcpunish.gui;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.List;
import java.util.concurrent.TimeUnit;
import me.simpmc.simpmcpunish.SimpMCPunish;
import me.simpmc.simpmcpunish.gui.PunishmentGUI;
import me.simpmc.simpmcpunish.gui.ReasonInputHandler;
import me.simpmc.simpmcpunish.model.PunishmentType;
import me.simpmc.simpmcpunish.util.MessageUtil;
import me.simpmc.simpmcpunish.util.MessagesManager;
import me.simpmc.simpmcpunish.util.TimeUtil;
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

public class CustomDurationHandler
implements Listener {
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();
    private final SimpMCPunish plugin;
    private final Player staff;
    private final OfflinePlayer target;
    private final PunishmentType type;
    private volatile boolean completed = false;

    public CustomDurationHandler(SimpMCPunish plugin, Player staff, OfflinePlayer target, PunishmentType type) {
        this.plugin = plugin;
        this.staff = staff;
        this.target = target;
        this.type = type;
    }

    public void start() {
        MessagesManager msg = this.plugin.getMessagesManager();
        this.plugin.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)this.plugin);
        this.staff.sendMessage(MessageUtil.toComponent(""));
        this.staff.sendMessage(MessageUtil.toComponent(msg.getMessage("input.custom-duration.header")));
        this.staff.sendMessage(MessageUtil.toComponent(msg.getMessage("input.custom-duration.title")));
        this.staff.sendMessage(MessageUtil.toComponent(""));
        this.staff.sendMessage(MessageUtil.toComponent(msg.getMessage("input.custom-duration.format-header")));
        List<String> formats = msg.getMessageList("input.custom-duration.formats");
        for (String format : formats) {
            this.staff.sendMessage(MessageUtil.toComponent(format));
        }
        this.staff.sendMessage(MessageUtil.toComponent(""));
        this.staff.sendMessage(MessageUtil.toComponent(msg.getMessage("input.custom-duration.instruction")));
        this.staff.sendMessage(MessageUtil.toComponent(msg.getMessage("input.custom-duration.header")));
        this.plugin.getSchedulerManager().runAsyncLater(this::handleTimeout, 60L, TimeUnit.SECONDS);
    }

    private void handleTimeout() {
        if (!this.completed) {
            this.cleanup();
            this.plugin.getSchedulerManager().runForEntity((Entity)this.staff, () -> this.staff.sendMessage(MessageUtil.toComponent(this.plugin.getMessagesManager().getMessage("input.custom-duration.timeout"))));
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {
        if (!event.getPlayer().getUniqueId().equals(this.staff.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        String message = PLAIN_SERIALIZER.serialize(event.message());
        MessagesManager msg = this.plugin.getMessagesManager();
        if (message.equalsIgnoreCase("cancel") || message.equals("取消")) {
            this.cleanup();
            this.staff.sendMessage(MessageUtil.toComponent(msg.getMessage("input.custom-duration.cancelled")));
            PunishmentGUI.openAsync(this.plugin, this.staff, this.target);
            return;
        }
        long durationMs = TimeUtil.parseDuration(message);
        if (durationMs <= 0L) {
            this.staff.sendMessage(MessageUtil.toComponent(msg.getMessage("input.custom-duration.invalid")));
            return;
        }
        this.completed = true;
        this.cleanup();
        this.staff.sendMessage(MessageUtil.toComponent(msg.getMessage("input.custom-duration.success", "{duration}", TimeUtil.formatDuration(durationMs))));
        new ReasonInputHandler(this.plugin, this.staff, this.target, this.type, durationMs).start();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event.getPlayer().getUniqueId().equals(this.staff.getUniqueId())) {
            this.cleanup();
        }
    }

    private void cleanup() {
        this.completed = true;
        HandlerList.unregisterAll((Listener)this);
    }
}


