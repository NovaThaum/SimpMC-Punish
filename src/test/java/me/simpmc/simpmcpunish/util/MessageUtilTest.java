package me.simpmc.simpmcpunish.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class MessageUtilTest {
    @Test
    void preservesMultilineDisconnectMessages() {
        String message = "&cBanned\n\n&7Reason: test\n&7Expires: never";

        assertEquals("Banned\n\nReason: test\nExpires: never",
                PlainTextComponentSerializer.plainText().serialize(MessageUtil.toComponent(message)));
    }
}
