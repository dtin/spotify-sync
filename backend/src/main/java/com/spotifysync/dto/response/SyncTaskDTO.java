package com.spotifysync.dto.response;

import com.spotifysync.enums.SyncStatus;
import com.spotifysync.enums.SyncTaskType;
import lombok.Data;

@Data
public class SyncTaskDTO {
    private String taskId;
    private SyncTaskType type;       // PLAYLIST, LIKED_SONGS, ALBUM
    private String itemName;         // Playlist name / "Liked Songs" / Album name
    private String itemImageUrl;
    private SyncStatus status;
    private int totalTracks;
    private int syncedTracks;
    private int skippedTracks;
    private double progressPercent;
    private String errorMessage;
}
