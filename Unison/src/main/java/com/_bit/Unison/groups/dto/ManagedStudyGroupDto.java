package com._bit.Unison.groups.dto;

import java.time.LocalDateTime;

public record ManagedStudyGroupDto(
        String groupId,
        String courseId,
        String title,
        String description,
        String location,
        boolean isVirtual,
        LocalDateTime startTime,
        int durationMinutes,
        int maxCapacity,
        String createdByUserId,
        LocalDateTime createdAt,
        boolean currentUserIsLeader,
        AttendanceStatus currentUserAttendanceStatus
) {
}
