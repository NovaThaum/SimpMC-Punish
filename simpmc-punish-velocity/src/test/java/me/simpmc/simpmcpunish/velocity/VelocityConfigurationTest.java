package me.simpmc.simpmcpunish.velocity;

import static org.junit.jupiter.api.Assertions.assertTrue;

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

        assertTrue(configuration.webEnabled());
    }
}
