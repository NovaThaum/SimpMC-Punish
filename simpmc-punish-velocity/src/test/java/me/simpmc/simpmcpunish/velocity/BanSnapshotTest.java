package me.simpmc.simpmcpunish.velocity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BanSnapshotTest {
    private static final UUID PLAYER = UUID.randomUUID();

    @Test
    void newestMatchingRecordWins() {
        Instant old = Instant.parse("2026-01-01T00:00:00Z");
        Instant newer = Instant.parse("2026-01-02T00:00:00Z");
        PunishmentRecord oldRecord = new PunishmentRecord(20, PLAYER, "player", null, "staff", "BAN", "old", old, null);
        PunishmentRecord newRecord = new PunishmentRecord(21, PLAYER, "player", null, "staff", "BAN", "new", newer, null);

        assertEquals("new", new BanSnapshot(List.of(oldRecord, newRecord)).find(PLAYER, null).orElseThrow().reason());
    }

    @Test
    void ipBanMatchesMappedIpv4Address() {
        PunishmentRecord record = new PunishmentRecord(1, null, "player", "192.0.2.1", "staff", "BANIP", "reason", Instant.now(), null);

        assertTrue(record.matches(null, "::ffff:192.0.2.1"));
    }
}
