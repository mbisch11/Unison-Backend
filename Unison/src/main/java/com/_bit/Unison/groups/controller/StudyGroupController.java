package com._bit.Unison.groups.controller;

import com._bit.Unison.common.FlexibleDateTimeParser;
import com._bit.Unison.groups.dto.ManagedStudyGroupDto;
import com._bit.Unison.groups.dto.StudyGroupMemberDto;
import com._bit.Unison.groups.model.StudyGroup;
import com._bit.Unison.groups.service.StudyGroupService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/groups")
public class StudyGroupController {

    private final StudyGroupService groupService;

    public StudyGroupController(StudyGroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudyGroup createGroup(@RequestHeader("X-Session-Id") String sessionId, @RequestBody CreateGroupRequest req) {
        LocalDateTime startTime = FlexibleDateTimeParser.parseNullable(req.startTime, "startTime");
        return groupService.createGroup(sessionId, req.courseId, req.title, req.description, req.location, req.isVirtual, startTime, req.maxCapacity, req.durationMinutes);
    }

    @GetMapping
    public List<StudyGroup> searchGroups(@RequestHeader("X-Session-Id") String sessionId, @RequestParam String courseId, @RequestParam(required = false) String from, @RequestParam(required = false) String to, @RequestParam(required = false) Boolean isVirtual) {
        LocalDateTime fromDateTime = FlexibleDateTimeParser.parseNullable(from, "from");
        LocalDateTime toDateTime = FlexibleDateTimeParser.parseNullable(to, "to");
        return groupService.searchGroups(sessionId, courseId, fromDateTime, toDateTime, isVirtual);
    }

    @GetMapping("/search")
    public List<StudyGroup> searchGroupsDedicated(@RequestHeader("X-Session-Id") String sessionId, @RequestParam String courseId, @RequestParam(required = false) String from, @RequestParam(required = false) String to, @RequestParam(required = false) Boolean isVirtual){
        LocalDateTime fromDateTime = FlexibleDateTimeParser.parseNullable(from, "from");
        LocalDateTime toDateTime = FlexibleDateTimeParser.parseNullable(to, "to");
        return groupService.searchGroups(sessionId, courseId, fromDateTime, toDateTime, isVirtual);
    }

    @GetMapping("/available")
    public List<StudyGroup> getAvailableGroups(@RequestHeader("X-Session-Id") String sessionId, @RequestParam String courseId) {
        return groupService.getAvailableGroups(sessionId, courseId);
    }

    @GetMapping("/my-created")
    public List<ManagedStudyGroupDto> getMyCreatedGroups(@RequestHeader("X-Session-Id") String sessionId){
        return groupService.getMyCreatedGroups(sessionId);
    }

    @GetMapping("/my-joined")
    public List<ManagedStudyGroupDto> getMyJoinedGroups(@RequestHeader("X-Session-Id") String sessionId) {
        return groupService.getMyJoinedGroups(sessionId);
    }

    @GetMapping("/{groupId}")
    public StudyGroup getGroup(@RequestHeader("X-Session-Id") String sessionId, @PathVariable String groupId) {
        return groupService.getGroup(sessionId, groupId);
    }

    @PostMapping("/{groupId}/join")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void joinGroup(@RequestHeader("X-Session-Id") String sessionId, @PathVariable String groupId) {
        groupService.joinGroup(sessionId, groupId);
    }

    @PostMapping("/{groupId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveGroup(@RequestHeader("X-Session-Id") String sessionId, @PathVariable String groupId) {
        groupService.leaveGroup(sessionId, groupId);
    }

    @PostMapping("/{groupId}/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmAttendance(@RequestHeader("X-Session-Id") String sessionId, @PathVariable String groupId) {
        groupService.confirmAttendance(sessionId, groupId);
    }

    @PostMapping("/{groupId}/rsvp")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rsvpAttendance(@RequestHeader("X-Session-Id") String sessionId, @PathVariable String groupId) {
        groupService.rsvpAttendance(sessionId, groupId);
    }

    @PostMapping("/{groupId}/attendance/{memberUserId}/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyAttendance(
            @RequestHeader("X-Session-Id") String sessionId,
            @PathVariable String groupId,
            @PathVariable String memberUserId,
            @RequestBody VerifyAttendanceRequest req
    ) {
        groupService.verifyAttendance(sessionId, groupId, memberUserId, req.attended);
    }

    @GetMapping("/{groupId}/members")
    public ArrayList<StudyGroupMemberDto> listMembers(@RequestHeader("X-Session-Id") String sessionId, @PathVariable String groupId) {
        return groupService.listMembers(sessionId, groupId);
    }

    public static class CreateGroupRequest {
        public String courseId;
        public String title;
        public String description;
        public String location;
        public boolean isVirtual;
        public String startTime;
        public int durationMinutes;
        public int maxCapacity;
    }

    public static class VerifyAttendanceRequest {
        public boolean attended;
    }
}
