package me.simpmc.simpmcpunish.velocity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

class VelocityConfigurationTest {
    @TempDir
    Path dataDirectory;

    @Test
    void newConfigurationEnablesVanillaBanComponents() throws Exception {
        VelocityConfiguration configuration = VelocityConfiguration.load(
                this.dataDirectory,
                LoggerFactory.getLogger(VelocityConfigurationTest.class));

        assertTrue(configuration.vanillaBanComponents());
    }

    @Test
    void existingConfigurationWithoutOptionKeepsCustomBanScreen() throws Exception {
        Files.writeString(this.dataDirectory.resolve("config.yml"), "{}\n");

        VelocityConfiguration configuration = VelocityConfiguration.load(
                this.dataDirectory,
                LoggerFactory.getLogger(VelocityConfigurationTest.class));

        assertFalse(configuration.vanillaBanComponents());
    }
}
