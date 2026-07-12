package com.spotifysync.dto.response;

import lombok.Data;

@Data
public class TrackResponse {
    private String trackId;
    private String name;
    private String artistName;
    private String albumName;
    private String albumImageUrl;
    private String addedAt;
    private boolean synced;
}
