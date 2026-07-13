package com.spotifysync.entity;

import com.spotifysync.enums.SyncStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "sync_sessions")
@Data
public class SyncSession {
    @Id
    private String id;
    
    private SyncStatus status;
    
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    
    private int totalTasks;
    private int completedTasks;
    private int failedTasks;
    
    private List<SyncTask> tasks = new java.util.ArrayList<>(); // Stores the IDs of SyncTasks
    
    private String userSessionId;
}
