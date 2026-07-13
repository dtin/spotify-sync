package com.spotifysync.repository;

import com.spotifysync.entity.SyncTask;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SyncTaskRepository extends MongoRepository<SyncTask, String> {
}
