package com._bit.Unison.common;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

public final class FlexibleDateTimeParser {

    private FlexibleDateTimeParser() {
    }

    public static LocalDateTime parseNullable(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();

        try {
            return LocalDateTime.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            // Fall through to offset-aware parsing.
        }

        try {
            return OffsetDateTime.parse(trimmed)
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // Fall through to instant parsing.
        }

        try {
            return Instant.parse(trimmed)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            throw new IllegalArgumentException(fieldName + " must be a valid ISO date-time");
        }
    }
}
