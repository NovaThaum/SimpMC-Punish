package me.simpmc.simpmcpunish.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class MessageUtilTest {
    @Test
    void compactsMultilineDisconnectMessages() {
        String message = "&cBanned\r\n\r\n  &7Reason: test\n&7Expires: never  ";

        assertEquals("&cBanned &7Reason: test &7Expires: never", MessageUtil.compactLines(message));
    }

    @Test
    void preservesEmptyAndNullMessages() {
        assertEquals("   \n", MessageUtil.compactLines("   \n"));
        assertNull(MessageUtil.compactLines(null));
    }
}
