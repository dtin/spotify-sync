package com.spotifysync.repository;

import com.spotifysync.entity.SyncSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SyncSessionRepository extends MongoRepository<SyncSession, String> {
    Optional<SyncSession> findTopByUserSessionIdOrderByStartedAtDesc(String userSessionId);
}
