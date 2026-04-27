package com._bit.Unison.auth.repo;

import com._bit.Unison.auth.model.AuthSession;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuthSessionRepository extends MongoRepository<AuthSession, String> {

    void deleteByUserId(String userId);
}
