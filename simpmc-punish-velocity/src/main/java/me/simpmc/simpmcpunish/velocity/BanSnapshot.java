package me.simpmc.simpmcpunish.velocity;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record BanSnapshot(List<PunishmentRecord> records) {
    public BanSnapshot {
        records = records == null ? List.of() : records.stream()
                .sorted(Comparator.comparing(PunishmentRecord::createdAt).reversed()
                        .thenComparing(Comparator.comparingInt(PunishmentRecord::id).reversed()))
                .toList();
    }

    public Optional<PunishmentRecord> find(UUID uuid, String ipAddress) {
        return records.stream().filter(record -> record.matches(uuid, ipAddress)).findFirst();
    }

    public static BanSnapshot empty() {
        return new BanSnapshot(List.of());
    }
}
