package com._bit.Unison.users.controller;

import com._bit.Unison.users.model.UserProfile;
import com._bit.Unison.users.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.Mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserProfileController.class)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserProfileService userService;

    @Test
    void createUser_returns201_andUserJson() throws Exception {
        UserProfile saved = new UserProfile("Michael", Set.of("HCDD440"));
        when(userService.createUser(eq("Michael"), anySet())).thenReturn(saved);

        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(
                                """
                                {"displayName":"Michael","courseIds":["HCDD440"]}
                                """)).andExpect(status().isCreated()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.displayName").value("Michael"));

        verify(userService).createUser(eq("Michael"), anySet());
    }

    @Test
    void getUser_returns200_andUserJson() throws Exception {
        UserProfile u = new UserProfile("Michael", Set.of("HCDD440"));
        when(userService.getUser("u1")).thenReturn(u);

        mvc.perform(get("/users/u1")).andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("Michael"));

        verify(userService).getUser("u1");
    }
}