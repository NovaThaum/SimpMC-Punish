package me.simpmc.simpmcpunish.model;

public enum PunishmentType {
    BAN("永久封禁", "\u00a7c", true),
    TEMPBAN("临时封禁", "\u00a76", true),
    BANIP("IP 封禁", "\u00a74", true),
    TEMPBANIP("临时 IP 封禁", "\u00a7c", true),
    MUTE("永久禁言", "\u00a7e", false),
    TEMPMUTE("临时禁言", "\u00a7a", false),
    MUTEIP("IP 禁言", "\u00a76", false),
    TEMPMUTEIP("临时 IP 禁言", "\u00a7e", false),
    WARN("警告", "\u00a7e", false),
    TEMPWARN("临时警告", "\u00a76", false),
    KICK("踢出", "\u00a79", false);

    private final String displayName;
    private final String color;
    private final boolean preventLogin;

    private PunishmentType(String displayName, String color, boolean preventLogin) {
        this.displayName = displayName;
        this.color = color;
        this.preventLogin = preventLogin;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getColor() {
        return this.color;
    }

    public boolean preventsLogin() {
        return this.preventLogin;
    }

    public boolean isTemporary() {
        return this == TEMPBAN || this == TEMPMUTE || this == TEMPBANIP || this == TEMPMUTEIP
                || this == TEMPWARN;
    }

    public boolean isBan() {
        return this == BAN || this == TEMPBAN;
    }

    public boolean isMute() {
        return this == MUTE || this == TEMPMUTE;
    }

    public boolean isIpBan() {
        return this == BANIP || this == TEMPBANIP;
    }

    public boolean isIpMute() {
        return this == MUTEIP || this == TEMPMUTEIP;
    }

    public boolean isIpBased() {
        return this.isIpBan() || this.isIpMute();
    }

    public boolean isWarning() {
        return this == WARN || this == TEMPWARN;
    }

    public String getPermission() {
        return switch (this) {
            case BAN -> "simpmc-punish.ban";
            case TEMPBAN -> "simpmc-punish.tempban";
            case BANIP -> "simpmc-punish.banip";
            case TEMPBANIP -> "simpmc-punish.tempbanip";
            case MUTE -> "simpmc-punish.mute";
            case TEMPMUTE -> "simpmc-punish.tempmute";
            case MUTEIP -> "simpmc-punish.muteip";
            case TEMPMUTEIP -> "simpmc-punish.tempmuteip";
            case WARN -> "simpmc-punish.warn";
            case TEMPWARN -> "simpmc-punish.tempwarn";
            case KICK -> "simpmc-punish.kick";
        };
    }
}


