package com.spotifysync.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class SyncRequest {
    private List<String> likedSongIds;
    private List<String> ignoredSongIds;
    private List<String> playlistIds;
    private List<String> ignoredPlaylistIds;
    private List<String> albumIds;
    private List<String> ignoredAlbumIds;
}
