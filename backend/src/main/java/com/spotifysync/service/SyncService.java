package com.spotifysync.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.spotifysync.dto.request.SyncRequest;
import com.spotifysync.dto.response.PlaylistResponse;
import com.spotifysync.dto.response.SyncProgressDTO;
import com.spotifysync.dto.response.SyncTaskDTO;
import com.spotifysync.entity.SyncSession;
import com.spotifysync.entity.SyncTask;
import com.spotifysync.enums.AccountType;
import com.spotifysync.enums.SyncStatus;
import com.spotifysync.enums.SyncTaskType;
import com.spotifysync.repository.SyncSessionRepository;
import com.spotifysync.repository.SyncStateRepository;
import com.spotifysync.entity.SyncState;
import com.spotifysync.entity.SyncedItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SyncService {

    private final SpotifyAuthService authService;
    private final SpotifyApiService apiService;
    private final SyncProgressService progressService;
    private final SyncSessionRepository sessionRepository;
    private final SyncStateRepository syncStateRepository;
    private final SpotifyApiClient restTemplate;
    private final ObjectMapper objectMapper;
    private final Executor taskExecutor;

    @Value("${spotify.api.host:https://api.spotify.com}")
    private String spotifyApiHost;

    @Value("${spotify.api.paths.tracks:/v1/me/tracks}")
    private String pathTracks;

    @Value("${spotify.api.paths.albums:/v1/me/albums}")
    private String pathAlbums;

    @Value("${spotify.api.paths.me:/v1/me}")
    private String pathMe;

    @Value("${spotify.api.paths.users:/v1/users}")
    private String pathUsers;

    @Value("${spotify.api.paths.base-playlists:/v1/playlists}")
    private String pathBasePlaylists;

    @Value("${spotify.api.limit.sync-playlist-tracks:100}")
    private int syncPlaylistTracksLimit;

    public SyncService(SpotifyAuthService authService, SpotifyApiService apiService,
                       SyncProgressService progressService, SyncSessionRepository sessionRepository,
                       SyncStateRepository syncStateRepository,
                       SpotifyApiClient restTemplate, ObjectMapper objectMapper,
                       @Qualifier("syncTaskExecutor") Executor taskExecutor) {
        this.authService = authService;
        this.apiService = apiService;
        this.progressService = progressService;
        this.sessionRepository = sessionRepository;
        this.syncStateRepository = syncStateRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
    }

    @Async("syncTaskExecutor")
    public void startSync(String userSessionId, SyncRequest request) {
        SyncSession session = new SyncSession();
        session.setUserSessionId(userSessionId);
        session.setStatus(SyncStatus.IN_PROGRESS);
        session.setStartedAt(LocalDateTime.now());
        
        List<SyncTask> tasks = new ArrayList<>();
        
        if (request.getLikedSongIds() != null && !request.getLikedSongIds().isEmpty()) {
            SyncTask t = new SyncTask();
                        t.setType(SyncTaskType.LIKED_SONGS);
            t.setStatus(SyncStatus.PENDING);
            tasks.add(t);
        }
        
        if (request.getPlaylistIds() != null) {
            for (String id : request.getPlaylistIds()) {
                SyncTask t = new SyncTask();
                                t.setType(SyncTaskType.PLAYLIST);
                t.setSourcePlaylistId(id);
                t.setStatus(SyncStatus.PENDING);
                tasks.add(t);
            }
        }
        
        if (request.getAlbumIds() != null) {
            for (String id : request.getAlbumIds()) {
                SyncTask t = new SyncTask();
                                t.setType(SyncTaskType.ALBUM);
                t.setSourceAlbumId(id);
                t.setStatus(SyncStatus.PENDING);
                tasks.add(t);
            }
        }
        
        session.setTasks(tasks);
        session.setTotalTasks(tasks.size());
        sessionRepository.save(session);
        
        // Broadcast initial state
        broadcastProgress(session);

        String sourceToken;
        String destToken;
        String destUserId;
        try {
            sourceToken = authService.getValidAccessToken(userSessionId, AccountType.SOURCE);
            destToken = authService.getValidAccessToken(userSessionId, AccountType.DESTINATION);
            destUserId = getUserId(destToken);
        } catch (Exception e) {
            log.error("Failed to initialize sync session", e);
            session.setStatus(SyncStatus.FAILED);
            for (SyncTask task : tasks) {
                task.setStatus(SyncStatus.FAILED);
                task.setErrorMessage(e.getMessage());
            }
            session.setFailedTasks(tasks.size());
            updateSessionState(session);
            return;
        }

        List<CompletableFuture<Void>> futures = tasks.stream().map(task -> CompletableFuture.runAsync(() -> {
            synchronized (session) {
                task.setStatus(SyncStatus.IN_PROGRESS);
                updateSessionState(session);
            }
            
            try {
                if (task.getType() == SyncTaskType.LIKED_SONGS) {
                    syncLikedSongs(task, destToken, session, request.getLikedSongIds(), request.getTrackMeta());
                } else if (task.getType() == SyncTaskType.PLAYLIST) {
                    syncPlaylist(task, sourceToken, destToken, destUserId, session);
                } else if (task.getType() == SyncTaskType.ALBUM) {
                    syncAlbum(task, sourceToken, destToken, session);
                }
                
                synchronized (session) {
                    task.setStatus(SyncStatus.COMPLETED);
                    session.setCompletedTasks(session.getCompletedTasks() + 1);
                }
            } catch (Exception e) {
                log.error("Task failed", e);
                synchronized (session) {
                    task.setStatus(SyncStatus.FAILED);
                    task.setErrorMessage(e.getMessage());
                    session.setFailedTasks(session.getFailedTasks() + 1);
                }
            }
            
            updateSessionState(session);
        }
        
        , taskExecutor)).collect(Collectors.toList());
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        synchronized (session) {
            session.setStatus(SyncStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
            updateSessionState(session);
        }
        updateSyncStateData(userSessionId, request);
    }

    private void updateSessionState(SyncSession session) {
        synchronized (session) {
            sessionRepository.save(session);
            broadcastProgress(session);
        }
    }

    private void syncLikedSongs(SyncTask task, String destToken, SyncSession session, List<String> trackIds, java.util.Map<String, SyncRequest.TrackMeta> trackMeta) throws Exception {
        List<String> idsToSync = new ArrayList<>(trackIds);
        Collections.reverse(idsToSync);
        
        synchronized (session) {
            task.setTotalTracks(idsToSync.size());
            updateSessionState(session);
        }
        
        for (int i = 0; i < idsToSync.size(); i += 50) {
            int end = Math.min(i + 50, idsToSync.size());
            List<String> batch = idsToSync.subList(i, end);
            
            // Update current track info for the first track in this batch
            String firstId = batch.get(0);
            if (trackMeta != null && trackMeta.containsKey(firstId)) {
                SyncRequest.TrackMeta meta = trackMeta.get(firstId);
                synchronized (session) {
                    task.setCurrentTrackName(meta.getName());
                    task.setCurrentArtistName(meta.getArtist());
                    updateSessionState(session);
                }
            }
            
            ObjectNode body = objectMapper.createObjectNode();
            ArrayNode idsNode = body.putArray("ids");
            batch.forEach(idsNode::add);
            
            String url = new StringBuilder(spotifyApiHost).append(pathTracks).toString();
            restTemplate.put(url, destToken, body.toString(), String.class);
            
            synchronized (session) {
                // Add each track in the batch to recentlySyncedTracks
                for (String id : batch) {
                    String tName = id;
                    String aName = "";
                    if (trackMeta != null && trackMeta.containsKey(id)) {
                        SyncRequest.TrackMeta meta = trackMeta.get(id);
                        tName = meta.getName();
                        aName = meta.getArtist();
                    }
                    SyncTask.SyncedTrackInfo info = new SyncTask.SyncedTrackInfo(tName, aName, "SYNCED");
                    task.getRecentlySyncedTracks().add(0, info); // prepend (newest first)
                    if (task.getRecentlySyncedTracks().size() > 30) {
                        task.getRecentlySyncedTracks().remove(task.getRecentlySyncedTracks().size() - 1);
                    }
                }
                task.setSyncedTracks(task.getSyncedTracks() + batch.size());
                updateSessionState(session);
            }
        }
        // Clear current track on finish
        synchronized (session) {
            task.setCurrentTrackName(null);
            task.setCurrentArtistName(null);
            updateSessionState(session);
        }
    }

    private void syncPlaylist(SyncTask task, String sourceToken, String destToken, String destUserId, SyncSession session) throws Exception {
        String plUrl = new StringBuilder(spotifyApiHost).append(pathBasePlaylists).append("/").append(task.getSourcePlaylistId()).toString();
        ResponseEntity<String> plResp = restTemplate.get(plUrl, sourceToken, String.class);
        JsonNode plNode = objectMapper.readTree(plResp.getBody());
        String name = plNode.get("name").asText();
        String desc = plNode.has("description") ? plNode.get("description").asText() : "";
        
        synchronized (session) {
            task.setSourcePlaylistName(name);
            if (plNode.has("images") && plNode.get("images").isArray() && plNode.get("images").size() > 0) {
                task.setSourcePlaylistImageUrl(plNode.get("images").get(0).get("url").asText());
            }
            updateSessionState(session);
        }
        
        ObjectNode body = objectMapper.createObjectNode();
        body.put("name", name);
        body.put("description", desc + " (Synced)");
        body.put("public", false);
        
        String createUrl = new StringBuilder(spotifyApiHost).append(pathUsers).append("/").append(destUserId).append("/playlists").toString();
        ResponseEntity<String> createResp = restTemplate.post(createUrl, destToken, body.toString(), String.class);
        String targetPlaylistId = objectMapper.readTree(createResp.getBody()).get("id").asText();
        
        synchronized (session) {
            task.setTargetPlaylistId(targetPlaylistId);
            updateSessionState(session);
        }
        
        // Collect tracks with name info
        List<String> trackUris = new ArrayList<>();
        List<String[]> trackInfo = new ArrayList<>(); // [uri, name, artist]
        String url = new StringBuilder(spotifyApiHost).append(pathBasePlaylists).append("/").append(task.getSourcePlaylistId()).append("/tracks?limit=").append(syncPlaylistTracksLimit).toString();
        
        while (url != null && !url.equals("null")) {
            ResponseEntity<String> response = restTemplate.get(url, sourceToken, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode items = root.get("items");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    if (item.has("track") && !item.get("track").isNull()) {
                        JsonNode t = item.get("track");
                        String uri = t.get("uri").asText();
                        String tName = t.has("name") ? t.get("name").asText() : uri;
                        String aName = (t.has("artists") && t.get("artists").isArray() && t.get("artists").size() > 0)
                            ? t.get("artists").get(0).get("name").asText() : "";
                        trackUris.add(uri);
                        trackInfo.add(new String[]{uri, tName, aName});
                    }
                }
            }
            url = root.has("next") && !root.get("next").isNull() ? root.get("next").asText() : null;
        }
        
        synchronized (session) {
            task.setTotalTracks(trackUris.size());
            updateSessionState(session);
        }
        
        for (int i = 0; i < trackUris.size(); i += 100) {
            int end = Math.min(i + 100, trackUris.size());
            List<String> batch = trackUris.subList(i, end);
            
            // Set current track from first in batch
            if (i < trackInfo.size()) {
                String[] first = trackInfo.get(i);
                synchronized (session) {
                    task.setCurrentTrackName(first[1]);
                    task.setCurrentArtistName(first[2]);
                    updateSessionState(session);
                }
            }
            
            ObjectNode addBody = objectMapper.createObjectNode();
            ArrayNode urisNode = addBody.putArray("uris");
            batch.forEach(urisNode::add);
            
            String addUrl = new StringBuilder(spotifyApiHost).append(pathBasePlaylists).append("/").append(targetPlaylistId).append("/tracks").toString();
            restTemplate.post(addUrl, destToken, addBody.toString(), String.class);
            
            synchronized (session) {
                for (int j = i; j < end && j < trackInfo.size(); j++) {
                    String[] info = trackInfo.get(j);
                    SyncTask.SyncedTrackInfo ti = new SyncTask.SyncedTrackInfo(info[1], info[2], "SYNCED");
                    task.getRecentlySyncedTracks().add(0, ti);
                    if (task.getRecentlySyncedTracks().size() > 30) {
                        task.getRecentlySyncedTracks().remove(task.getRecentlySyncedTracks().size() - 1);
                    }
                }
                task.setSyncedTracks(task.getSyncedTracks() + batch.size());
                updateSessionState(session);
            }
        }
    }

    private void syncAlbum(SyncTask task, String sourceToken, String destToken, SyncSession session) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode idsNode = body.putArray("ids");
        idsNode.add(task.getSourceAlbumId());
        
        String url = new StringBuilder(spotifyApiHost).append(pathAlbums).toString();
        restTemplate.put(url, destToken, body.toString(), String.class);
        
        synchronized (session) {
            task.setTotalTracks(1);
            task.setSyncedTracks(1);
            updateSessionState(session);
        }
    }
    
    private String getUserId(String token) throws Exception {
        String url = new StringBuilder(spotifyApiHost).append(pathMe).toString();
        ResponseEntity<String> response = restTemplate.get(url, token, String.class);
        return objectMapper.readTree(response.getBody()).get("id").asText();
    }

    private void broadcastProgress(SyncSession session) {
        SyncProgressDTO dto = new SyncProgressDTO();
        dto.setUserSessionId(session.getUserSessionId());
        dto.setStatus(session.getStatus());
        dto.setTotalTasks(session.getTotalTasks());
        dto.setCompletedTasks(session.getCompletedTasks());
        dto.setFailedTasks(session.getFailedTasks());
        
        double overall = 0;
        if (session.getTotalTasks() > 0) {
            overall = ((double) session.getCompletedTasks() / session.getTotalTasks()) * 100;
        }
        dto.setOverallProgressPercent(overall);
        
        List<SyncTaskDTO> taskDtos = session.getTasks().stream().map(t -> {
            SyncTaskDTO td = new SyncTaskDTO();
            td.setTaskId(t.getId());
            td.setType(t.getType());
            td.setStatus(t.getStatus());
            td.setTotalTracks(t.getTotalTracks());
            td.setSyncedTracks(t.getSyncedTracks());
            td.setSkippedTracks(t.getSkippedTracks());
            td.setErrorMessage(t.getErrorMessage());
            
            if (t.getType() == SyncTaskType.LIKED_SONGS) {
                td.setItemName("Liked Songs");
            } else if (t.getType() == SyncTaskType.PLAYLIST) {
                td.setItemName(t.getSourcePlaylistName());
                td.setItemImageUrl(t.getSourcePlaylistImageUrl());
            } else {
                td.setItemName("Album");
            }
            
            // Map recently synced tracks
            if (t.getRecentlySyncedTracks() != null) {
                List<SyncTaskDTO.SyncedTrackInfo> recentDtos = t.getRecentlySyncedTracks().stream().map(ti -> {
                    SyncTaskDTO.SyncedTrackInfo trackInfoDto = new SyncTaskDTO.SyncedTrackInfo();
                    trackInfoDto.setTrackName(ti.getTrackName());
                    trackInfoDto.setArtistName(ti.getArtistName());
                    trackInfoDto.setStatus(ti.getStatus());
                    return trackInfoDto;
                }).collect(Collectors.toList());
                td.setRecentlySyncedTracks(recentDtos);
            }
            td.setCurrentTrackName(t.getCurrentTrackName());
            td.setCurrentArtistName(t.getCurrentArtistName());
            
            double p = 0;
            if (t.getTotalTracks() > 0) {
                p = ((double) t.getSyncedTracks() / t.getTotalTracks()) * 100;
            }
            if (t.getStatus() == SyncStatus.COMPLETED) p = 100;
            td.setProgressPercent(p);
            
            return td;
        }).collect(Collectors.toList());
        
        dto.setTasks(taskDtos);
        
        progressService.sendProgress(session.getUserSessionId(), dto);
    }

    private void updateSyncStateData(String systemUserId, SyncRequest request) {
        SyncState state = syncStateRepository.findBySystemUserId(systemUserId).orElse(new SyncState());
        state.setSystemUserId(systemUserId);
        state.setFullySynced(true);
        
        LocalDateTime now = LocalDateTime.now();
        
        if (request.getLikedSongIds() != null) {
            request.getLikedSongIds().forEach(id -> {
                if (state.getSyncedTracks().stream().noneMatch(t -> t.getSpotifyId().equals(id))) {
                    state.getSyncedTracks().add(new SyncedItem(id, now));
                }
            });
        }
        if (request.getIgnoredSongIds() != null) {
            request.getIgnoredSongIds().forEach(id -> {
                if (state.getIgnoredTracks().stream().noneMatch(t -> t.getSpotifyId().equals(id))) {
                    state.getIgnoredTracks().add(new SyncedItem(id, now));
                }
            });
        }
        
        if (request.getPlaylistIds() != null) {
            request.getPlaylistIds().forEach(id -> {
                if (state.getSyncedPlaylists().stream().noneMatch(p -> p.getSpotifyId().equals(id))) {
                    state.getSyncedPlaylists().add(new SyncedItem(id, now));
                }
            });
        }
        if (request.getIgnoredPlaylistIds() != null) {
            request.getIgnoredPlaylistIds().forEach(id -> {
                if (state.getIgnoredPlaylists().stream().noneMatch(p -> p.getSpotifyId().equals(id))) {
                    state.getIgnoredPlaylists().add(new SyncedItem(id, now));
                }
            });
        }
        
        if (request.getAlbumIds() != null) {
            request.getAlbumIds().forEach(id -> {
                if (state.getSyncedAlbums().stream().noneMatch(a -> a.getSpotifyId().equals(id))) {
                    state.getSyncedAlbums().add(new SyncedItem(id, now));
                }
            });
        }
        if (request.getIgnoredAlbumIds() != null) {
            request.getIgnoredAlbumIds().forEach(id -> {
                if (state.getIgnoredAlbums().stream().noneMatch(a -> a.getSpotifyId().equals(id))) {
                    state.getIgnoredAlbums().add(new SyncedItem(id, now));
                }
            });
        }
        
        syncStateRepository.save(state);
    }
}
