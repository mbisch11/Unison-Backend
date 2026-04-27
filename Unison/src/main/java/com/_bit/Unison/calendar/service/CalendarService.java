package com._bit.Unison.calendar.service;

import com._bit.Unison.auth.service.SessionResolver;
import com._bit.Unison.calendar.dto.CalendarEventDto;
import com._bit.Unison.groups.model.GroupMembership;
import com._bit.Unison.groups.model.StudyGroup;
import com._bit.Unison.groups.repo.GroupMembershipRepository;
import com._bit.Unison.groups.repo.StudyGroupRepository;
import com._bit.Unison.groups.repo.StudyGroupSearchRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CalendarService {

    private static final DateTimeFormatter ICS_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private final SessionResolver sessionResolver;
    private final StudyGroupRepository groupRepo;
    private final StudyGroupSearchRepository groupSearchRepo;
    private final GroupMembershipRepository membershipRepo;

    public CalendarService(
            SessionResolver sessionResolver,
            StudyGroupRepository groupRepo,
            StudyGroupSearchRepository groupSearchRepo,
            GroupMembershipRepository membershipRepo
    ) {
        this.sessionResolver = sessionResolver;
        this.groupRepo = groupRepo;
        this.groupSearchRepo = groupSearchRepo;
        this.membershipRepo = membershipRepo;
    }

    public List<CalendarEventDto> listEvents(String sessionId) {
        String userId = sessionResolver.requireUserId(sessionId);

        Set<String> groupIds = membershipRepo.findByUserId(userId).stream()
                .map(GroupMembership::getGroupId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        groupSearchRepo.findByCreatedByUserId(userId).stream()
                .map(StudyGroup::getGroupId)
                .forEach(groupIds::add);

        return groupIds.stream()
                .map(groupRepo::findById)
                .flatMap(optionalGroup -> optionalGroup.stream())
                .map(this::toCalendarEvent)
                .sorted(Comparator.comparing(CalendarEventDto::startTime))
                .toList();
    }

    public String exportCalendar(String sessionId) {
        List<CalendarEventDto> events = listEvents(sessionId);
        StringBuilder builder = new StringBuilder();
        builder.append("BEGIN:VCALENDAR\r\n");
        builder.append("VERSION:2.0\r\n");
        builder.append("PRODID:-//Unison//Study Group Calendar//EN\r\n");
        builder.append("CALSCALE:GREGORIAN\r\n");

        for (CalendarEventDto event : events) {
            builder.append("BEGIN:VEVENT\r\n");
            builder.append("UID:").append(escapeText(event.groupId())).append("@unison\r\n");
            builder.append("DTSTAMP:").append(toIcsTimestamp(LocalDateTime.now())).append("\r\n");
            builder.append("DTSTART:").append(toIcsTimestamp(event.startTime())).append("\r\n");
            builder.append("DTEND:").append(toIcsTimestamp(event.endTime())).append("\r\n");
            builder.append("SUMMARY:").append(escapeText(event.title())).append("\r\n");
            builder.append("DESCRIPTION:").append(escapeText(event.description())).append("\r\n");
            builder.append("LOCATION:").append(escapeText(event.location())).append("\r\n");
            builder.append("END:VEVENT\r\n");
        }

        builder.append("END:VCALENDAR\r\n");
        return builder.toString();
    }

    private CalendarEventDto toCalendarEvent(StudyGroup group) {
        LocalDateTime endTime = group.getStartTime().plusMinutes(group.getDurationMinutes());
        return new CalendarEventDto(
                "group-" + group.getGroupId(),
                group.getGroupId(),
                group.getCourseId(),
                group.getTitle(),
                group.getDescription(),
                group.getLocation(),
                group.isVirtual(),
                group.getStartTime(),
                endTime
        );
    }

    private String toIcsTimestamp(LocalDateTime value) {
        return value.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("UTC"))
                .toLocalDateTime()
                .format(ICS_TIMESTAMP);
    }

    private String escapeText(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n");
    }
}
