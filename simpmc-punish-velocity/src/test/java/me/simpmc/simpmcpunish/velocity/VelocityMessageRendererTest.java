package me.simpmc.simpmcpunish.velocity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class VelocityMessageRendererTest {
    @Test
    void auditReasonIsCollapsedToOneLineAndTruncated() {
        VelocityMessageRenderer renderer = new VelocityMessageRenderer(
                new VelocityMessages("<red>ban</red>", "kick", "error"), 13, true);

        assertEquals("line one | l…", renderer.auditReason("line one\nline two"));
    }

    @Test
    void temporaryBanUsesVanillaTranslationKeys() {
        VelocityMessageRenderer renderer = new VelocityMessageRenderer(
                new VelocityMessages("unused", "kick", "error"), 240, true);
        PunishmentRecord punishment = new PunishmentRecord(
                1,
                UUID.randomUUID(),
                "player",
                null,
                "staff",
                "TEMPBAN",
                "test reason",
                Instant.parse("2026-09-05T12:00:00Z"),
                Instant.parse("2026-09-06T12:00:00Z"));

        assertEquals(
                "multiplayer.disconnect.banned.reasonmultiplayer.disconnect.banned.expiration",
                PlainTextComponentSerializer.plainText().serialize(renderer.banScreen(punishment)));
    }

    @Test
    void temporaryIpBanUsesVanillaTranslationKeys() {
        VelocityMessageRenderer renderer = new VelocityMessageRenderer(
                new VelocityMessages("unused", "kick", "error"), 240, true);
        PunishmentRecord punishment = new PunishmentRecord(
                1,
                UUID.randomUUID(),
                "player",
                "192.0.2.1",
                "staff",
                "TEMPBANIP",
                "test reason",
                Instant.parse("2026-09-05T12:00:00Z"),
                Instant.parse("2026-09-06T12:00:00Z"));

        assertEquals(
                "multiplayer.disconnect.banned_ip.reasonmultiplayer.disconnect.banned_ip.expiration",
                PlainTextComponentSerializer.plainText().serialize(renderer.banScreen(punishment)));
    }

    @Test
    void customBanScreenRemainsAvailableWhenVanillaComponentsAreDisabled() {
        VelocityMessageRenderer renderer = new VelocityMessageRenderer(
                new VelocityMessages("Banned <player>: <reason> by <staff> (<expires>)", "kick", "error"),
                240,
                false);
        PunishmentRecord punishment = new PunishmentRecord(
                1,
                UUID.randomUUID(),
                "player",
                null,
                "staff",
                "BAN",
                "test reason",
                Instant.parse("2026-09-05T12:00:00Z"),
                null);

        assertEquals(
                "Banned player: test reason by staff (永久)",
                PlainTextComponentSerializer.plainText().serialize(renderer.banScreen(punishment)));
    }
}
