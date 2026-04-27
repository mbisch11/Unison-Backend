package com._bit.Unison.auth.controller;

import com._bit.Unison.auth.service.AuthService;
import com._bit.Unison.users.model.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthService.LoginResult signup(@RequestBody SignupRequest req){
        return authService.signup(req.displayName, req.email, req.username, req.password, req.courseIds);
    }

    @PostMapping("/admin/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthService.LoginResult adminSignup(@RequestBody AdminSignupRequest req) {
        return authService.adminSignup(req.token, req.displayName, req.email, req.username, req.password, req.courseIds);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthService.LoginResult login(@RequestBody LoginRequest req) {
        return authService.login(req.emailOrUsername, req.password);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader("X-Session-Id") String sessionId) {
        authService.logout(sessionId);
    }


    @GetMapping("/me")
    public UserProfile me(@RequestHeader("X-Session-Id") String sessionId) {
        return authService.me(sessionId);
    }

    public static class SignupRequest {
        public String displayName;
        public String email;
        public String username;
        public String password;
        public Set<String> courseIds;
    }

    public static class LoginRequest {
        public String emailOrUsername;
        public String password;
    }

    public static class AdminSignupRequest {
        public String token;
        public String displayName;
        public String email;
        public String username;
        public String password;
        public Set<String> courseIds;
    }
}
