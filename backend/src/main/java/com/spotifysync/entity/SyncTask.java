package com.spotifysync.entity;

import com.spotifysync.enums.SyncStatus;
import com.spotifysync.enums.SyncTaskType;
import org.springframework.data.annotation.Id;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

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

    // Live track-level progress
    private String currentTrackName;
    private String currentArtistName;
    private List<SyncedTrackInfo> recentlySyncedTracks = new ArrayList<>();

    private String errorMessage;

    @Data
    public static class SyncedTrackInfo {
        private String trackName;
        private String artistName;
        private String status; // "SYNCED" or "SKIPPED"

        public SyncedTrackInfo(String trackName, String artistName, String status) {
            this.trackName = trackName;
            this.artistName = artistName;
            this.status = status;
        }
    }
}
