package com.spotifysync.dto.response;

import com.spotifysync.enums.SyncStatus;
import lombok.Data;
import java.util.List;

@Data
public class SyncProgressDTO {
    private String userSessionId;
    private SyncStatus status;
    private int totalTasks;
    private int completedTasks;
    private int failedTasks;
    private double overallProgressPercent;
    private List<SyncTaskDTO> tasks;
}
