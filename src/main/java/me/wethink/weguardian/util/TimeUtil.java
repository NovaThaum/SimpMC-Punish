/*
 * Decompiled with CFR 0.152.
 */
package me.wethink.weguardian.util;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeUtil {
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhdwMy])");

    private TimeUtil() {
    }

    public static long parseDuration(String input) {
        if (input == null || input.isEmpty()) {
            return -1L;
        }
        if (input.equalsIgnoreCase("permanent") || input.equalsIgnoreCase("perm") || input.equals("-1")) {
            return -1L;
        }
        Matcher matcher = TIME_PATTERN.matcher(input.toLowerCase());
        long totalMillis = 0L;
        boolean found = false;
        while (matcher.find()) {
            String unit;
            found = true;
            long value = Long.parseLong(matcher.group(1));
            totalMillis += (switch (unit = matcher.group(2)) {
                case "s" -> TimeUnit.SECONDS.toMillis(value);
                case "m" -> TimeUnit.MINUTES.toMillis(value);
                case "h" -> TimeUnit.HOURS.toMillis(value);
                case "d" -> TimeUnit.DAYS.toMillis(value);
                case "w" -> TimeUnit.DAYS.toMillis(value * 7L);
                case "M" -> TimeUnit.DAYS.toMillis(value * 30L);
                case "y" -> TimeUnit.DAYS.toMillis(value * 365L);
                default -> 0L;
            });
        }
        return found ? totalMillis : -1L;
    }

    public static String formatDuration(long millis) {
        String result;
        if (millis < 0L) {
            return "Permanent";
        }
        if (millis == 0L) {
            return "Expired";
        }
        Duration duration = Duration.ofMillis(millis);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        StringBuilder sb = new StringBuilder();
        if (days > 0L) {
            sb.append(days).append("d ");
        }
        if (hours > 0L) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0L) {
            sb.append(minutes).append("m ");
        }
        if (seconds > 0L && days == 0L) {
            sb.append(seconds).append("s");
        }
        return (result = sb.toString().trim()).isEmpty() ? "< 1s" : result;
    }

    public static String formatRemaining(Instant expiresAt) {
        if (expiresAt == null) {
            return "Permanent";
        }
        long remaining = expiresAt.toEpochMilli() - Instant.now().toEpochMilli();
        if (remaining <= 0L) {
            return "Expired";
        }
        return TimeUtil.formatDuration(remaining);
    }

    public static String formatDate(Instant instant) {
        if (instant == null) {
            return "Never";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm").withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }

    public static Instant getExpiryInstant(long durationMillis) {
        if (durationMillis < 0L) {
            return null;
        }
        return Instant.now().plusMillis(durationMillis);
    }
}


