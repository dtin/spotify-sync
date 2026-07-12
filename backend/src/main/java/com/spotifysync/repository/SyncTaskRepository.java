package com.spotifysync.repository;

import com.spotifysync.entity.SyncTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncTaskRepository extends JpaRepository<SyncTask, Long> {
}
