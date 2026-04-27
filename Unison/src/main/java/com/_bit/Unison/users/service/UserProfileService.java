package com._bit.Unison.users.service;

import com._bit.Unison.auth.service.SessionResolver;
import com._bit.Unison.common.AuthenticationFailedException;
import com._bit.Unison.common.ConflictException;
import com._bit.Unison.common.NotFoundException;
import com._bit.Unison.users.model.UserProfile;
import com._bit.Unison.users.repo.UserProfileRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Service
public class UserProfileService {

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");

	private final UserProfileRepository userRepo;
	private final SessionResolver sessionResolver;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	
	public UserProfileService(UserProfileRepository userRepo, SessionResolver sessionResolver) {
		this.userRepo = userRepo;
		this.sessionResolver = sessionResolver;
	}

	public UserProfile registerUser(String displayName, String email, String username, String password, Set<String> courseIds){
		return registerUser(displayName, email, username, password, courseIds, Set.of(ROLE_USER));
	}

	public UserProfile registerUser(String displayName, String email, String username, String password, Set<String> courseIds, Set<String> roles){
		validateEditableFields(displayName, email, username);
		validatePassword(password);

		String trimmedDisplayName = displayName.trim();
		String trimmedEmail = email.trim();
		String trimmedUsername = username.trim();
		String normalizedEmail = normalizeEmail(email);
		String normalizedUsername = normalizeUsername(username);
		Set<String> normalizedCourseIds = normalizeCourseIds(courseIds);
		Set<String> normalizedRoles = normalizeRoles(roles);

		if (userRepo.existsByNormalizedEmail(normalizedEmail)){
			throw new ConflictException("email is already in use");
		} else if (userRepo.existsByNormalizedUsername(normalizedUsername)) {
			throw new ConflictException("username is already in use");
		}

		String passwordHash = passwordEncoder.encode(password);

		UserProfile user = new UserProfile(
				trimmedDisplayName,
				trimmedEmail,
				trimmedUsername,
				normalizedEmail,
				normalizedUsername,
				passwordHash,
				normalizedCourseIds,
				normalizedRoles
		);

		return userRepo.save(user);
	}

	public UserProfile updateCurrentUser(String sessionId, String displayName, String email, String username, Set<String> courseIds) {
		String userId = sessionResolver.requireUserId(sessionId);
		UserProfile user = userRepo.findById(userId)
				.orElseThrow(() -> new NotFoundException("User not found: " + userId));

		validateEditableFields(displayName, email, username);

		String trimmedDisplayName = displayName.trim();
		String trimmedEmail = email.trim();
		String trimmedUsername = username.trim();
		String normalizedEmail = normalizeEmail(email);
		String normalizedUsername = normalizeUsername(username);
		Set<String> normalizedCourseIds = normalizeCourseIds(courseIds);

		if (userRepo.existsByNormalizedEmailAndUserIdNot(normalizedEmail, userId)) {
			throw new ConflictException("email is already in use");
		}

		if (userRepo.existsByNormalizedUsernameAndUserIdNot(normalizedUsername, userId)) {
			throw new ConflictException("username is already in use");
		}

		user.updateDisplayName(trimmedDisplayName);
		user.updateEmail(trimmedEmail, normalizedEmail);
		user.updateUsername(trimmedUsername, normalizedUsername);
		user.setCourseIds(normalizedCourseIds);

		return userRepo.save(user);
	}

	public UserProfile authenticate(String emailOrUsername, String rawPassword){
		if (emailOrUsername == null || emailOrUsername.isBlank()){
			throw new IllegalArgumentException("Email or Username is required");
		} else if (rawPassword == null || rawPassword.isBlank()) {
			throw new IllegalArgumentException("Password is required");
		}

		String normalized = emailOrUsername.trim().toLowerCase();

		UserProfile user = userRepo.findByNormalizedEmail(normalized)
				.or(() -> userRepo.findByNormalizedUsername(normalized))
				.orElseThrow(() -> new AuthenticationFailedException("Invalid username/email or password"));

		if(!passwordEncoder.matches(rawPassword, user.getPasswordHash())){
			throw new AuthenticationFailedException("Invalid username/email or password");
		}

		return user;
	}

	public UserProfile getUser(String uId) {
		if (uId == null || uId.isBlank()) {
			throw new IllegalArgumentException("user id is required");
		}

		return userRepo.findById(uId)
				.orElseThrow(() -> new NotFoundException("User not found: " + uId));
	}

	private void validateEditableFields(String displayName, String email, String username) {
		if (displayName == null || displayName.isBlank()){
			throw new IllegalArgumentException("displayName is required");
		} else if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("email is required");
		} else if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
			throw new IllegalArgumentException("email format is invalid");
		} else if (username == null || username.isBlank()) {
			throw new IllegalArgumentException("username is required");
		} else if (!USERNAME_PATTERN.matcher(username.trim()).matches()) {
			throw new IllegalArgumentException("username must be 3-20 characters and contain letters, numbers, or underscores");
		}
	}

	private void validatePassword(String password) {
		if (password == null || password.length() < 8) {
			throw new IllegalArgumentException("password must be at least 8 characters");
		}
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase();
	}

	private String normalizeUsername(String username) {
		return username.trim().toLowerCase();
	}

	private Set<String> normalizeCourseIds(Set<String> courseIds) {
		if (courseIds == null) {
			return new HashSet<>();
		}

		return courseIds.stream()
				.filter(courseId -> courseId != null && !courseId.isBlank())
				.map(String::trim)
				.collect(Collectors.toCollection(HashSet::new));
	}

	private Set<String> normalizeRoles(Set<String> roles) {
		if (roles == null || roles.isEmpty()) {
			return new HashSet<>(Set.of(ROLE_USER));
		}

		Set<String> normalizedRoles = roles.stream()
				.filter(role -> role != null && !role.isBlank())
				.map(String::trim)
				.map(String::toUpperCase)
				.collect(Collectors.toCollection(HashSet::new));

		if (normalizedRoles.isEmpty()) {
			normalizedRoles.add(ROLE_USER);
		}

		return normalizedRoles;
	}
}
