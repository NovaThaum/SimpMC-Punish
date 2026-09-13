package me.simpmc.simpmcpunish.model;

import java.time.Instant;
import java.util.UUID;
import me.simpmc.simpmcpunish.model.PunishmentType;

public class Punishment {
    private int id;
    private UUID targetUUID;
    private String targetName;
    private String targetIp;
    private UUID staffUUID;
    private String staffName;
    private PunishmentType type;
    private String reason;
    private String sourceCommand;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean active;
    private UUID removedByUUID;
    private String removedByName;
    private Instant removedAt;
    private String removeReason;

    public Punishment() {
    }

    public Punishment(UUID targetUUID, String targetName, UUID staffUUID, String staffName, PunishmentType type, String reason, Instant createdAt, Instant expiresAt) {
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.staffUUID = staffUUID;
        this.staffName = staffName;
        this.type = type;
        this.reason = reason;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.active = true;
    }

    public Punishment(UUID targetUUID, String targetName, String targetIp, UUID staffUUID, String staffName, PunishmentType type, String reason, Instant createdAt, Instant expiresAt) {
        this(targetUUID, targetName, staffUUID, staffName, type, reason, createdAt, expiresAt);
        this.targetIp = targetIp;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UUID getTargetUUID() {
        return this.targetUUID;
    }

    public void setTargetUUID(UUID targetUUID) {
        this.targetUUID = targetUUID;
    }

    public String getTargetName() {
        return this.targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getTargetIp() {
        return this.targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public UUID getStaffUUID() {
        return this.staffUUID;
    }

    public void setStaffUUID(UUID staffUUID) {
        this.staffUUID = staffUUID;
    }

    public String getStaffName() {
        return this.staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public PunishmentType getType() {
        return this.type;
    }

    public void setType(PunishmentType type) {
        this.type = type;
    }

    public String getReason() {
        return this.reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSourceCommand() {
        return this.sourceCommand;
    }

    public void setSourceCommand(String sourceCommand) {
        this.sourceCommand = sourceCommand;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return this.expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public UUID getRemovedByUUID() {
        return this.removedByUUID;
    }

    public void setRemovedByUUID(UUID removedByUUID) {
        this.removedByUUID = removedByUUID;
    }

    public String getRemovedByName() {
        return this.removedByName;
    }

    public void setRemovedByName(String removedByName) {
        this.removedByName = removedByName;
    }

    public Instant getRemovedAt() {
        return this.removedAt;
    }

    public void setRemovedAt(Instant removedAt) {
        this.removedAt = removedAt;
    }

    public String getRemoveReason() {
        return this.removeReason;
    }

    public void setRemoveReason(String removeReason) {
        this.removeReason = removeReason;
    }

    public boolean isPermanent() {
        return this.expiresAt == null;
    }

    public boolean isExpired() {
        if (this.isPermanent()) {
            return false;
        }
        return Instant.now().isAfter(this.expiresAt);
    }

    public long getRemainingMillis() {
        if (this.isPermanent()) {
            return -1L;
        }
        long remaining = this.expiresAt.toEpochMilli() - Instant.now().toEpochMilli();
        return Math.max(0L, remaining);
    }

    public String toString() {
        return "处罚记录{编号=" + this.id + ", 目标玩家='" + this.targetName + "', 类型=" + String.valueOf((Object)this.type) + ", 原因='" + this.reason + "', 生效=" + this.active + ", 到期时间=" + String.valueOf(this.expiresAt) + "}";
    }
}


