package com.spotifysync.repository;

import com.spotifysync.entity.SyncState;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface SyncStateRepository extends MongoRepository<SyncState, String> {
    Optional<SyncState> findBySystemUserId(String systemUserId);
}
