package com._bit.Unison.groups.service;

import com._bit.Unison.auth.service.SessionResolver;
import com._bit.Unison.groups.model.AttendanceEvent;
import com._bit.Unison.groups.model.GroupMembership;
import com._bit.Unison.groups.model.StudyGroup;
import com._bit.Unison.groups.repo.AttendanceEventRepository;
import com._bit.Unison.groups.repo.GroupMembershipRepository;
import com._bit.Unison.groups.repo.StudyGroupRepository;
import com._bit.Unison.users.model.UserProfile;
import com._bit.Unison.users.repo.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class StudyGroupService {

    private final SessionResolver sessionResolver;
    private final StudyGroupRepository groupRepo;
    private final GroupMembershipRepository membershipRepo;
    private final AttendanceEventRepository attendanceRepo;
    private final UserProfileRepository userRepo;

    public StudyGroupService(SessionResolver sessionResolver, StudyGroupRepository groupRepo, GroupMembershipRepository membershipRepo, AttendanceEventRepository attendanceRepo, UserProfileRepository userRepo) {
        this.sessionResolver = sessionResolver;
        this.groupRepo = groupRepo;
        this.membershipRepo = membershipRepo;
        this.attendanceRepo = attendanceRepo;
        this.userRepo = userRepo;
    }

    public StudyGroup createGroup(String sessionId, String courseId, String title, String description, String location, boolean isVirtual, LocalDateTime startTime, int maxCapacity) {

        String userId = sessionResolver.requireUserId(sessionId);

        if (courseId == null || courseId.isBlank()) throw new IllegalArgumentException("courseId required");
        if (startTime == null) throw new IllegalArgumentException("startTime required");
        if (maxCapacity <= 0) throw new IllegalArgumentException("maxCapacity must be > 0");

        StudyGroup g = new StudyGroup(courseId, title, description, location, isVirtual, startTime, maxCapacity, userId);
        return groupRepo.save(g);
    }

    public StudyGroup getGroup(String sessionId, String groupId) {
        sessionResolver.requireUserId(sessionId);
        return groupRepo.findById(groupId).orElseThrow(() -> new IllegalArgumentException("Group not found"));
    }

    public List<StudyGroup> searchGroups(String sessionId, String courseId, LocalDateTime from, LocalDateTime to, Boolean isVirtual) {

        sessionResolver.requireUserId(sessionId);

        if (courseId == null || courseId.isBlank()) throw new IllegalArgumentException("courseId required");

        if (from == null || to == null) {
            return groupRepo.findByCourseId(courseId);
        }

        if (isVirtual == null) {
            return groupRepo.findByCourseIdAndStartTimeBetween(courseId, from, to);
        }

        return groupRepo.findByCourseIdAndIsVirtualAndStartTimeBetween(courseId, isVirtual, from, to);
    }

    public void joinGroup(String sessionId, String groupId) {
        String userId = sessionResolver.requireUserId(sessionId);

        StudyGroup group = groupRepo.findById(groupId).orElseThrow(() -> new IllegalArgumentException("Group not found"));

        if (membershipRepo.existsByGroupIdAndUserId(groupId, userId)) {
            throw new IllegalArgumentException("Already joined");
        }

        long memberCount = membershipRepo.countByGroupId(groupId);
        if (memberCount >= group.getMaxCapacity()) {
            throw new IllegalArgumentException("Group is full");
        }

        membershipRepo.save(new GroupMembership(groupId, userId, false));
        attendanceRepo.save(new AttendanceEvent(groupId, userId, "JOINED"));
    }

    public void leaveGroup(String sessionId, String groupId) {
        String userId = sessionResolver.requireUserId(sessionId);

        GroupMembership membership = membershipRepo.findByGroupIdAndUserId(groupId, userId).orElseThrow(() -> new IllegalArgumentException("Not a member of this group"));

        membershipRepo.delete(membership);
        attendanceRepo.save(new AttendanceEvent(groupId, userId, "LEFT"));
    }

    public void confirmAttendance(String sessionId, String groupId) {
        String userId = sessionResolver.requireUserId(sessionId);

        if (!membershipRepo.existsByGroupIdAndUserId(groupId, userId)) {
            throw new IllegalArgumentException("Join the group before confirming attendance");
        }

        attendanceRepo.save(new AttendanceEvent(groupId, userId, "CONFIRMED"));
    }

    public ArrayList<UserProfile> listMembers(String sessionId, String groupId) {
        sessionResolver.requireUserId(sessionId);

        if (!groupRepo.existsById(groupId)) {
            throw new IllegalArgumentException("Group not found");
        }

        ArrayList<UserProfile> members = new ArrayList<>();
        for (GroupMembership m : membershipRepo.findByGroupId(groupId)) {
            userRepo.findById(m.getUserId()).ifPresent(members::add);
        }
        return members;
    }
}