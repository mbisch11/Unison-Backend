package com._bit.Unison.groups.dto;

public record StudyGroupMemberDto(
        String userId,
        String displayName,
        String username,
        boolean isLeader,
        AttendanceStatus attendanceStatus
) {
}
