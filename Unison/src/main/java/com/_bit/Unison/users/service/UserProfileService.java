package com._bit.Unison.users.service;

import com._bit.Unison.users.model.UserProfile;
import com._bit.Unison.users.repo.UserProfileRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class UserProfileService {

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
	private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");

	private final UserProfileRepository userRepo;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	
	public UserProfileService(UserProfileRepository userRepo) {
		this.userRepo = userRepo;
	}

	public UserProfile registerUser(String displayName, String email, String username, String rawPassword, Set<String> courseIds){
		if (displayName == null || displayName.isBlank()){
			throw new IllegalArgumentException("displayName is required");
		} else if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("email is required");
		} else if (!EMAIL_PATTERN.matcher(email).matches()) {
			throw new IllegalArgumentException("email format is invalid");
		} else if (username == null || username.isBlank()) {
			throw new IllegalArgumentException("username is required");
		} else if (!USERNAME_PATTERN.matcher(username).matches()) {
			throw new IllegalArgumentException("username must be 3-20 characters and contain letters, numbers, or underscores");
		} else if (rawPassword == null || rawPassword.length() < 8) {
			throw new IllegalArgumentException("password must be at least 8 characters");
		}

		String normalizedEmail = email.trim().toLowerCase();
		String normalizedUsername = username.trim().toLowerCase();

		if (userRepo.existsByNormalizedEmail(normalizedEmail)){
			throw new IllegalArgumentException("email is already in use");
		} else if (userRepo.existsByNormalizedUsername(normalizedUsername)) {
			throw new IllegalArgumentException("username is already");
		}

		String passwordHash = passwordEncoder.encode(rawPassword);

		UserProfile user = new UserProfile(displayName.trim(), email.trim(), username.trim(), normalizedEmail, normalizedUsername, passwordHash, (courseIds == null) ? new HashSet<>() : courseIds);

		return userRepo.save(user);
	}

	public UserProfile authenticate(String emailOrUsername, String rawPassword){
		if (emailOrUsername == null || emailOrUsername.isBlank()){
			throw new IllegalArgumentException("Email or Username is required");
		} else if (rawPassword == null || rawPassword.isBlank()) {
			throw new IllegalArgumentException("Password is required");
		}

		String normalized = emailOrUsername.trim().toLowerCase();

		UserProfile user = userRepo.findByNormalizedEmail(normalized).or(() -> userRepo.findByNormalizedUsername(normalized)).orElseThrow(() -> new IllegalArgumentException("Invalid username or email"));

		if(!passwordEncoder.matches(rawPassword, user.getPasswordHash())){
			throw new IllegalArgumentException("Invalid Password");
		}

		return user;
	}

	public UserProfile getUser(String uId) {
		return userRepo.findById(uId).orElseThrow(() -> new RuntimeException("User not found:" + uId));
	}
}
