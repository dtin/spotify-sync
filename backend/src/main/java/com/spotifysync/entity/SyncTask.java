package com.spotifysync.entity;

import com.spotifysync.enums.SyncStatus;
import com.spotifysync.enums.SyncTaskType;
import org.springframework.data.annotation.Id;
import lombok.Data;

@Data
public class SyncTask {
    private String id;
    

    private SyncTaskType type; // PLAYLIST, LIKED_SONGS, ALBUM
    
    // For PLAYLIST
    private String sourcePlaylistId;
    private String sourcePlaylistName;
    private String sourcePlaylistImageUrl;
    private String targetPlaylistId;
    
    // For ALBUM
    private String sourceAlbumId;
    private String sourceAlbumName;
    private String sourceAlbumImageUrl;
    private String sourceAlbumArtist;
    

    private SyncStatus status;
    
    private int totalTracks;
    private int syncedTracks;
    private int skippedTracks;
    

    private String errorMessage;
}
