package com.spotifysync.entity;

import com.spotifysync.enums.AccountType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class PlaylistInfo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String spotifyPlaylistId;
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String imageUrl;
    
    private String ownerName;
    private int totalTracks;
    private boolean isPublic;
    
    private String userSessionId;
    
    @Enumerated(EnumType.STRING)
    private AccountType accountType;
    
    private boolean synced;
    private LocalDateTime lastSyncedAt;
}
