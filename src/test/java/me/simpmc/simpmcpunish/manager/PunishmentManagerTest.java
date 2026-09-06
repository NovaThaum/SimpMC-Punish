package me.simpmc.simpmcpunish.manager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class PunishmentManagerTest {
    private static final String PATH = "behavior.vanilla-ban-components";

    @Test
    void missingOptionIgnoresBundledDefaultForExistingConfiguration() {
        YamlConfiguration existing = new YamlConfiguration();
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set(PATH, true);
        existing.setDefaults(defaults);

        assertFalse(PunishmentManager.usesVanillaBanComponents(existing));
    }

    @Test
    void explicitOptionControlsVanillaBanComponents() {
        YamlConfiguration configuration = new YamlConfiguration();

        configuration.set(PATH, true);
        assertTrue(PunishmentManager.usesVanillaBanComponents(configuration));

        configuration.set(PATH, false);
        assertFalse(PunishmentManager.usesVanillaBanComponents(configuration));
    }

    @Test
    void newConfigurationEnablesVanillaBanComponents() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/config.yml")) {
            assertNotNull(input);
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(input, StandardCharsets.UTF_8));

            assertTrue(PunishmentManager.usesVanillaBanComponents(configuration));
        }
    }
}
