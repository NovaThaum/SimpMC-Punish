/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.inventory.ItemStack
 */
package me.wethink.weguardian.gui;

import java.util.List;
import java.util.function.Consumer;
import me.wethink.weguardian.WeGuardian;
import me.wethink.weguardian.gui.DurationGUI;
import me.wethink.weguardian.gui.HistoryGUI;
import me.wethink.weguardian.gui.ReasonInputHandler;
import fr.mrmicky.fastinv.FastInv;
import fr.mrmicky.fastinv.ItemBuilder;
import me.wethink.weguardian.model.Punishment;
import me.wethink.weguardian.model.PunishmentType;
import me.wethink.weguardian.util.MessageUtil;
import me.wethink.weguardian.util.MessagesManager;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class PunishmentGUI
extends FastInv {
    private static ItemStack GLASS_PANE;
    private static ItemStack BAN_ITEM;
    private static ItemStack TEMP_BAN_ITEM;
    private static ItemStack KICK_ITEM;
    private static ItemStack MUTE_ITEM;
    private static ItemStack TEMP_MUTE_ITEM;
    private static ItemStack HISTORY_ITEM;
    private static ItemStack CLOSE_ITEM;
    private static final Consumer<InventoryClickEvent> CLOSE_HANDLER;
    private final WeGuardian plugin;
    private final Player staff;
    private final OfflinePlayer target;

    public static void initializeIcons() {
        MessagesManager msg = WeGuardian.getInstance().getMessagesManager();
        GLASS_PANE = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        List<String> banLore = msg.getMessageList("gui.punish.items.ban.lore");
        BAN_ITEM = new ItemBuilder(Material.BARRIER).name(MessageUtil.colorize(msg.getMessage("gui.punish.items.ban.name"))).lore((String[])banLore.stream().map(MessageUtil::colorize).toArray(String[]::new)).build();
        List<String> tempbanLore = msg.getMessageList("gui.punish.items.tempban.lore");
        TEMP_BAN_ITEM = new ItemBuilder(Material.CLOCK).name(MessageUtil.colorize(msg.getMessage("gui.punish.items.tempban.name"))).lore((String[])tempbanLore.stream().map(MessageUtil::colorize).toArray(String[]::new)).build();
        List<String> kickLore = msg.getMessageList("gui.punish.items.kick.lore");
        KICK_ITEM = new ItemBuilder(Material.LEATHER_BOOTS).name(MessageUtil.colorize(msg.getMessage("gui.punish.items.kick.name"))).lore((String[])kickLore.stream().map(MessageUtil::colorize).toArray(String[]::new)).build();
        List<String> muteLore = msg.getMessageList("gui.punish.items.mute.lore");
        MUTE_ITEM = new ItemBuilder(Material.PAPER).name(MessageUtil.colorize(msg.getMessage("gui.punish.items.mute.name"))).lore((String[])muteLore.stream().map(MessageUtil::colorize).toArray(String[]::new)).build();
        List<String> tempmuteLore = msg.getMessageList("gui.punish.items.tempmute.lore");
        TEMP_MUTE_ITEM = new ItemBuilder(Material.WRITABLE_BOOK).name(MessageUtil.colorize(msg.getMessage("gui.punish.items.tempmute.name"))).lore((String[])tempmuteLore.stream().map(MessageUtil::colorize).toArray(String[]::new)).build();
        List<String> historyLore = msg.getMessageList("gui.punish.items.history.lore");
        HISTORY_ITEM = new ItemBuilder(Material.BOOK).name(MessageUtil.colorize(msg.getMessage("gui.punish.items.history.name"))).lore((String[])historyLore.stream().map(MessageUtil::colorize).toArray(String[]::new)).build();
        List<String> closeLore = msg.getMessageList("gui.punish.items.close.lore");
        CLOSE_ITEM = new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name(MessageUtil.colorize(msg.getMessage("gui.punish.items.close.name"))).lore((String[])closeLore.stream().map(MessageUtil::colorize).toArray(String[]::new)).build();
    }

    public PunishmentGUI(WeGuardian plugin, Player staff, OfflinePlayer target) {
        super(45, MessageUtil.colorize(plugin.getMessagesManager().getMessage("gui.punish.title", "{player}", target.getName())));
        this.plugin = plugin;
        this.staff = staff;
        this.target = target;
    }

    public void build() {
        MessagesManager msg = this.plugin.getMessagesManager();
        for (int i = 0; i < 9; ++i) {
            this.setItem(i, GLASS_PANE);
            this.setItem(36 + i, GLASS_PANE);
        }
        this.setItem(9, GLASS_PANE);
        this.setItem(17, GLASS_PANE);
        this.setItem(18, GLASS_PANE);
        this.setItem(26, GLASS_PANE);
        this.setItem(27, GLASS_PANE);
        this.setItem(35, GLASS_PANE);
        if (this.staff.hasPermission(PunishmentType.BAN.getPermission())) {
            this.setItem(11, BAN_ITEM, e -> this.openReasonInput(PunishmentType.BAN, -1L));
        }
        if (this.staff.hasPermission(PunishmentType.TEMPBAN.getPermission())) {
            this.setItem(12, TEMP_BAN_ITEM, e -> this.openDurationGUIAsync(PunishmentType.TEMPBAN));
        }
        if (this.staff.hasPermission(PunishmentType.KICK.getPermission())) {
            this.setItem(13, KICK_ITEM, e -> this.openReasonInput(PunishmentType.KICK, -1L));
        }
        if (this.staff.hasPermission(PunishmentType.MUTE.getPermission())) {
            this.setItem(14, MUTE_ITEM, e -> this.openReasonInput(PunishmentType.MUTE, -1L));
        }
        if (this.staff.hasPermission(PunishmentType.TEMPMUTE.getPermission())) {
            this.setItem(15, TEMP_MUTE_ITEM, e -> this.openDurationGUIAsync(PunishmentType.TEMPMUTE));
        }
        this.setItem(31, HISTORY_ITEM, e -> {
            this.staff.closeInventory();
            this.staff.sendMessage(MessageUtil.toComponent(msg.getMessage("input.reason.loading-history")));
            this.plugin.getPunishmentManager().getHistory(this.target.getUniqueId()).thenAccept(history -> {
                HistoryGUI gui = new HistoryGUI(this.plugin, this.staff, this.target, (List<Punishment>)history);
                gui.build();
                this.plugin.getSchedulerManager().runForEntity((Entity)this.staff, gui::open);
            });
        });
        this.setItem(40, CLOSE_ITEM, CLOSE_HANDLER);
        String onlineStatus = this.target.isOnline() ? msg.getMessage("online-yes") : msg.getMessage("online-no");
        List<String> headLore = msg.getMessageList("gui.punish.items.player-head.lore", "{uuid}", this.target.getUniqueId().toString().substring(0, 8) + "...", "{online}", MessageUtil.colorize(onlineStatus));
        ItemStack headItem = new ItemBuilder(Material.PLAYER_HEAD).name(MessageUtil.colorize(msg.getMessage("gui.punish.items.player-head.name", "{player}", this.target.getName()))).lore((String[])headLore.stream().map(MessageUtil::colorize).toArray(String[]::new)).build();
        this.setItem(4, headItem);
    }

    private void openDurationGUIAsync(PunishmentType type) {
        if (!this.staff.hasPermission(type.getPermission())) {
            this.staff.closeInventory();
            this.staff.sendMessage(MessageUtil.toComponent(this.plugin.getMessagesManager().getMessage("input.reason.no-permission")));
            return;
        }
        this.staff.closeInventory();
        this.plugin.getSchedulerManager().runAsync(() -> {
            DurationGUI gui = new DurationGUI(this.plugin, this.staff, this.target, type);
            gui.build();
            this.plugin.getSchedulerManager().runForEntity((Entity)this.staff, gui::open);
        });
    }

    private void openReasonInput(PunishmentType type, long durationMs) {
        if (!this.staff.hasPermission(type.getPermission())) {
            this.staff.closeInventory();
            this.staff.sendMessage(MessageUtil.toComponent(this.plugin.getMessagesManager().getMessage("input.reason.no-permission")));
            return;
        }
        this.staff.closeInventory();
        new ReasonInputHandler(this.plugin, this.staff, this.target, type, durationMs).start();
    }

    public void open() {
        this.open(this.staff);
    }

    public static void openAsync(WeGuardian plugin, Player staff, OfflinePlayer target) {
        plugin.getSchedulerManager().runAsync(() -> {
            PunishmentGUI gui = new PunishmentGUI(plugin, staff, target);
            gui.build();
            plugin.getSchedulerManager().runForEntity((Entity)staff, gui::open);
        });
    }

    static {
        CLOSE_HANDLER = e -> e.getWhoClicked().closeInventory();
    }
}


