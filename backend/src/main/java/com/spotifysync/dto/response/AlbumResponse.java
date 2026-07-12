package com.spotifysync.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AlbumResponse {
    private String spotifyAlbumId;
    private String name;
    private String artistName;
    private String imageUrl;
    private int totalTracks;
    private String releaseDate;
    private boolean synced;
    private LocalDateTime lastSyncedAt;
}
