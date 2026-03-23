package com._bit.Unison.auth.controller;

import com._bit.Unison.auth.model.AuthSession;
import com._bit.Unison.auth.repo.AuthSessionRepository;
import com._bit.Unison.auth.service.SessionResolver;
import com._bit.Unison.users.model.UserProfile;
import com._bit.Unison.users.repo.UserProfileRepository;
import com._bit.Unison.users.service.UserProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthSessionRepository sessionRepo;
    private final SessionResolver sessionResolver;
    private final UserProfileRepository userRepo;
    private final UserProfileService userService;

    public AuthController(AuthSessionRepository sessionRepo, SessionResolver sessionResolver, UserProfileRepository userRepo, UserProfileService userService) {
        this.sessionRepo = sessionRepo;
        this.sessionResolver = sessionResolver;
        this.userRepo = userRepo;
        this.userService = userService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse signup(@RequestBody SignupRequest req){
        UserProfile created = userService.registerUser(req.displayName, req.email, req.username, req.password, req.courseIds);

        return createSession(created.getUserId(), req.roles);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse login(@RequestBody LoginRequest req) {
        UserProfile user = userService.authenticate(req.emailOrUsername, req.password);
        return createSession(user.getUserId(), req.roles);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader("X-Session-Id") String sessionId) {
        sessionRepo.deleteById(sessionId);
    }


    @GetMapping("/me")
    public UserProfile me(@RequestHeader("X-Session-Id") String sessionId) {
        String userId = sessionResolver.requireUserId(sessionId);
        return userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private LoginResponse createSession(String userId, Set<String> roles) {
        String sessionId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        Set<String> safeRoles = (roles == null || roles.isEmpty()) ? Set.of("USER") : roles;

        AuthSession session = new AuthSession(sessionId, userId, safeRoles, expiresAt);
        sessionRepo.save(session);

        return new LoginResponse(sessionId, userId, expiresAt);
    }

    public static class SignupRequest {
        public String displayName;
        public String email;
        public String username;
        public String password;
        public Set<String> courseIds;
        public Set<String> roles;
    }

    public static class LoginRequest {
        public String emailOrUsername;
        public String password;
        public Set<String> roles;
    }

    public static class LoginResponse {
        public String sessionId;
        public String userId;
        public Instant expiresAt;

        public LoginResponse(String sessionId, String userId, Instant expiresAt) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.expiresAt = expiresAt;
        }
    }
}