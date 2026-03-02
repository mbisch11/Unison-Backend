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
		return userService.createUser(req.displayName, req.courseIds);
	}
	
	@GetMapping("/{id}")
	public UserProfile getUser(@PathVariable("id") String uId) {
		return userService.getUser(uId);
	}
	
	public static class CreateUserRequest {
		public String displayName;
		public Set<String> courseIds;
	}
}
