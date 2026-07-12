package com.spotifysync.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotifysync.dto.response.AlbumResponse;
import com.spotifysync.dto.response.PlaylistResponse;
import com.spotifysync.dto.response.TrackResponse;
import com.spotifysync.enums.AccountType;
import com.spotifysync.exception.SpotifyApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpotifyApiService {

    private final SpotifyAuthService authService;
    private final SpotifyApiClient restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spotify.api.limit.playlists:50}")
    private int playlistLimit;

    @Value("${spotify.api.limit.tracks:50}")
    private int trackLimit;

    @Value("${spotify.api.limit.albums:50}")
    private int albumLimit;

    // --- PLAYLISTS ---

    public List<PlaylistResponse> getUserPlaylists(String userSessionId, AccountType accountType) {
        String accessToken = authService.getValidAccessToken(userSessionId, accountType);
        List<PlaylistResponse> playlists = new ArrayList<>();
        String url = "https://api.spotify.com/v1/me/playlists?limit=" + playlistLimit;

        try {
            while (url != null && !url.equals("null")) {
                ResponseEntity<String> response = restTemplate.get(url, accessToken, String.class);
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode items = root.get("items");
                
                if (items.isArray()) {
                    for (JsonNode item : items) {
                        if (item.isNull()) continue;
                        PlaylistResponse pr = new PlaylistResponse();
                        pr.setSpotifyPlaylistId(item.get("id").asText());
                        pr.setName(item.get("name").asText());
                        pr.setDescription(item.has("description") ? item.get("description").asText() : "");
                        pr.setOwnerName(item.get("owner").get("display_name").asText());
                        pr.setTotalTracks(item.get("tracks").get("total").asInt());
                        pr.setPublic(item.has("public") && !item.get("public").isNull() ? item.get("public").asBoolean() : false);
                        
                        if (item.has("images") && item.get("images").isArray() && item.get("images").size() > 0) {
                            pr.setImageUrl(item.get("images").get(0).get("url").asText());
                        }
                        
                        pr.setSynced(false); // will be checked against target later if needed
                        playlists.add(pr);
                    }
                }
                url = root.has("next") && !root.get("next").isNull() ? root.get("next").asText() : null;
            }
        } catch (Exception e) {
            throw new SpotifyApiException("Failed to fetch playlists", e);
        }
        return playlists;
    }

    // --- LIKED SONGS ---

    public List<TrackResponse> getSavedTracks(String userSessionId, AccountType accountType) {
        String accessToken = authService.getValidAccessToken(userSessionId, accountType);
        List<TrackResponse> tracks = new ArrayList<>();
        String url = "https://api.spotify.com/v1/me/tracks?limit=" + trackLimit;

        try {
            while (url != null && !url.equals("null")) {
                ResponseEntity<String> response = restTemplate.get(url, accessToken, String.class);
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode items = root.get("items");
                
                if (items.isArray()) {
                    for (JsonNode item : items) {
                        JsonNode trackNode = item.get("track");
                        TrackResponse tr = new TrackResponse();
                        tr.setAddedAt(item.get("added_at").asText());
                        tr.setTrackId(trackNode.get("id").asText());
                        tr.setName(trackNode.get("name").asText());
                        
                        if (trackNode.has("artists") && trackNode.get("artists").isArray() && trackNode.get("artists").size() > 0) {
                            tr.setArtistName(trackNode.get("artists").get(0).get("name").asText());
                        }
                        
                        JsonNode albumNode = trackNode.get("album");
                        if (albumNode != null) {
                            tr.setAlbumName(albumNode.get("name").asText());
                            if (albumNode.has("images") && albumNode.get("images").isArray() && albumNode.get("images").size() > 0) {
                                tr.setAlbumImageUrl(albumNode.get("images").get(0).get("url").asText());
                            }
                        }
                        
                        tr.setSynced(false);
                        tracks.add(tr);
                    }
                }
                url = root.has("next") && !root.get("next").isNull() ? root.get("next").asText() : null;
            }
        } catch (Exception e) {
            throw new SpotifyApiException("Failed to fetch liked songs", e);
        }
        return tracks;
    }

    // --- SAVED ALBUMS ---

    public List<AlbumResponse> getSavedAlbums(String userSessionId, AccountType accountType) {
        String accessToken = authService.getValidAccessToken(userSessionId, accountType);
        List<AlbumResponse> albums = new ArrayList<>();
        String url = "https://api.spotify.com/v1/me/albums?limit=" + albumLimit;

        try {
            while (url != null && !url.equals("null")) {
                ResponseEntity<String> response = restTemplate.get(url, accessToken, String.class);
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode items = root.get("items");
                
                if (items.isArray()) {
                    for (JsonNode item : items) {
                        JsonNode albumNode = item.get("album");
                        AlbumResponse ar = new AlbumResponse();
                        ar.setSpotifyAlbumId(albumNode.get("id").asText());
                        ar.setName(albumNode.get("name").asText());
                        ar.setReleaseDate(albumNode.get("release_date").asText());
                        ar.setTotalTracks(albumNode.get("total_tracks").asInt());
                        
                        if (albumNode.has("artists") && albumNode.get("artists").isArray() && albumNode.get("artists").size() > 0) {
                            ar.setArtistName(albumNode.get("artists").get(0).get("name").asText());
                        }
                        
                        if (albumNode.has("images") && albumNode.get("images").isArray() && albumNode.get("images").size() > 0) {
                            ar.setImageUrl(albumNode.get("images").get(0).get("url").asText());
                        }
                        
                        ar.setSynced(false);
                        albums.add(ar);
                    }
                }
                url = root.has("next") && !root.get("next").isNull() ? root.get("next").asText() : null;
            }
        } catch (Exception e) {
            throw new SpotifyApiException("Failed to fetch saved albums", e);
        }
        return albums;
    }
    
}
