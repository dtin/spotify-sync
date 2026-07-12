package com.spotifysync.repository;

import com.spotifysync.entity.SyncSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SyncSessionRepository extends JpaRepository<SyncSession, Long> {
    Optional<SyncSession> findTopByUserSessionIdOrderByStartedAtDesc(String userSessionId);
}
