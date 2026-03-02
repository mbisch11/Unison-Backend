package com._bit.Unison.groups.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "attendance_event")
public class AttendanceEvent {

    @Id
    private String attendanceId;
    private String groupId;
    private String userId;
    private String eventType;

    public AttendanceEvent(String groupId, String userId, String eventType){
        this.groupId = groupId;
        this.userId = userId;
        this.eventType = eventType;
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

    public String getEventType() {
        return eventType;
    }
}
