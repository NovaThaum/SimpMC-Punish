package me.simpmc.simpmcpunish.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class MessageUtilTest {
    @Test
    void preservesMultilineDisconnectMessages() {
        String message = "&cBanned\n\n&7Reason: test\n&7Expires: never";

        assertEquals("Banned\n\nReason: test\nExpires: never",
                PlainTextComponentSerializer.plainText().serialize(MessageUtil.toComponent(message)));
    }

    @Test
    void temporaryBanUsesVanillaTranslationKeys() {
        Component message = MessageUtil.toBanDisconnectComponent(
                "test reason",
                Instant.parse("2026-09-06T12:34:56Z"),
                false);

        assertEquals(
                "multiplayer.disconnect.banned.reasonmultiplayer.disconnect.banned.expiration",
                PlainTextComponentSerializer.plainText().serialize(message));
        TranslatableComponent reason = assertInstanceOf(TranslatableComponent.class, message);
        assertEquals("multiplayer.disconnect.banned.reason", reason.key());
        assertEquals("test reason", PlainTextComponentSerializer.plainText()
                .serialize(reason.arguments().getFirst().asComponent()));
        TranslatableComponent expiration = assertInstanceOf(
                TranslatableComponent.class,
                reason.children().getFirst());
        assertEquals("multiplayer.disconnect.banned.expiration", expiration.key());
        String expirationArgument = PlainTextComponentSerializer.plainText()
                .serialize(expiration.arguments().getFirst().asComponent());
        assertTrue(expirationArgument.matches(
                "\\d{4}-\\d{2}-\\d{2} at \\d{2}:\\d{2}:\\d{2} .+"));
    }

    @Test
    void permanentBanDoesNotAppendExpirationKey() {
        Component message = MessageUtil.toBanDisconnectComponent("test reason", null, false);

        assertEquals(
                "multiplayer.disconnect.banned.reason",
                PlainTextComponentSerializer.plainText().serialize(message));
    }

    @Test
    void kickUsesVanillaBanReasonKey() {
        Component message = MessageUtil.toKickDisconnectComponent("test reason");

        assertEquals(
                "multiplayer.disconnect.banned.reason",
                PlainTextComponentSerializer.plainText().serialize(message));
        TranslatableComponent reason = assertInstanceOf(TranslatableComponent.class, message);
        assertEquals("multiplayer.disconnect.banned.reason", reason.key());
        assertEquals("test reason", PlainTextComponentSerializer.plainText()
                .serialize(reason.arguments().getFirst().asComponent()));
        assertTrue(reason.children().isEmpty());
    }

    @Test
    void kickWithoutReasonUsesDefaultReason() {
        Component message = MessageUtil.toKickDisconnectComponent(null);

        TranslatableComponent reason = assertInstanceOf(TranslatableComponent.class, message);
        assertEquals("未填写原因", PlainTextComponentSerializer.plainText()
                .serialize(reason.arguments().getFirst().asComponent()));
    }
}
