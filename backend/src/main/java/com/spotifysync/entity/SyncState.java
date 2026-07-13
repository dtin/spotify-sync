package com.spotifysync.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "sync_states")
@Data
public class SyncState {
    @Id
    private String id;
    private String systemUserId;
    private boolean isFullySynced;
    
    private List<SyncedItem> syncedTracks = new ArrayList<>();
    private List<SyncedItem> ignoredTracks = new ArrayList<>();
    
    private List<SyncedItem> syncedPlaylists = new ArrayList<>();
    private List<SyncedItem> ignoredPlaylists = new ArrayList<>();
    
    private List<SyncedItem> syncedAlbums = new ArrayList<>();
    private List<SyncedItem> ignoredAlbums = new ArrayList<>();
}
