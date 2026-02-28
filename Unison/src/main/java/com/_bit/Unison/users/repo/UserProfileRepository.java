package com._bit.Unison.users.repo;

import com._bit.Unison.users.model.UserProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserProfileRepository extends MongoRepository<UserProfile, String> {

    Optional<UserProfile> findByDisplayName(String displayName);
}