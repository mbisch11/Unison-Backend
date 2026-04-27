package com._bit.Unison.calendar.service;

import com._bit.Unison.auth.service.SessionResolver;
import com._bit.Unison.calendar.dto.CalendarEventDto;
import com._bit.Unison.groups.model.GroupMembership;
import com._bit.Unison.groups.model.StudyGroup;
import com._bit.Unison.groups.repo.GroupMembershipRepository;
import com._bit.Unison.groups.repo.StudyGroupRepository;
import com._bit.Unison.groups.repo.StudyGroupSearchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CalendarServiceTest {

    @Test
    void listEvents_mergesJoinedAndCreatedGroups() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        StudyGroupRepository groupRepo = mock(StudyGroupRepository.class);
        StudyGroupSearchRepository groupSearchRepo = mock(StudyGroupSearchRepository.class);
        GroupMembershipRepository membershipRepo = mock(GroupMembershipRepository.class);
        CalendarService service = new CalendarService(sessionResolver, groupRepo, groupSearchRepo, membershipRepo);

        StudyGroup joinedGroup = new StudyGroup("CMPSC 131", "Joined Session", "Review", "Library", false, LocalDateTime.now().plusDays(1), 90, 5, "other");
        StudyGroup createdGroup = new StudyGroup("HCDD 440", "Created Session", "Studio work", "Studio", false, LocalDateTime.now().plusDays(2), 120, 6, "u1");
        ReflectionTestUtils.setField(joinedGroup, "groupId", "g1");
        ReflectionTestUtils.setField(createdGroup, "groupId", "g2");

        when(sessionResolver.requireUserId("s1")).thenReturn("u1");
        when(membershipRepo.findByUserId("u1")).thenReturn(List.of(new GroupMembership("g1", "u1", false)));
        when(groupSearchRepo.findByCreatedByUserId("u1")).thenReturn(List.of(createdGroup));
        when(groupRepo.findById("g1")).thenReturn(Optional.of(joinedGroup));
        when(groupRepo.findById("g2")).thenReturn(Optional.of(createdGroup));

        List<CalendarEventDto> events = service.listEvents("s1");

        assertEquals(2, events.size());
        assertEquals("g1", events.get(0).groupId());
        assertEquals(90, java.time.Duration.between(events.get(0).startTime(), events.get(0).endTime()).toMinutes());
    }

    @Test
    void exportCalendar_returnsIcsPayload() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        StudyGroupRepository groupRepo = mock(StudyGroupRepository.class);
        StudyGroupSearchRepository groupSearchRepo = mock(StudyGroupSearchRepository.class);
        GroupMembershipRepository membershipRepo = mock(GroupMembershipRepository.class);
        CalendarService service = new CalendarService(sessionResolver, groupRepo, groupSearchRepo, membershipRepo);

        StudyGroup group = new StudyGroup("CMPSC 131", "Joined Session", "Review", "Library", false, LocalDateTime.now().plusDays(1), 90, 5, "u1");
        ReflectionTestUtils.setField(group, "groupId", "g1");

        when(sessionResolver.requireUserId("s1")).thenReturn("u1");
        when(membershipRepo.findByUserId("u1")).thenReturn(List.of(new GroupMembership("g1", "u1", false)));
        when(groupSearchRepo.findByCreatedByUserId("u1")).thenReturn(List.of());
        when(groupRepo.findById("g1")).thenReturn(Optional.of(group));

        String ics = service.exportCalendar("s1");

        assertTrue(ics.contains("BEGIN:VCALENDAR"));
        assertTrue(ics.contains("SUMMARY:Joined Session"));
        assertTrue(ics.contains("END:VCALENDAR"));
    }
}
