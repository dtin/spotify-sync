package com.spotifysync.controller;

import com.spotifysync.dto.request.SyncRequest;
import com.spotifysync.dto.response.AlbumResponse;
import com.spotifysync.dto.response.PlaylistResponse;
import com.spotifysync.dto.response.TrackResponse;
import com.spotifysync.enums.AccountType;
import com.spotifysync.service.SpotifyApiService;
import com.spotifysync.service.SyncService;
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

    @GetMapping("/playlists")
    public ResponseEntity<List<PlaylistResponse>> getPlaylists(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(apiService.getUserPlaylists(extractToken(authHeader), AccountType.SOURCE));
    }

    @GetMapping("/liked-songs")
    public ResponseEntity<List<TrackResponse>> getLikedSongs(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(apiService.getSavedTracks(extractToken(authHeader), AccountType.SOURCE));
    }

    @GetMapping("/albums")
    public ResponseEntity<List<AlbumResponse>> getAlbums(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(apiService.getSavedAlbums(extractToken(authHeader), AccountType.SOURCE));
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startSync(@RequestHeader("Authorization") String authHeader,
                                                         @RequestBody SyncRequest request) {
        String userSessionId = extractToken(authHeader);
        syncService.startSync(userSessionId, request);
        return ResponseEntity.ok(Map.of("message", "Sync started successfully", "sessionId", userSessionId));
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Missing or invalid Authorization header");
    }
}
