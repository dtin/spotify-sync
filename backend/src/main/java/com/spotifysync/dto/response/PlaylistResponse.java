package com.spotifysync.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PlaylistResponse {
    private String spotifyPlaylistId;
    private String name;
    private String description;
    private String imageUrl;
    private String ownerName;
    private int totalTracks;
    private boolean isPublic;
    private boolean synced;
    private LocalDateTime lastSyncedAt;
}
