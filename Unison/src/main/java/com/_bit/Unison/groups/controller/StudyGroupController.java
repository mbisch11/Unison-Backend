package com._bit.Unison.groups.controller;

import com._bit.Unison.groups.model.StudyGroup;
import com._bit.Unison.groups.service.StudyGroupService;
import com._bit.Unison.users.model.UserProfile;
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
        return groupService.createGroup(sessionId, req.courseId, req.title, req.description, req.location, req.isVirtual, req.startTime, req.maxCapacity);
    }

    @GetMapping
    public List<StudyGroup> searchGroups(@RequestHeader("X-Session-Id") String sessionId, @RequestParam String courseId, @RequestParam(required = false) LocalDateTime from, @RequestParam(required = false) LocalDateTime to, @RequestParam(required = false) Boolean isVirtual) {
        return groupService.searchGroups(sessionId, courseId, from, to, isVirtual);
    }

    @GetMapping("/search")
    public List<StudyGroup> searchGroupsDedicated(@RequestHeader("X-Session-Id") String sessionId, @RequestParam String courseId, @RequestParam(required = false) LocalDateTime from, @RequestParam(required = false) LocalDateTime to, @RequestParam(required = false) Boolean isVirtual){
        return groupService.searchGroups(sessionId, courseId, from, to, isVirtual);
    }

    @GetMapping("/available")
    public List<StudyGroup> getAvailableGroups(@RequestHeader("X-Session-Id") String sessionId, @RequestParam String courseId) {
        return groupService.getAvailableGroups(sessionId, courseId);
    }

    @GetMapping("/my-created")
    public List<StudyGroup> getMyCreatedGroups(@RequestHeader("X-Session-Id") String sessionId){
        return groupService.getMyCreatedGroups(sessionId);
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

    @GetMapping("/{groupId}/members")
    public ArrayList<UserProfile> listMembers(@RequestHeader("X-Session-Id") String sessionId, @PathVariable String groupId) {
        return groupService.listMembers(sessionId, groupId);
    }

    public static class CreateGroupRequest {
        public String courseId;
        public String title;
        public String description;
        public String location;
        public boolean isVirtual;
        public LocalDateTime startTime;
        public int maxCapacity;
    }
}