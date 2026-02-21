package com.creatoros.auth.util;

import java.time.Duration;

public final class DurationUtil {

    private DurationUtil() {
    }

    /**
     * Parses either:
     * - ISO-8601 duration (e.g. PT15M)
     * - seconds as an integer string (e.g. 900)
     */
    public static Duration parseDurationSecondsOrIso(String value, Duration defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        String trimmed = value.trim();
        try {
            if (trimmed.startsWith("P") || trimmed.startsWith("p")) {
                return Duration.parse(trimmed);
            }
            long seconds = Long.parseLong(trimmed);
            if (seconds <= 0) {
                throw new IllegalArgumentException("Duration seconds must be positive");
            }
            return Duration.ofSeconds(seconds);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid duration value: '" + value + "'", ex);
        }
    }
}
