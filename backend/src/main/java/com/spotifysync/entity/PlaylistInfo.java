package com.spotifysync.entity;

import com.spotifysync.enums.AccountType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

import java.time.LocalDateTime;

@Document(collection = "playlist_info")
@Data
public class PlaylistInfo {
    @Id
    private String id;
    
    private String spotifyPlaylistId;
    private String name;
    
    private String description;
    
    private String imageUrl;
    
    private String ownerName;
    private int totalTracks;
    private boolean isPublic;
    
    private String userSessionId;
    
    private AccountType accountType;
    
    private boolean synced;
    private LocalDateTime lastSyncedAt;
}
