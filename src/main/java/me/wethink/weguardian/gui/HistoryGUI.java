/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 */
package me.wethink.weguardian.gui;

import java.util.ArrayList;
import java.util.List;
import me.wethink.weguardian.WeGuardian;
import me.wethink.weguardian.gui.PunishmentGUI;
import fr.mrmicky.fastinv.FastInv;
import fr.mrmicky.fastinv.ItemBuilder;
import me.wethink.weguardian.model.Punishment;
import me.wethink.weguardian.model.PunishmentType;
import me.wethink.weguardian.util.MessageUtil;
import me.wethink.weguardian.util.MessagesManager;
import me.wethink.weguardian.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class HistoryGUI
extends FastInv {
    private static ItemStack GLASS_PANE;
    private static ItemStack PREV_PAGE;
    private static ItemStack NEXT_PAGE;
    private static ItemStack BACK_BUTTON;
    private static final int[] ITEM_SLOTS;
    private static final int ITEMS_PER_PAGE;
    private final WeGuardian plugin;
    private final Player staff;
    private final OfflinePlayer target;
    private final List<Punishment> history;
    private final int page;
    private ItemStack headItem;
    private final List<ItemStack> punishmentItems = new ArrayList<ItemStack>();

    public static void initializeIcons() {
        MessagesManager msg = WeGuardian.getInstance().getMessagesManager();
        GLASS_PANE = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        List<String> prevLore = msg.getMessageList("gui.history.items.prev-page.lore");
        PREV_PAGE = new ItemBuilder(Material.ARROW).name(MessageUtil.colorize(msg.getMessage("gui.history.items.prev-page.name"))).lore((String[])prevLore.stream().map(MessageUtil::colorize).toArray(String[]::new)).build();
        List<String> nextLore = msg.getMessageList("gui.history.items.next-page.lore");
        NEXT_PAGE = new ItemBuilder(Material.ARROW).name(MessageUtil.colorize(msg.getMessage("gui.history.items.next-page.name"))).lore((String[])nextLore.stream().map(MessageUtil::colorize).toArray(String[]::new)).build();
        List<String> backLore = msg.getMessageList("gui.history.items.back.lore");
        BACK_BUTTON = new ItemBuilder(Material.BARRIER).name(MessageUtil.colorize(msg.getMessage("gui.history.items.back.name"))).lore((String[])backLore.stream().map(MessageUtil::colorize).toArray(String[]::new)).build();
    }

    public HistoryGUI(WeGuardian plugin, Player staff, OfflinePlayer target, List<Punishment> history) {
        this(plugin, staff, target, history, 0);
    }

    public HistoryGUI(WeGuardian plugin, Player staff, OfflinePlayer target, List<Punishment> history, int page) {
        super(54, MessageUtil.colorize(plugin.getMessagesManager().getMessage("gui.history.title", "{player}", target.getName(), "{page}", String.valueOf(page + 1))));
        this.plugin = plugin;
        this.staff = staff;
        this.target = target;
        this.history = history;
        this.page = page;
    }

    public void build() {
        MessagesManager msg = this.plugin.getMessagesManager();
        long activeCount = this.history.stream().filter(Punishment::isActive).count();
        List<String> headLore = msg.getMessageList("gui.history.items.player-head.lore", "{total}", String.valueOf(this.history.size()), "{active}", String.valueOf(activeCount));
        this.headItem = new ItemBuilder(Material.PLAYER_HEAD).name(MessageUtil.colorize(msg.getMessage("gui.history.items.player-head.name", "{player}", this.target.getName()))).lore((String[])headLore.stream().map(MessageUtil::colorize).toArray(String[]::new)).build();
        int startIndex = this.page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, this.history.size());
        for (int i = startIndex; i < endIndex; ++i) {
            this.punishmentItems.add(this.createPunishmentItem(this.history.get(i)));
        }
        this.populateInventory();
    }

    private void populateInventory() {
        int maxPages;
        int i;
        for (i = 0; i < 9; ++i) {
            this.setItem(i, GLASS_PANE);
            this.setItem(45 + i, GLASS_PANE);
        }
        this.setItem(4, this.headItem);
        for (i = 0; i < this.punishmentItems.size() && i < ITEM_SLOTS.length; ++i) {
            this.setItem(ITEM_SLOTS[i], this.punishmentItems.get(i));
        }
        if (this.page > 0) {
            this.setItem(45, PREV_PAGE, e -> this.openPageAsync(this.page - 1));
        }
        if (this.page < (maxPages = (int)Math.ceil((double)this.history.size() / (double)ITEMS_PER_PAGE)) - 1) {
            this.setItem(53, NEXT_PAGE, e -> this.openPageAsync(this.page + 1));
        }
        this.setItem(49, BACK_BUTTON, e -> PunishmentGUI.openAsync(this.plugin, this.staff, this.target));
    }

    private void openPageAsync(int newPage) {
        this.staff.closeInventory();
        this.plugin.getSchedulerManager().runAsync(() -> {
            HistoryGUI gui = new HistoryGUI(this.plugin, this.staff, this.target, this.history, newPage);
            gui.build();
            this.plugin.getSchedulerManager().runForEntity((Entity)this.staff, gui::open);
        });
    }

    private ItemStack createPunishmentItem(Punishment punishment) {
        String statusColor;
        MessagesManager msg = this.plugin.getMessagesManager();
        Material material = HistoryGUI.getMaterialForType(punishment.getType());
        String string = statusColor = punishment.isActive() ? "&a" : "&c";
        String status = punishment.isActive() ? (punishment.isExpired() ? "Expired" : "Active") : "Removed";
        String durationText = punishment.getType().isTemporary() ? TimeUtil.formatRemaining(punishment.getExpiresAt()) : msg.getMessage("duration-permanent");
        ArrayList<String> lore = new ArrayList<String>(12);
        lore.add("");
        lore.add(MessageUtil.colorize("&7Type: " + punishment.getType().getColor() + punishment.getType().getDisplayName()));
        lore.add(MessageUtil.colorize("&7Reason: &f" + (punishment.getReason() != null ? punishment.getReason() : "None")));
        lore.add(MessageUtil.colorize("&7By: &f" + punishment.getStaffName()));
        lore.add(MessageUtil.colorize("&7Date: &f" + TimeUtil.formatDate(punishment.getCreatedAt())));
        lore.add("");
        lore.add(MessageUtil.colorize("&7Duration: &f" + durationText));
        lore.add(MessageUtil.colorize("&7Status: " + statusColor + status));
        if (!punishment.isActive() && punishment.getRemovedByName() != null) {
            lore.add("");
            lore.add(MessageUtil.colorize("&7Removed by: &f" + punishment.getRemovedByName()));
            lore.add(MessageUtil.colorize("&7Removed at: &f" + TimeUtil.formatDate(punishment.getRemovedAt())));
        }
        return new ItemBuilder(material).name(MessageUtil.colorize(punishment.getType().getColor() + "&l" + punishment.getType().getDisplayName() + " &7#" + punishment.getId())).lore(lore.toArray(new String[0])).build();
    }

    private static Material getMaterialForType(PunishmentType type) {
        return switch (type) {
            default -> throw new MatchException(null, null);
            case PunishmentType.BAN -> Material.RED_WOOL;
            case PunishmentType.TEMPBAN -> Material.ORANGE_WOOL;
            case PunishmentType.BANIP -> Material.RED_CONCRETE;
            case PunishmentType.TEMPBANIP -> Material.ORANGE_CONCRETE;
            case PunishmentType.MUTE -> Material.YELLOW_WOOL;
            case PunishmentType.TEMPMUTE -> Material.LIME_WOOL;
            case PunishmentType.MUTEIP -> Material.YELLOW_CONCRETE;
            case PunishmentType.TEMPMUTEIP -> Material.LIME_CONCRETE;
            case PunishmentType.KICK -> Material.LIGHT_BLUE_WOOL;
        };
    }

    public void open() {
        this.open(this.staff);
    }

    public static void openAsync(WeGuardian plugin, Player staff, OfflinePlayer target) {
        staff.sendMessage(MessageUtil.toComponent(plugin.getMessagesManager().getMessage("input.reason.loading-history")));
        plugin.getPunishmentManager().getHistory(target.getUniqueId()).thenAccept(history -> {
            HistoryGUI gui = new HistoryGUI(plugin, staff, target, (List<Punishment>)history);
            gui.build();
            plugin.getSchedulerManager().runForEntity((Entity)staff, gui::open);
        });
    }

    static {
        ITEM_SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16, 17, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29, 30, 31, 32, 33, 34, 35, 37, 38, 39, 40, 41, 42, 43, 44};
        ITEMS_PER_PAGE = ITEM_SLOTS.length;
    }
}


