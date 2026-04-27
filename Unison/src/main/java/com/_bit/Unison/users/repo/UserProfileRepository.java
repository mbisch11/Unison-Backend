package com._bit.Unison.users.repo;

import com._bit.Unison.users.model.UserProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends MongoRepository<UserProfile, String> {

    Optional<UserProfile> findByDisplayName(String displayName);

    Optional<UserProfile> findByNormalizedEmail(String normalizedEmail);

    Optional<UserProfile> findByNormalizedUsername(String normalizedUsername);

    boolean existsByNormalizedEmail(String normalizedEmail);

    boolean existsByNormalizedUsername(String normalizedUsername);

    boolean existsByNormalizedEmailAndUserIdNot(String normalizedEmail, String userId);

    boolean existsByNormalizedUsernameAndUserIdNot(String normalizedUsername, String userId);

    boolean existsByRolesContaining(String role);

    List<UserProfile> findByRolesContaining(String role);
}
