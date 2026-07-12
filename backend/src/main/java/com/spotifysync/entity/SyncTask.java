package com.spotifysync.entity;

import com.spotifysync.enums.SyncStatus;
import com.spotifysync.enums.SyncTaskType;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class SyncTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "sync_session_id")
    private SyncSession syncSession;
    
    @Enumerated(EnumType.STRING)
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
    
    @Enumerated(EnumType.STRING)
    private SyncStatus status;
    
    private int totalTracks;
    private int syncedTracks;
    private int skippedTracks;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}
