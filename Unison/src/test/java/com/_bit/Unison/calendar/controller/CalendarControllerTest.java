package com._bit.Unison.calendar.controller;

import com._bit.Unison.calendar.dto.CalendarEventDto;
import com._bit.Unison.calendar.service.CalendarService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CalendarController.class)
class CalendarControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CalendarService calendarService;

    @Test
    void listEvents_returns200() throws Exception {
        when(calendarService.listEvents("s1")).thenReturn(List.of(
                new CalendarEventDto(
                        "event-1",
                        "g1",
                        "CMPSC 131",
                        "Joined Session",
                        "Review",
                        "Library",
                        false,
                        LocalDateTime.parse("2030-01-01T18:00:00"),
                        LocalDateTime.parse("2030-01-01T19:30:00")
                )
        ));

        mvc.perform(get("/calendar/events").header("X-Session-Id", "s1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupId").value("g1"))
                .andExpect(jsonPath("$[0].title").value("Joined Session"));
    }

    @Test
    void exportCalendar_returnsIcsAttachment() throws Exception {
        when(calendarService.exportCalendar("s1")).thenReturn("BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n");

        mvc.perform(get("/calendar/export.ics").header("X-Session-Id", "s1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"unison-calendar.ics\""))
                .andExpect(content().contentTypeCompatibleWith("text/calendar"))
                .andExpect(content().string("BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n"));
    }
}
