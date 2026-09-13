package me.simpmc.simpmcpunish.velocity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public record VelocityMessages(String kickFallback, String databaseError) {
    public static VelocityMessages load(Path dataDirectory) throws IOException {
        Path file = dataDirectory.resolve("messages.yml");
        if (!Files.exists(file)) {
            try (InputStream input = VelocityMessages.class.getResourceAsStream("/messages.yml")) {
                if (input == null) {
                    throw new IOException("缺少内置 messages.yml");
                }
                Files.copy(input, file);
            }
        }
        LoaderOptions options = new LoaderOptions();
        Yaml yaml = new Yaml(new SafeConstructor(options));
        try (InputStream input = Files.newInputStream(file)) {
            Object loaded = yaml.load(input);
            Map<?, ?> values = loaded instanceof Map<?, ?> map ? map : Collections.emptyMap();
            return new VelocityMessages(
                    value(values, "kick-fallback", "<red>你已被后端服务器踢出。</red>"),
                    value(values, "database-error", "<red>处罚数据库暂时不可用，请稍后重试。</red>"));
        }
    }

    private static String value(Map<?, ?> values, String key, String fallback) {
        Object value = values.get(key);
        return value == null ? fallback : String.valueOf(value);
    }
}
