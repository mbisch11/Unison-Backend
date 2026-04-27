package com._bit.Unison.moderation.repo;

import com._bit.Unison.moderation.model.AdminInvite;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AdminInviteRepository extends MongoRepository<AdminInvite, String> {

    Optional<AdminInvite> findByTokenHash(String tokenHash);

    List<AdminInvite> findAllByOrderByCreatedAtDesc();

    List<AdminInvite> findByBootstrapTrueAndUsedAtIsNullAndRevokedAtIsNull();
}
