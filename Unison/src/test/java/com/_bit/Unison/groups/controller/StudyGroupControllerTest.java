package com._bit.Unison.groups.controller;

import com._bit.Unison.groups.dto.AttendanceStatus;
import com._bit.Unison.groups.dto.ManagedStudyGroupDto;
import com._bit.Unison.groups.dto.StudyGroupMemberDto;
import com._bit.Unison.groups.model.StudyGroup;
import com._bit.Unison.groups.service.StudyGroupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudyGroupController.class)
class StudyGroupControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private StudyGroupService groupService;

    @Test
    void createGroup_returns201() throws Exception {
        StudyGroup group = studyGroup("g1", LocalDateTime.now().plusDays(1));
        when(groupService.createGroup(eq("s1"), eq("CMPSC 131"), eq("Midterm Prep"), eq("Review session"), eq("Library"), eq(false), any(LocalDateTime.class), eq(5), eq(90)))
                .thenReturn(group);

        mvc.perform(post("/groups")
                        .header("X-Session-Id", "s1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "CMPSC 131",
                                  "title": "Midterm Prep",
                                  "description": "Review session",
                                  "location": "Library",
                                  "isVirtual": false,
                                  "startTime": "2026-04-21T18:00:00Z",
                                  "durationMinutes": 90,
                                  "maxCapacity": 5
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupId").value("g1"));
    }

    @Test
    void searchGroups_withInvalidDate_returns400() throws Exception {
        mvc.perform(get("/groups/search")
                        .header("X-Session-Id", "s1")
                        .param("courseId", "CMPSC 131")
                        .param("from", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from must be a valid ISO date-time"));
    }

    @Test
    void getAvailableGroups_returns200() throws Exception {
        when(groupService.getAvailableGroups("s1", "CMPSC 131"))
                .thenReturn(List.of(studyGroup("g1", LocalDateTime.now().plusDays(1))));

        mvc.perform(get("/groups/available")
                        .header("X-Session-Id", "s1")
                        .param("courseId", "CMPSC 131"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].groupId").value("g1"));
    }

    @Test
    void getMyJoinedGroups_returns200() throws Exception {
        when(groupService.getMyJoinedGroups("s1"))
                .thenReturn(List.of(new ManagedStudyGroupDto(
                        "g2",
                        "CMPSC 131",
                        "Title",
                        "Desc",
                        "Library",
                        false,
                        LocalDateTime.now().plusDays(2),
                        60,
                        5,
                        "u1",
                        LocalDateTime.now(),
                        false,
                        AttendanceStatus.RSVPED
                )));

        mvc.perform(get("/groups/my-joined").header("X-Session-Id", "s1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupId").value("g2"))
                .andExpect(jsonPath("$[0].currentUserAttendanceStatus").value("RSVPED"));
    }

    @Test
    void join_leave_rsvp_and_verify_return204() throws Exception {
        mvc.perform(post("/groups/g1/join").header("X-Session-Id", "s1"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/groups/g1/leave").header("X-Session-Id", "s1"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/groups/g1/rsvp").header("X-Session-Id", "s1"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/groups/g1/attendance/u2/verify")
                        .header("X-Session-Id", "s1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "attended": true
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(groupService).joinGroup("s1", "g1");
        verify(groupService).leaveGroup("s1", "g1");
        verify(groupService).rsvpAttendance("s1", "g1");
        verify(groupService).verifyAttendance("s1", "g1", "u2", true);
    }

    @Test
    void listMembers_returns200() throws Exception {
        when(groupService.listMembers("s1", "g1")).thenReturn(new java.util.ArrayList<>(List.of(
                new StudyGroupMemberDto("u1", "Michael", "michael", true, AttendanceStatus.RSVPED)
        )));

        mvc.perform(get("/groups/g1/members").header("X-Session-Id", "s1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("u1"))
                .andExpect(jsonPath("$[0].isLeader").value(true));
    }

    private StudyGroup studyGroup(String groupId, LocalDateTime startTime) {
        StudyGroup group = new StudyGroup("CMPSC 131", "Title", "Desc", "Library", false, startTime, 5, "u1");
        ReflectionTestUtils.setField(group, "groupId", groupId);
        return group;
    }
}
