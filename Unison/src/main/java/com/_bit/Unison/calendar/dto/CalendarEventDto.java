package com._bit.Unison.calendar.dto;

import java.time.LocalDateTime;

public record CalendarEventDto(
        String eventId,
        String groupId,
        String courseId,
        String title,
        String description,
        String location,
        boolean isVirtual,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
