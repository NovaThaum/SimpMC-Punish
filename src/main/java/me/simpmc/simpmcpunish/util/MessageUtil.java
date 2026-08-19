package me.simpmc.simpmcpunish.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class MessageUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern SECTION_PATTERN = Pattern.compile("\u00a7[0-9a-fk-orA-FK-OR]");
    private static final Pattern MINI_MESSAGE_PATTERN = Pattern.compile("<[a-zA-Z_#]+[^>]*>");

    private MessageUtil() {
    }

    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("\u00a7x");
            for (char c : hex.toCharArray()) {
                replacement.append('\u00a7').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return MessageUtil.translateAmpersandCodes(buffer.toString());
    }

    private static String translateAmpersandCodes(String message) {
        char[] chars = message.toCharArray();
        for (int i = 0; i < chars.length - 1; ++i) {
            if (chars[i] != '&' || !MessageUtil.isColorCode(chars[i + 1])) continue;
            chars[i] = 167;
            chars[i + 1] = Character.toLowerCase(chars[i + 1]);
        }
        return new String(chars);
    }

    private static boolean isColorCode(char c) {
        return "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(c) > -1;
    }

    public static Component toComponent(String message) {
        boolean hasLegacy;
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        boolean hasMiniMessage = MINI_MESSAGE_PATTERN.matcher(message).find();
        boolean bl = hasLegacy = message.contains("&") || message.contains("\u00a7");
        if (hasMiniMessage && !hasLegacy) {
            try {
                return MINI_MESSAGE.deserialize(message);
            }
            catch (Exception e) {
                return Component.text(message);
            }
        }
        if (hasLegacy && !hasMiniMessage) {
            return LEGACY.deserialize(MessageUtil.colorize(message));
        }
        if (hasMiniMessage && hasLegacy) {
            try {
                String legacyToMini = MessageUtil.convertLegacyToMiniMessage(message);
                return MINI_MESSAGE.deserialize(legacyToMini);
            }
            catch (Exception e) {
                return LEGACY.deserialize(MessageUtil.colorize(message));
            }
        }
        return Component.text(message);
    }

    private static String convertLegacyToMiniMessage(String message) {
        message = message.replace("&0", "<black>").replace("\u00a70", "<black>");
        message = message.replace("&1", "<dark_blue>").replace("\u00a71", "<dark_blue>");
        message = message.replace("&2", "<dark_green>").replace("\u00a72", "<dark_green>");
        message = message.replace("&3", "<dark_aqua>").replace("\u00a73", "<dark_aqua>");
        message = message.replace("&4", "<dark_red>").replace("\u00a74", "<dark_red>");
        message = message.replace("&5", "<dark_purple>").replace("\u00a75", "<dark_purple>");
        message = message.replace("&6", "<gold>").replace("\u00a76", "<gold>");
        message = message.replace("&7", "<gray>").replace("\u00a77", "<gray>");
        message = message.replace("&8", "<dark_gray>").replace("\u00a78", "<dark_gray>");
        message = message.replace("&9", "<blue>").replace("\u00a79", "<blue>");
        message = message.replace("&a", "<green>").replace("\u00a7a", "<green>");
        message = message.replace("&A", "<green>").replace("\u00a7A", "<green>");
        message = message.replace("&b", "<aqua>").replace("\u00a7b", "<aqua>");
        message = message.replace("&B", "<aqua>").replace("\u00a7B", "<aqua>");
        message = message.replace("&c", "<red>").replace("\u00a7c", "<red>");
        message = message.replace("&C", "<red>").replace("\u00a7C", "<red>");
        message = message.replace("&d", "<light_purple>").replace("\u00a7d", "<light_purple>");
        message = message.replace("&D", "<light_purple>").replace("\u00a7D", "<light_purple>");
        message = message.replace("&e", "<yellow>").replace("\u00a7e", "<yellow>");
        message = message.replace("&E", "<yellow>").replace("\u00a7E", "<yellow>");
        message = message.replace("&f", "<white>").replace("\u00a7f", "<white>");
        message = message.replace("&F", "<white>").replace("\u00a7F", "<white>");
        message = message.replace("&k", "<obfuscated>").replace("\u00a7k", "<obfuscated>");
        message = message.replace("&K", "<obfuscated>").replace("\u00a7K", "<obfuscated>");
        message = message.replace("&l", "<bold>").replace("\u00a7l", "<bold>");
        message = message.replace("&L", "<bold>").replace("\u00a7L", "<bold>");
        message = message.replace("&m", "<strikethrough>").replace("\u00a7m", "<strikethrough>");
        message = message.replace("&M", "<strikethrough>").replace("\u00a7M", "<strikethrough>");
        message = message.replace("&n", "<underlined>").replace("\u00a7n", "<underlined>");
        message = message.replace("&N", "<underlined>").replace("\u00a7N", "<underlined>");
        message = message.replace("&o", "<italic>").replace("\u00a7o", "<italic>");
        message = message.replace("&O", "<italic>").replace("\u00a7O", "<italic>");
        message = message.replace("&r", "<reset>").replace("\u00a7r", "<reset>");
        message = message.replace("&R", "<reset>").replace("\u00a7R", "<reset>");
        Matcher hexMatcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder();
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1);
            hexMatcher.appendReplacement(buffer, "<#" + hex + ">");
        }
        hexMatcher.appendTail(buffer);
        return buffer.toString();
    }

    public static String stripColor(String message) {
        if (message == null) {
            return null;
        }
        return PLAIN.serialize(MessageUtil.toComponent(message));
    }

    public static String replacePlaceholders(String message, String ... replacements) {
        if (message == null || replacements.length == 0) {
            return message;
        }
        String result = message;
        for (int i = 0; i < replacements.length - 1; i += 2) {
            result = result.replace(replacements[i], replacements[i + 1]);
        }
        return result;
    }

}


