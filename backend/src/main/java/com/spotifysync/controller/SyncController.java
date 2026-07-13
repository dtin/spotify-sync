package com.spotifysync.controller;

import com.spotifysync.dto.request.SyncRequest;
import com.spotifysync.dto.response.AlbumResponse;
import com.spotifysync.dto.response.PlaylistResponse;
import com.spotifysync.dto.response.TrackResponse;
import com.spotifysync.enums.AccountType;
import com.spotifysync.service.SpotifyApiService;
import com.spotifysync.service.SyncService;
import com.spotifysync.service.AutoSyncService;
import com.spotifysync.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SpotifyApiService apiService;
    private final SyncService syncService;
    private final AutoSyncService autoSyncService;
    private final JwtUtil jwtUtil;

    @GetMapping("/playlists")
    public ResponseEntity<List<PlaylistResponse>> getPlaylists(java.security.Principal principal) {
        return ResponseEntity.ok(apiService.getUserPlaylists(principal.getName(), AccountType.SOURCE));
    }

    @GetMapping("/liked-songs")
    public ResponseEntity<List<TrackResponse>> getLikedSongs(java.security.Principal principal) {
        return ResponseEntity.ok(apiService.getSavedTracks(principal.getName(), AccountType.SOURCE));
    }

    @GetMapping("/albums")
    public ResponseEntity<List<AlbumResponse>> getAlbums(java.security.Principal principal) {
        return ResponseEntity.ok(apiService.getSavedAlbums(principal.getName(), AccountType.SOURCE));
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startSync(java.security.Principal principal,
                                                         @RequestBody SyncRequest request) {
        String systemUserId = principal.getName();
        syncService.startSync(systemUserId, request);
        return ResponseEntity.ok(Map.of("message", "Sync started successfully", "sessionId", systemUserId));
    }
    
    @PostMapping("/auto")
    public ResponseEntity<Map<String, String>> triggerAutoSync(java.security.Principal principal) {
        String systemUserId = principal.getName();
        autoSyncService.triggerDeltaSyncForUser(systemUserId, null);
        return ResponseEntity.ok(Map.of("message", "Auto-sync triggered successfully"));
    }


}
