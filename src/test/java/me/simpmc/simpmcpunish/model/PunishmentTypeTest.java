package me.simpmc.simpmcpunish.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PunishmentTypeTest {
    @Test
    void warningTypesExposeDedicatedPermissionsAndTemporaryState() {
        assertEquals("simpmc-punish.warn", PunishmentType.WARN.getPermission());
        assertEquals("simpmc-punish.tempwarn", PunishmentType.TEMPWARN.getPermission());
        assertTrue(PunishmentType.WARN.isWarning());
        assertTrue(PunishmentType.TEMPWARN.isWarning());
        assertTrue(PunishmentType.TEMPWARN.isTemporary());
    }
}
