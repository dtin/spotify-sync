package com.spotifysync.entity;

import com.spotifysync.enums.SyncStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
public class SyncSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    private SyncStatus status;
    
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    
    private int totalTasks;
    private int completedTasks;
    private int failedTasks;
    
    @OneToMany(mappedBy = "syncSession", cascade = CascadeType.ALL)
    private List<SyncTask> tasks;
    
    private String userSessionId;
}
