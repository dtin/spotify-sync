package com.spotifysync.service;

import com.spotifysync.dto.request.SyncRequest;
import com.spotifysync.dto.response.AlbumResponse;
import com.spotifysync.dto.response.PlaylistResponse;
import com.spotifysync.dto.response.TrackResponse;
import com.spotifysync.entity.SyncState;
import com.spotifysync.entity.SyncedItem;
import com.spotifysync.enums.AccountType;
import com.spotifysync.repository.SyncStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoSyncService {

    private final SyncStateRepository syncStateRepository;
    private final SyncService syncService;
    private final SpotifyApiService apiService;

    // Runs every day at 12:00 PM
    @Scheduled(cron = "0 0 12 * * ?")
    public void runDailyAutoSync() {
        log.info("Starting daily auto-sync cronjob...");
        List<SyncState> allStates = syncStateRepository.findAll();
        
        for (SyncState state : allStates) {
            if (state.isFullySynced()) {
                log.info("Triggering delta sync for user: {}", state.getSystemUserId());
                try {
                    triggerDeltaSyncForUser(state.getSystemUserId(), state);
                } catch (Exception e) {
                    log.error("Failed to auto-sync for user: {}", state.getSystemUserId(), e);
                }
            }
        }
    }
    
    public void triggerDeltaSyncForUser(String systemUserId, SyncState state) {
        if (state == null) {
            state = syncStateRepository.findBySystemUserId(systemUserId)
                .orElseThrow(() -> new RuntimeException("SyncState not found"));
        }
        
        List<TrackResponse> sourceTracks = apiService.getSavedTracks(systemUserId, AccountType.SOURCE);
        List<PlaylistResponse> sourcePlaylists = apiService.getUserPlaylists(systemUserId, AccountType.SOURCE);
        List<AlbumResponse> sourceAlbums = apiService.getSavedAlbums(systemUserId, AccountType.SOURCE);
        
        Set<String> processedTracks = getProcessedIds(state.getSyncedTracks(), state.getIgnoredTracks());
        Set<String> processedPlaylists = getProcessedIds(state.getSyncedPlaylists(), state.getIgnoredPlaylists());
        Set<String> processedAlbums = getProcessedIds(state.getSyncedAlbums(), state.getIgnoredAlbums());
        
        List<String> newTracks = sourceTracks.stream().map(TrackResponse::getTrackId)
                .filter(id -> !processedTracks.contains(id)).collect(Collectors.toList());
                
        List<String> newPlaylists = sourcePlaylists.stream().map(PlaylistResponse::getSpotifyPlaylistId)
                .filter(id -> !processedPlaylists.contains(id)).collect(Collectors.toList());
                
        List<String> newAlbums = sourceAlbums.stream().map(AlbumResponse::getSpotifyAlbumId)
                .filter(id -> !processedAlbums.contains(id)).collect(Collectors.toList());
                
        if (newTracks.isEmpty() && newPlaylists.isEmpty() && newAlbums.isEmpty()) {
            log.info("No new items to sync for user: {}", systemUserId);
            return;
        }
        
        log.info("Delta sync found {} new tracks, {} new playlists, {} new albums for user {}", 
                 newTracks.size(), newPlaylists.size(), newAlbums.size(), systemUserId);
                 
        SyncRequest deltaRequest = new SyncRequest();
        deltaRequest.setLikedSongIds(newTracks);
        deltaRequest.setPlaylistIds(newPlaylists);
        deltaRequest.setAlbumIds(newAlbums);
        
        syncService.startSync(systemUserId, deltaRequest);
    }
    
    private Set<String> getProcessedIds(List<SyncedItem> synced, List<SyncedItem> ignored) {
        Set<String> set = synced.stream().map(SyncedItem::getSpotifyId).collect(Collectors.toSet());
        if (ignored != null) {
            set.addAll(ignored.stream().map(SyncedItem::getSpotifyId).collect(Collectors.toSet()));
        }
        return set;
    }
}
