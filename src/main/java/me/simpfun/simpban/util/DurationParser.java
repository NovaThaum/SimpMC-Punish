/*
 * Decompiled with CFR 0.152.
 */
package me.simpfun.simpban.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationParser {
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([smhdwMy])");

    public static long parse(String duration) {
        if (duration == null || duration.isEmpty()) {
            return -1L;
        }
        Matcher matcher = DURATION_PATTERN.matcher(duration.trim());
        if (!matcher.matches()) {
            return -1L;
        }
        long amount = Long.parseLong(matcher.group(1));
        char unit = matcher.group(2).charAt(0);
        return switch (unit) {
            case 's' -> amount * 1000L;
            case 'm' -> amount * 60L * 1000L;
            case 'h' -> amount * 60L * 60L * 1000L;
            case 'd' -> amount * 24L * 60L * 60L * 1000L;
            case 'w' -> amount * 7L * 24L * 60L * 60L * 1000L;
            case 'M' -> amount * 30L * 24L * 60L * 60L * 1000L;
            case 'y' -> amount * 365L * 24L * 60L * 60L * 1000L;
            default -> -1L;
        };
    }

    public static String format(long millis) {
        if (millis <= 0L) {
            return "Permanent";
        }
        long seconds = millis / 1000L;
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        long days = hours / 24L;
        if (days > 0L) {
            return days + " day" + (days != 1L ? "s" : "");
        }
        if (hours > 0L) {
            return hours + " hour" + (hours != 1L ? "s" : "");
        }
        if (minutes > 0L) {
            return minutes + " minute" + (minutes != 1L ? "s" : "");
        }
        return seconds + " second" + (seconds != 1L ? "s" : "");
    }
}


