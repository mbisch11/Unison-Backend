package com._bit.Unison.groups.service;

import com._bit.Unison.auth.service.SessionResolver;
import com._bit.Unison.common.ConflictException;
import com._bit.Unison.common.NotFoundException;
import com._bit.Unison.groups.dto.AttendanceStatus;
import com._bit.Unison.groups.dto.ManagedStudyGroupDto;
import com._bit.Unison.groups.dto.StudyGroupMemberDto;
import com._bit.Unison.groups.model.AttendanceEvent;
import com._bit.Unison.groups.model.GroupMembership;
import com._bit.Unison.groups.model.StudyGroup;
import com._bit.Unison.groups.repo.AttendanceEventRepository;
import com._bit.Unison.groups.repo.GroupMembershipRepository;
import com._bit.Unison.groups.repo.StudyGroupRepository;
import com._bit.Unison.groups.repo.StudyGroupSearchRepository;
import com._bit.Unison.groups.service.filter.AvailableGroupsFilterStrategy;
import com._bit.Unison.groups.service.filter.CourseFilterStrategy;
import com._bit.Unison.groups.service.filter.GroupFilterContext;
import com._bit.Unison.groups.service.filter.TimeWindowFilterStrategy;
import com._bit.Unison.groups.service.filter.VirtualFilterStrategy;
import com._bit.Unison.notifications.service.NotificationService;
import com._bit.Unison.users.model.UserProfile;
import com._bit.Unison.users.repo.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StudyGroupService {

    private static final String EVENT_JOINED = "JOINED";
    private static final String EVENT_LEFT = "LEFT";
    private static final String EVENT_RSVPED = "RSVPED";
    private static final String EVENT_VERIFIED_ATTENDED = "VERIFIED_ATTENDED";
    private static final String EVENT_VERIFIED_ABSENT = "VERIFIED_ABSENT";
    private static final String EVENT_LEGACY_CONFIRMED = "CONFIRMED";

    private final SessionResolver sessionResolver;
    private final StudyGroupRepository groupRepo;
    private final StudyGroupSearchRepository groupSearchRepo;
    private final GroupMembershipRepository membershipRepo;
    private final AttendanceEventRepository attendanceRepo;
    private final UserProfileRepository userRepo;
    private final NotificationService notificationService;

    public StudyGroupService(
            SessionResolver sessionResolver,
            StudyGroupRepository groupRepo,
            StudyGroupSearchRepository groupSearchRepo,
            GroupMembershipRepository membershipRepo,
            AttendanceEventRepository attendanceRepo,
            UserProfileRepository userRepo,
            NotificationService notificationService
    ) {
        this.sessionResolver = sessionResolver;
        this.groupRepo = groupRepo;
        this.groupSearchRepo = groupSearchRepo;
        this.membershipRepo = membershipRepo;
        this.attendanceRepo = attendanceRepo;
        this.userRepo = userRepo;
        this.notificationService = notificationService;
    }

    public StudyGroup createGroup(String sessionId, String courseId, String title, String description, String location, boolean isVirtual, LocalDateTime startTime, int maxCapacity) {
        return createGroup(sessionId, courseId, title, description, location, isVirtual, startTime, maxCapacity, 60);
    }

    public StudyGroup createGroup(String sessionId, String courseId, String title, String description, String location, boolean isVirtual, LocalDateTime startTime, int maxCapacity, int durationMinutes) {
        String userId = sessionResolver.requireUserId(sessionId);
        String normalizedCourseId = normalizeCourseId(courseId);
        String normalizedTitle = normalizeTitle(title);
        String normalizedDescription = normalizeDescription(description);
        String normalizedLocation = normalizeLocation(location, isVirtual);
        LocalDateTime normalizedStartTime = normalizeStartTime(startTime);
        int normalizedDurationMinutes = normalizeDurationMinutes(durationMinutes);
        int normalizedCapacity = normalizeCapacity(maxCapacity);

        StudyGroup group = new StudyGroup(
                normalizedCourseId,
                normalizedTitle,
                normalizedDescription,
                normalizedLocation,
                isVirtual,
                normalizedStartTime,
                normalizedDurationMinutes,
                normalizedCapacity,
                userId
        );
        StudyGroup savedGroup = groupRepo.save(group);

        membershipRepo.save(new GroupMembership(savedGroup.getGroupId(), userId, true));
        logAttendance(savedGroup.getGroupId(), userId, EVENT_JOINED);

        return savedGroup;
    }

    public StudyGroup getGroup(String sessionId, String groupId) {
        sessionResolver.requireUserId(sessionId);
        return requireGroup(groupId);
    }

    public List<StudyGroup> searchGroups(String sessionId, String courseId, LocalDateTime from, LocalDateTime to, Boolean isVirtual) {
        sessionResolver.requireUserId(sessionId);
        String normalizedCourseId = normalizeCourseId(courseId);
        validateTimeWindow(from, to);

        List<StudyGroup> candidates = groupSearchRepo.findByCourseIdIgnoreCase(normalizedCourseId);

        GroupFilterContext context = new GroupFilterContext()
                .addStrategy(new CourseFilterStrategy(normalizedCourseId));

        if (from != null || to != null) {
            context.addStrategy(new TimeWindowFilterStrategy(from, to));
        }
        if (isVirtual != null) {
            context.addStrategy(new VirtualFilterStrategy(isVirtual));
        }

        return sortGroups(context.applyAll(candidates));
    }

    public void joinGroup(String sessionId, String groupId) {
        String userId = sessionResolver.requireUserId(sessionId);
        StudyGroup group = requireGroup(groupId);

        if (membershipRepo.existsByGroupIdAndUserId(groupId, userId)) {
            throw new ConflictException("Already joined");
        }

        if (group.getStartTime() != null && !group.getStartTime().isAfter(LocalDateTime.now())) {
            throw new ConflictException("Cannot join a group that has already started");
        }

        long memberCount = membershipRepo.countByGroupId(groupId);
        if (memberCount >= group.getMaxCapacity()) {
            throw new ConflictException("Group is full");
        }

        membershipRepo.save(new GroupMembership(groupId, userId, false));
        logAttendance(groupId, userId, EVENT_JOINED);
        notifyGroupCreatorAboutJoin(group, userId);
    }

    public void leaveGroup(String sessionId, String groupId) {
        String userId = sessionResolver.requireUserId(sessionId);
        requireGroup(groupId);

        GroupMembership membership = requireMembership(groupId, userId, "Not a member of this group");

        membershipRepo.delete(membership);
        logAttendance(groupId, userId, EVENT_LEFT);
    }

    public void confirmAttendance(String sessionId, String groupId) {
        rsvpAttendance(sessionId, groupId);
    }

    public void rsvpAttendance(String sessionId, String groupId) {
        String userId = sessionResolver.requireUserId(sessionId);
        StudyGroup group = requireGroup(groupId);

        requireMembership(groupId, userId, "Join the group before confirming attendance");

        if (group.getStartTime() != null && !group.getStartTime().isAfter(LocalDateTime.now())) {
            throw new ConflictException("Attendance RSVP closes once the group starts");
        }

        AttendanceStatus attendanceStatus = buildAttendanceStatus(groupId, userId);
        if (attendanceStatus == AttendanceStatus.RSVPED) {
            throw new ConflictException("Attendance RSVP already submitted");
        }
        if (attendanceStatus == AttendanceStatus.VERIFIED_ATTENDED || attendanceStatus == AttendanceStatus.VERIFIED_ABSENT) {
            throw new ConflictException("Attendance has already been reviewed for this session");
        }

        logAttendance(groupId, userId, EVENT_RSVPED);
        notificationService.createNotification(
                userId,
                NotificationService.TYPE_ATTENDANCE_RSVP,
                "Attendance RSVP saved",
                "You're marked as attending " + group.getTitle() + ".",
                Map.of("groupId", groupId)
        );
    }

    public void verifyAttendance(String sessionId, String groupId, String memberUserId, boolean attended) {
        String leaderUserId = sessionResolver.requireUserId(sessionId);
        StudyGroup group = requireGroup(groupId);

        GroupMembership leaderMembership = requireMembership(groupId, leaderUserId, "Join the group before reviewing attendance");
        if (!leaderMembership.isLeader()) {
            throw new ConflictException("Only group leaders can review attendance");
        }

        if (memberUserId == null || memberUserId.isBlank()) {
            throw new IllegalArgumentException("memberUserId required");
        }
        if (memberUserId.equals(leaderUserId)) {
            throw new ConflictException("Group leaders cannot verify their own attendance");
        }

        if (group.getStartTime() != null && group.getStartTime().isAfter(LocalDateTime.now())) {
            throw new ConflictException("Attendance can only be reviewed after the group starts");
        }

        GroupMembership memberMembership = requireMembership(groupId, memberUserId, "Reported member is not part of this group");
        if (memberMembership.isLeader()) {
            throw new ConflictException("Group leaders cannot be reviewed through this flow");
        }

        AttendanceStatus attendanceStatus = buildAttendanceStatus(groupId, memberUserId);
        if (attendanceStatus == AttendanceStatus.NOT_RESPONDED) {
            throw new ConflictException("This member has not RSVPed for the session");
        }
        if (attendanceStatus == AttendanceStatus.VERIFIED_ATTENDED || attendanceStatus == AttendanceStatus.VERIFIED_ABSENT) {
            throw new ConflictException("Attendance has already been reviewed for this member");
        }

        String eventType = attended ? EVENT_VERIFIED_ATTENDED : EVENT_VERIFIED_ABSENT;
        logAttendance(groupId, memberUserId, leaderUserId, eventType);
        notifyMemberAboutAttendanceReview(group, memberUserId, attended);
    }

    public ArrayList<StudyGroupMemberDto> listMembers(String sessionId, String groupId) {
        String userId = sessionResolver.requireUserId(sessionId);
        requireGroup(groupId);

        requireMembership(groupId, userId, "Join the group before viewing members");

        Map<String, AttendanceStatus> attendanceStatusesByUserId = buildAttendanceStatusesByUserId(groupId);

        return membershipRepo.findByGroupId(groupId).stream()
                .map(membership -> userRepo.findById(membership.getUserId())
                        .map(user -> new StudyGroupMemberDto(
                                user.getUserId(),
                                user.getDisplayName(),
                                user.getUsername(),
                                membership.isLeader(),
                                attendanceStatusesByUserId.getOrDefault(user.getUserId(), AttendanceStatus.NOT_RESPONDED)
                        )))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(StudyGroupMemberDto::isLeader).reversed()
                        .thenComparing(member -> member.displayName().toLowerCase(Locale.US)))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<StudyGroup> getAvailableGroups(String sessionId, String courseId) {
        String userId = sessionResolver.requireUserId(sessionId);
        String normalizedCourseId = normalizeCourseId(courseId);
        List<StudyGroup> candidates = groupSearchRepo.findByCourseIdIgnoreCase(normalizedCourseId);

        GroupFilterContext context = new GroupFilterContext()
                .addStrategy(new CourseFilterStrategy(normalizedCourseId))
                .addStrategy(new AvailableGroupsFilterStrategy(membershipRepo));

        List<StudyGroup> availableGroups = context.applyAll(candidates).stream()
                .filter(group -> !membershipRepo.existsByGroupIdAndUserId(group.getGroupId(), userId))
                .collect(Collectors.toList());

        return sortGroups(availableGroups);
    }

    public List<ManagedStudyGroupDto> getMyCreatedGroups(String sessionId){
        String userId = sessionResolver.requireUserId(sessionId);
        Map<String, GroupMembership> membershipsByGroupId = membershipRepo.findByUserId(userId).stream()
                .collect(Collectors.toMap(GroupMembership::getGroupId, Function.identity(), (first, second) -> first));

        return sortGroups(groupSearchRepo.findByCreatedByUserId(userId)).stream()
                .map(group -> toManagedStudyGroupDto(group, membershipsByGroupId.get(group.getGroupId()), buildAttendanceStatus(group.getGroupId(), userId)))
                .collect(Collectors.toList());
    }

    public List<ManagedStudyGroupDto> getMyJoinedGroups(String sessionId) {
        String userId = sessionResolver.requireUserId(sessionId);
        List<GroupMembership> memberships = membershipRepo.findByUserId(userId);
        Set<String> joinedGroupIds = memberships.stream()
                .map(GroupMembership::getGroupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, GroupMembership> membershipsByGroupId = memberships.stream()
                .collect(Collectors.toMap(GroupMembership::getGroupId, Function.identity(), (first, second) -> first));

        List<StudyGroup> joinedGroups = joinedGroupIds.stream()
                .map(groupRepo::findById)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());

        return sortGroups(joinedGroups).stream()
                .map(group -> toManagedStudyGroupDto(group, membershipsByGroupId.get(group.getGroupId()), buildAttendanceStatus(group.getGroupId(), userId)))
                .collect(Collectors.toList());
    }

    private void validateTimeWindow(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }
    }

    private String normalizeCourseId(String courseId) {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("courseId required");
        }
        return courseId.trim();
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title required");
        }
        return title.trim();
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }

    private String normalizeLocation(String location, boolean isVirtual) {
        String normalizedLocation = location == null ? "" : location.trim();

        if (!isVirtual && normalizedLocation.isBlank()) {
            throw new IllegalArgumentException("location is required for in-person groups");
        }

        return normalizedLocation;
    }

    private LocalDateTime normalizeStartTime(LocalDateTime startTime) {
        if (startTime == null) {
            throw new IllegalArgumentException("startTime required");
        }
        if (!startTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("startTime must be in the future");
        }
        return startTime;
    }

    private int normalizeCapacity(int maxCapacity) {
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("maxCapacity must be greater than 0");
        }
        return maxCapacity;
    }

    private int normalizeDurationMinutes(int durationMinutes) {
        if (durationMinutes <= 0) {
            return 60;
        }
        return durationMinutes;
    }

    private StudyGroup requireGroup(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException("groupId required");
        }
        return groupRepo.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));
    }

    private GroupMembership requireMembership(String groupId, String userId, String message) {
        return membershipRepo.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ConflictException(message));
    }

    private void logAttendance(String groupId, String userId, String eventType) {
        attendanceRepo.save(new AttendanceEvent(groupId, userId, eventType));
    }

    private void logAttendance(String groupId, String userId, String recordedByUserId, String eventType) {
        attendanceRepo.save(new AttendanceEvent(groupId, userId, recordedByUserId, eventType));
    }

    private AttendanceStatus buildAttendanceStatus(String groupId, String userId) {
        return resolveAttendanceStatus(attendanceRepo.findByGroupIdAndUserId(groupId, userId));
    }

    private Map<String, AttendanceStatus> buildAttendanceStatusesByUserId(String groupId) {
        return attendanceRepo.findByGroupId(groupId).stream()
                .collect(Collectors.groupingBy(AttendanceEvent::getUserId))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> resolveAttendanceStatus(entry.getValue())));
    }

    private AttendanceStatus resolveAttendanceStatus(List<AttendanceEvent> events) {
        if (events == null || events.isEmpty()) {
            return AttendanceStatus.NOT_RESPONDED;
        }

        boolean membershipActive = false;
        AttendanceStatus status = AttendanceStatus.NOT_RESPONDED;

        List<AttendanceEvent> orderedEvents = events.stream()
                .sorted(Comparator.comparing(AttendanceEvent::getRecordedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();

        for (AttendanceEvent event : orderedEvents) {
            switch (event.getEventType()) {
                case EVENT_JOINED -> {
                    membershipActive = true;
                    status = AttendanceStatus.NOT_RESPONDED;
                }
                case EVENT_LEFT -> {
                    membershipActive = false;
                    status = AttendanceStatus.NOT_RESPONDED;
                }
                case EVENT_RSVPED -> {
                    if (membershipActive) {
                        status = AttendanceStatus.RSVPED;
                    }
                }
                case EVENT_VERIFIED_ATTENDED, EVENT_LEGACY_CONFIRMED -> {
                    if (membershipActive) {
                        status = AttendanceStatus.VERIFIED_ATTENDED;
                    }
                }
                case EVENT_VERIFIED_ABSENT -> {
                    if (membershipActive) {
                        status = AttendanceStatus.VERIFIED_ABSENT;
                    }
                }
                default -> {
                    // Ignore unrelated attendance event types.
                }
            }
        }

        return membershipActive ? status : AttendanceStatus.NOT_RESPONDED;
    }

    private ManagedStudyGroupDto toManagedStudyGroupDto(StudyGroup group, GroupMembership membership, AttendanceStatus attendanceStatus) {
        return new ManagedStudyGroupDto(
                group.getGroupId(),
                group.getCourseId(),
                group.getTitle(),
                group.getDescription(),
                group.getLocation(),
                group.isVirtual(),
                group.getStartTime(),
                group.getDurationMinutes(),
                group.getMaxCapacity(),
                group.getCreatedByUserId(),
                group.getCreatedAt(),
                membership != null && membership.isLeader(),
                attendanceStatus
        );
    }

    private void notifyGroupCreatorAboutJoin(StudyGroup group, String joinedUserId) {
        if (group.getCreatedByUserId() == null || group.getCreatedByUserId().equals(joinedUserId)) {
            return;
        }

        String joinedDisplayName = userRepo.findById(joinedUserId)
                .map(UserProfile::getDisplayName)
                .orElse("A student");

        notificationService.createNotification(
                group.getCreatedByUserId(),
                NotificationService.TYPE_GROUP_JOINED,
                "New member joined your group",
                joinedDisplayName + " joined " + group.getTitle() + ".",
                Map.of("groupId", group.getGroupId(), "memberUserId", joinedUserId)
        );
    }

    private void notifyMemberAboutAttendanceReview(StudyGroup group, String memberUserId, boolean attended) {
        String title = attended ? "Attendance verified" : "Attendance marked absent";
        String body = attended
                ? "Your leader marked you present for " + group.getTitle() + "."
                : "Your leader marked you absent for " + group.getTitle() + ".";

        notificationService.createNotification(
                memberUserId,
                NotificationService.TYPE_ATTENDANCE_VERIFIED,
                title,
                body,
                Map.of("groupId", group.getGroupId(), "attended", Boolean.toString(attended))
        );
    }

    private List<StudyGroup> sortGroups(List<StudyGroup> groups) {
        return groups.stream()
                .sorted(Comparator.comparing(StudyGroup::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }
}
