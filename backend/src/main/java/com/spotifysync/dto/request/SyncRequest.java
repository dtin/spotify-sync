package com.spotifysync.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class SyncRequest {
    private boolean syncLikedSongs;
    private List<String> playlistIds;
    private List<String> albumIds;
}
