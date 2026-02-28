package com._bit.Unison.users.service;

import com._bit.Unison.users.model.UserProfile;
import com._bit.Unison.users.repo.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class UserProfileService {
	private final UserProfileRepository userRepo;
	
	public UserProfileService(UserProfileRepository userRepo) {
		this.userRepo = userRepo;
	}
	
	public UserProfile createUser(String displayName, Set<String> CourseIds) {
		UserProfile user = new UserProfile(displayName, CourseIds);
		return userRepo.save(user);
	}
	
	public UserProfile getUser(String uId) {
		return userRepo.findById(uId).orElseThrow(() -> new RuntimeException("User not found:" + uId));
	}
}
