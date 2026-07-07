/*
 * Decompiled with CFR 0.152.
 */
package me.wethink.weguardian.model;

public enum PunishmentType {
    BAN("Ban", "\u00a7c", true),
    TEMPBAN("Temporary Ban", "\u00a76", true),
    BANIP("IP Ban", "\u00a74", true),
    TEMPBANIP("Temporary IP Ban", "\u00a7c", true),
    MUTE("Mute", "\u00a7e", false),
    TEMPMUTE("Temporary Mute", "\u00a7a", false),
    MUTEIP("IP Mute", "\u00a76", false),
    TEMPMUTEIP("Temporary IP Mute", "\u00a7e", false),
    KICK("Kick", "\u00a79", false);

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
        return this == TEMPBAN || this == TEMPMUTE || this == TEMPBANIP || this == TEMPMUTEIP;
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

    public String getPermission() {
        return switch (this.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> "weguardian.ban";
            case 1 -> "weguardian.tempban";
            case 2 -> "weguardian.banip";
            case 3 -> "weguardian.tempbanip";
            case 4 -> "weguardian.mute";
            case 5 -> "weguardian.tempmute";
            case 6 -> "weguardian.muteip";
            case 7 -> "weguardian.tempmuteip";
            case 8 -> "weguardian.kick";
        };
    }
}


