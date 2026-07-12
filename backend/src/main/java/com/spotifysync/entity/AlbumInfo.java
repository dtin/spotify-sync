package com.spotifysync.entity;

import com.spotifysync.enums.AccountType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class AlbumInfo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String spotifyAlbumId;
    private String name;
    private String artistName;
    
    @Column(columnDefinition = "TEXT")
    private String imageUrl;
    
    private int totalTracks;
    private String releaseDate;
    
    private String userSessionId;
    
    @Enumerated(EnumType.STRING)
    private AccountType accountType;
    
    private boolean synced;
    private LocalDateTime lastSyncedAt;
}
