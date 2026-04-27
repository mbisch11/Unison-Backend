package com._bit.Unison.groups.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "attendance_event")
public class AttendanceEvent {

    @Id
    private String attendanceId;
    private String groupId;
    private String userId;
    private String recordedByUserId;
    private String eventType;
    private Instant recordedAt;

    public AttendanceEvent() {
    }

    public AttendanceEvent(String groupId, String userId, String eventType){
        this(groupId, userId, userId, eventType);
    }

    public AttendanceEvent(String groupId, String userId, String recordedByUserId, String eventType) {
        this.groupId = groupId;
        this.userId = userId;
        this.recordedByUserId = recordedByUserId;
        this.eventType = eventType;
        this.recordedAt = Instant.now();
    }

    public String getAttendanceId() {
        return attendanceId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getUserId() {
        return userId;
    }

    public String getRecordedByUserId() {
        return recordedByUserId == null || recordedByUserId.isBlank() ? userId : recordedByUserId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
