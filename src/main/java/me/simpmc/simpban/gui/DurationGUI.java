package me.simpmc.simpban.gui;

import java.util.List;
import me.simpmc.simpban.SimpBan;
import me.simpmc.simpban.gui.CustomDurationHandler;
import me.simpmc.simpban.gui.PunishmentGUI;
import me.simpmc.simpban.gui.ReasonInputHandler;
import fr.mrmicky.fastinv.FastInv;
import fr.mrmicky.fastinv.ItemBuilder;
import me.simpmc.simpban.model.PunishmentType;
import me.simpmc.simpban.util.MessageUtil;
import me.simpmc.simpban.util.MessagesManager;
import me.simpmc.simpban.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DurationGUI
extends FastInv {
    private static ItemStack GLASS_PANE;
    private static ItemStack BACK_BUTTON;
    private static ItemStack PERM_ITEM;
    private static ItemStack CUSTOM_ITEM;
    private static ItemStack HOUR_1;
    private static ItemStack HOURS_6;
    private static ItemStack DAY_1;
    private static ItemStack DAYS_3;
    private static ItemStack WEEK_1;
    private static ItemStack DAYS_30;
    private static ItemStack DAYS_90;
    private static long DURATION_1H;
    private static long DURATION_6H;
    private static long DURATION_1D;
    private static long DURATION_3D;
    private static long DURATION_7D;
    private static long DURATION_30D;
    private static long DURATION_90D;
    private final SimpBan plugin;
    private final Player staff;
    private final OfflinePlayer target;
    private final PunishmentType type;

    public static void initializeIcons() {
        MessagesManager msg = SimpBan.getInstance().getMessagesManager();
        DURATION_1H = TimeUtil.parseDuration("1h");
        DURATION_6H = TimeUtil.parseDuration("6h");
        DURATION_1D = TimeUtil.parseDuration("1d");
        DURATION_3D = TimeUtil.parseDuration("3d");
        DURATION_7D = TimeUtil.parseDuration("7d");
        DURATION_30D = TimeUtil.parseDuration("30d");
        DURATION_90D = TimeUtil.parseDuration("90d");
        GLASS_PANE = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        List<String> backLore = msg.getMessageList("gui.duration.items.back.lore");
        BACK_BUTTON = new ItemBuilder(Material.ARROW).name(MessageUtil.colorize(msg.getMessage("gui.duration.items.back.name"))).lore((String[])backLore.stream().map(MessageUtil::colorize).toArray(String[]::new)).build();
        List<String> permLore = msg.getMessageList("gui.duration.items.permanent.lore");
        PERM_ITEM = new ItemBuilder(Material.BEDROCK).name(MessageUtil.colorize(msg.getMessage("gui.duration.items.permanent.name"))).lore((String[])permLore.stream().map(MessageUtil::colorize).toArray(String[]::new)).build();
        List<String> customLore = msg.getMessageList("gui.duration.items.custom.lore");
        CUSTOM_ITEM = new ItemBuilder(Material.NAME_TAG).name(MessageUtil.colorize(msg.getMessage("gui.duration.items.custom.name"))).lore((String[])customLore.stream().map(MessageUtil::colorize).toArray(String[]::new)).build();
        HOUR_1 = DurationGUI.createDurationItem(msg, "1h", Material.LIME_DYE, DURATION_1H);
        HOURS_6 = DurationGUI.createDurationItem(msg, "6h", Material.YELLOW_DYE, DURATION_6H);
        DAY_1 = DurationGUI.createDurationItem(msg, "1d", Material.ORANGE_DYE, DURATION_1D);
        DAYS_3 = DurationGUI.createDurationItem(msg, "3d", Material.RED_DYE, DURATION_3D);
        WEEK_1 = DurationGUI.createDurationItem(msg, "7d", Material.PURPLE_DYE, DURATION_7D);
        DAYS_30 = DurationGUI.createDurationItem(msg, "30d", Material.MAGENTA_DYE, DURATION_30D);
        DAYS_90 = DurationGUI.createDurationItem(msg, "90d", Material.BLUE_DYE, DURATION_90D);
    }

    private static ItemStack createDurationItem(MessagesManager msg, String key, Material material, long durationMs) {
        String name = msg.getMessage("gui.duration.items." + key + ".name");
        List<String> lore = msg.getMessageList("gui.duration.items." + key + ".lore", "{duration}", TimeUtil.formatDuration(durationMs));
        return new ItemBuilder(material).name(MessageUtil.colorize(name)).lore((String[])lore.stream().map(MessageUtil::colorize).toArray(String[]::new)).build();
    }

    public DurationGUI(SimpBan plugin, Player staff, OfflinePlayer target, PunishmentType type) {
        super(36, MessageUtil.colorize(plugin.getMessagesManager().getMessage("gui.duration.title", "{player}", target.getName())));
        this.plugin = plugin;
        this.staff = staff;
        this.target = target;
        this.type = type;
    }

    public void build() {
        MessagesManager msg = this.plugin.getMessagesManager();
        for (int i = 0; i < 9; ++i) {
            this.setItem(i, GLASS_PANE);
            this.setItem(27 + i, GLASS_PANE);
        }
        this.setItem(10, HOUR_1, e -> this.selectDuration(DURATION_1H));
        this.setItem(11, HOURS_6, e -> this.selectDuration(DURATION_6H));
        this.setItem(12, DAY_1, e -> this.selectDuration(DURATION_1D));
        this.setItem(13, DAYS_3, e -> this.selectDuration(DURATION_3D));
        this.setItem(14, WEEK_1, e -> this.selectDuration(DURATION_7D));
        this.setItem(15, DAYS_30, e -> this.selectDuration(DURATION_30D));
        this.setItem(16, DAYS_90, e -> this.selectDuration(DURATION_90D));
        this.setItem(22, PERM_ITEM, e -> {
            PunishmentType permType;
            this.staff.closeInventory();
            PunishmentType punishmentType = permType = this.type == PunishmentType.TEMPBAN ? PunishmentType.BAN : PunishmentType.MUTE;
            if (!this.staff.hasPermission(permType.getPermission())) {
                this.staff.sendMessage(MessageUtil.toComponent(msg.getMessage("input.custom-duration.no-permission-permanent")));
                return;
            }
            new ReasonInputHandler(this.plugin, this.staff, this.target, permType, -1L).start();
        });
        this.setItem(31, CUSTOM_ITEM, e -> {
            this.staff.closeInventory();
            new CustomDurationHandler(this.plugin, this.staff, this.target, this.type).start();
        });
        this.setItem(27, BACK_BUTTON, e -> PunishmentGUI.openAsync(this.plugin, this.staff, this.target));
    }

    private void selectDuration(long durationMs) {
        this.staff.closeInventory();
        new ReasonInputHandler(this.plugin, this.staff, this.target, this.type, durationMs).start();
    }

    public void open() {
        this.open(this.staff);
    }

    public static void openAsync(SimpBan plugin, Player staff, OfflinePlayer target, PunishmentType type) {
        plugin.getSchedulerManager().runAsync(() -> {
            DurationGUI gui = new DurationGUI(plugin, staff, target, type);
            gui.build();
            plugin.getSchedulerManager().runForEntity((Entity)staff, gui::open);
        });
    }
}


