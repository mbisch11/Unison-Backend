package com._bit.Unison.users.controller;

import com._bit.Unison.users.model.UserProfile;
import com._bit.Unison.users.service.UserProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/users")
public class UserProfileController {
	
	private final UserProfileService userService;
	
	public UserProfileController(UserProfileService userService) {
		this.userService = userService;
	}
	
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UserProfile createUser(@RequestBody CreateUserRequest req) {
		return userService.registerUser(req.displayName, req.email, req.username, req.rawPassword, req.courseIds);
	}
	
	@GetMapping("/{id}")
	public UserProfile getUser(@PathVariable("id") String id) {
		return userService.getUser(id);
	}
	
	public static class CreateUserRequest {
		public String displayName;
		public String email;
		public String username;
		public String rawPassword;
		public Set<String> courseIds;
	}
}
