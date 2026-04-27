package com._bit.Unison.notifications.controller;

import com._bit.Unison.notifications.model.Notification;
import com._bit.Unison.notifications.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void listNotifications_returns200() throws Exception {
        Notification notification = new Notification("u1", "GROUP_JOINED", "Someone joined your group", "Michael joined.", Map.of(), null);
        ReflectionTestUtils.setField(notification, "notificationId", "n1");
        when(notificationService.listNotifications("s1")).thenReturn(List.of(notification));

        mvc.perform(get("/notifications").header("X-Session-Id", "s1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].notificationId").value("n1"))
                .andExpect(jsonPath("$[0].type").value("GROUP_JOINED"));
    }

    @Test
    void markRead_returns204() throws Exception {
        mvc.perform(post("/notifications/n1/read").header("X-Session-Id", "s1"))
                .andExpect(status().isNoContent());

        verify(notificationService).markRead("s1", "n1");
    }

    @Test
    void markAllRead_returns204() throws Exception {
        mvc.perform(post("/notifications/read-all").header("X-Session-Id", "s1"))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllRead("s1");
    }
}
