package com._bit.Unison.calendar.controller;

import com._bit.Unison.calendar.dto.CalendarEventDto;
import com._bit.Unison.calendar.service.CalendarService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping("/events")
    public List<CalendarEventDto> listEvents(@RequestHeader("X-Session-Id") String sessionId) {
        return calendarService.listEvents(sessionId);
    }

    @GetMapping(value = "/export.ics", produces = "text/calendar")
    public ResponseEntity<String> exportCalendar(@RequestHeader("X-Session-Id") String sessionId) {
        String calendarContent = calendarService.exportCalendar(sessionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"unison-calendar.ics\"")
                .contentType(MediaType.parseMediaType("text/calendar"))
                .body(calendarContent);
    }
}
