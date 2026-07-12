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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private final SpotifyAuthService authService;
    private final SpotifyApiService apiService;
    private final SyncProgressService progressService;
    private final SyncSessionRepository sessionRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Async("syncTaskExecutor")
    public void startSync(String userSessionId, SyncRequest request) {
        SyncSession session = new SyncSession();
        session.setUserSessionId(userSessionId);
        session.setStatus(SyncStatus.IN_PROGRESS);
        session.setStartedAt(LocalDateTime.now());
        
        List<SyncTask> tasks = new ArrayList<>();
        
        if (request.getLikedSongIds() != null && !request.getLikedSongIds().isEmpty()) {
            SyncTask t = new SyncTask();
            t.setSyncSession(session);
            t.setType(SyncTaskType.LIKED_SONGS);
            t.setStatus(SyncStatus.PENDING);
            tasks.add(t);
        }
        
        if (request.getPlaylistIds() != null) {
            for (String id : request.getPlaylistIds()) {
                SyncTask t = new SyncTask();
                t.setSyncSession(session);
                t.setType(SyncTaskType.PLAYLIST);
                t.setSourcePlaylistId(id);
                t.setStatus(SyncStatus.PENDING);
                tasks.add(t);
            }
        }
        
        if (request.getAlbumIds() != null) {
            for (String id : request.getAlbumIds()) {
                SyncTask t = new SyncTask();
                t.setSyncSession(session);
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
            sessionRepository.save(session);
            broadcastProgress(session);
            return;
        }

        for (SyncTask task : tasks) {
            task.setStatus(SyncStatus.IN_PROGRESS);
            sessionRepository.save(session);
            broadcastProgress(session);
            
            try {
                if (task.getType() == SyncTaskType.LIKED_SONGS) {
                    syncLikedSongs(task, destToken, session, request.getLikedSongIds());
                } else if (task.getType() == SyncTaskType.PLAYLIST) {
                    syncPlaylist(task, sourceToken, destToken, destUserId, session);
                } else if (task.getType() == SyncTaskType.ALBUM) {
                    syncAlbum(task, sourceToken, destToken, session);
                }
                task.setStatus(SyncStatus.COMPLETED);
                session.setCompletedTasks(session.getCompletedTasks() + 1);
            } catch (Exception e) {
                log.error("Task failed", e);
                task.setStatus(SyncStatus.FAILED);
                task.setErrorMessage(e.getMessage());
                session.setFailedTasks(session.getFailedTasks() + 1);
            }
            
            sessionRepository.save(session);
            broadcastProgress(session);
        }
        
        session.setStatus(SyncStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        sessionRepository.save(session);
        broadcastProgress(session);
    }

    private void syncLikedSongs(SyncTask task, String destToken, SyncSession session, List<String> trackIds) throws Exception {
        // Use the track IDs provided by the frontend (which are in newest-to-oldest order)
        // Reverse them so we sync oldest first
        List<String> idsToSync = new ArrayList<>(trackIds);
        Collections.reverse(idsToSync);
        
        task.setTotalTracks(idsToSync.size());
        
        // Save to destination in batches of 50
        for (int i = 0; i < idsToSync.size(); i += 50) {
            int end = Math.min(i + 50, idsToSync.size());
            List<String> batch = idsToSync.subList(i, end);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(destToken);
            headers.set("Content-Type", "application/json");
            
            ObjectNode body = objectMapper.createObjectNode();
            ArrayNode idsNode = body.putArray("ids");
            batch.forEach(idsNode::add);
            
            HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);
            restTemplate.exchange("https://api.spotify.com/v1/me/tracks", HttpMethod.PUT, request, String.class);
            
            task.setSyncedTracks(task.getSyncedTracks() + batch.size());
            sessionRepository.save(session);
            broadcastProgress(session);
        }
    }

    private void syncPlaylist(SyncTask task, String sourceToken, String destToken, String destUserId, SyncSession session) throws Exception {
        // 1. Get Playlist details from source
        ResponseEntity<String> plResp = restTemplate.exchange("https://api.spotify.com/v1/playlists/" + task.getSourcePlaylistId(), HttpMethod.GET, createAuthHeader(sourceToken), String.class);
        JsonNode plNode = objectMapper.readTree(plResp.getBody());
        String name = plNode.get("name").asText();
        String desc = plNode.has("description") ? plNode.get("description").asText() : "";
        task.setSourcePlaylistName(name);
        if (plNode.has("images") && plNode.get("images").isArray() && plNode.get("images").size() > 0) {
            task.setSourcePlaylistImageUrl(plNode.get("images").get(0).get("url").asText());
        }
        
        // 2. Create Playlist on destination
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(destToken);
        headers.set("Content-Type", "application/json");
        ObjectNode body = objectMapper.createObjectNode();
        body.put("name", name);
        body.put("description", desc + " (Synced)");
        body.put("public", false);
        
        HttpEntity<String> createReq = new HttpEntity<>(body.toString(), headers);
        ResponseEntity<String> createResp = restTemplate.exchange("https://api.spotify.com/v1/users/" + destUserId + "/playlists", HttpMethod.POST, createReq, String.class);
        String targetPlaylistId = objectMapper.readTree(createResp.getBody()).get("id").asText();
        task.setTargetPlaylistId(targetPlaylistId);
        
        // 3. Fetch all tracks from source playlist and add to destination
        List<String> trackUris = new ArrayList<>();
        String url = "https://api.spotify.com/v1/playlists/" + task.getSourcePlaylistId() + "/tracks?limit=100";
        
        while (url != null && !url.equals("null")) {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, createAuthHeader(sourceToken), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode items = root.get("items");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    if (item.has("track") && !item.get("track").isNull()) {
                        trackUris.add(item.get("track").get("uri").asText());
                    }
                }
            }
            url = root.has("next") && !root.get("next").isNull() ? root.get("next").asText() : null;
        }
        
        task.setTotalTracks(trackUris.size());
        
        // 4. Add tracks in batches of 100
        for (int i = 0; i < trackUris.size(); i += 100) {
            int end = Math.min(i + 100, trackUris.size());
            List<String> batch = trackUris.subList(i, end);
            
            ObjectNode addBody = objectMapper.createObjectNode();
            ArrayNode urisNode = addBody.putArray("uris");
            batch.forEach(urisNode::add);
            
            HttpEntity<String> addReq = new HttpEntity<>(addBody.toString(), headers);
            restTemplate.exchange("https://api.spotify.com/v1/playlists/" + targetPlaylistId + "/tracks", HttpMethod.POST, addReq, String.class);
            
            task.setSyncedTracks(task.getSyncedTracks() + batch.size());
            sessionRepository.save(session);
            broadcastProgress(session);
        }
    }

    private void syncAlbum(SyncTask task, String sourceToken, String destToken, SyncSession session) throws Exception {
        // Just save the album ID to destination
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(destToken);
        headers.set("Content-Type", "application/json");
        
        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode idsNode = body.putArray("ids");
        idsNode.add(task.getSourceAlbumId());
        
        HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);
        restTemplate.exchange("https://api.spotify.com/v1/me/albums", HttpMethod.PUT, request, String.class);
        
        task.setTotalTracks(1);
        task.setSyncedTracks(1);
    }
    
    private String getUserId(String token) throws Exception {
        ResponseEntity<String> response = restTemplate.exchange("https://api.spotify.com/v1/me", HttpMethod.GET, createAuthHeader(token), String.class);
        return objectMapper.readTree(response.getBody()).get("id").asText();
    }
    
    private HttpEntity<Void> createAuthHeader(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
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
}
