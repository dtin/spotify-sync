package com.spotifysync.dto.request;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class SyncRequest {
    private List<String> likedSongIds;
    private List<String> ignoredSongIds;
    private List<String> playlistIds;
    private List<String> ignoredPlaylistIds;
    private List<String> albumIds;
    private List<String> ignoredAlbumIds;

    // Track metadata for live progress display: trackId -> {name, artist}
    private Map<String, TrackMeta> trackMeta;

    @Data
    public static class TrackMeta {
        private String name;
        private String artist;
    }
}
