package com.spotifysync.entity;

import com.spotifysync.enums.AccountType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

import java.time.LocalDateTime;

@Document(collection = "album_info")
@Data
public class AlbumInfo {
    @Id
    private String id;
    
    private String spotifyAlbumId;
    private String name;
    private String artistName;
    
    private String imageUrl;
    
    private int totalTracks;
    private String releaseDate;
    
    private String userSessionId;
    
    private AccountType accountType;
    
    private boolean synced;
    private LocalDateTime lastSyncedAt;
}
