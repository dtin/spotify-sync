package com.spotifysync.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotifysync.dto.response.AlbumResponse;
import com.spotifysync.dto.response.PlaylistResponse;
import com.spotifysync.dto.response.TrackResponse;
import com.spotifysync.enums.AccountType;
import com.spotifysync.exception.SpotifyApiException;
import com.spotifysync.util.SpotifyApiUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpotifyApiService {

    private final SpotifyAuthService authService;
    private final SpotifyApiClient restTemplate;
    private final ObjectMapper objectMapper;
    private final SpotifyApiUtils spotifyApiUtils;

    @Value("${spotify.api.host:https://api.spotify.com}")
    private String spotifyApiHost;

    @Value("${spotify.api.limit.playlists:50}")
    private int playlistLimit;

    @Value("${spotify.api.limit.tracks:50}")
    private int trackLimit;

    @Value("${spotify.api.limit.albums:50}")
    private int albumLimit;

    // --- PLAYLISTS ---

    public List<PlaylistResponse> getUserPlaylists(String userSessionId, AccountType accountType) {
        String url = new StringBuilder(spotifyApiHost).append("/v1/me/playlists?limit=").append(playlistLimit).toString();
        return spotifyApiUtils.fetchAllItems(userSessionId, accountType, url, "Failed to fetch playlists", item -> {
            PlaylistResponse pr = new PlaylistResponse();
            pr.setSpotifyPlaylistId(item.get("id").asText());
            pr.setName(item.get("name").asText());
            pr.setDescription(item.has("description") ? item.get("description").asText() : "");
            pr.setOwnerName(item.has("owner") && !item.get("owner").isNull() && item.get("owner").has("display_name") && !item.get("owner").get("display_name").isNull() ? item.get("owner").get("display_name").asText() : "Unknown");
            pr.setTotalTracks(item.has("tracks") && !item.get("tracks").isNull() && item.get("tracks").has("total") && !item.get("tracks").get("total").isNull() ? item.get("tracks").get("total").asInt() : 0);
            pr.setPublic(item.has("public") && !item.get("public").isNull() ? item.get("public").asBoolean() : false);
            
            if (item.has("images") && item.get("images").isArray() && item.get("images").size() > 0) {
                pr.setImageUrl(item.get("images").get(0).get("url").asText());
            }
            pr.setSynced(false);
            return pr;
        });
    }

    // --- LIKED SONGS ---

    public List<TrackResponse> getSavedTracks(String userSessionId, AccountType accountType) {
        String url = new StringBuilder(spotifyApiHost).append("/v1/me/tracks?limit=").append(trackLimit).toString();
        return spotifyApiUtils.fetchAllItems(userSessionId, accountType, url, "Failed to fetch liked songs", item -> {
            JsonNode trackNode = item.get("track");
            if (trackNode == null || trackNode.isNull()) return null;
            
            TrackResponse tr = new TrackResponse();
            tr.setAddedAt(item.get("added_at").asText());
            tr.setTrackId(trackNode.get("id").asText());
            tr.setName(trackNode.get("name").asText());
            
            if (trackNode.has("artists") && trackNode.get("artists").isArray() && trackNode.get("artists").size() > 0) {
                tr.setArtistName(trackNode.get("artists").get(0).get("name").asText());
            }
            
            JsonNode albumNode = trackNode.get("album");
            if (albumNode != null && !albumNode.isNull()) {
                tr.setAlbumName(albumNode.get("name").asText());
                if (albumNode.has("images") && albumNode.get("images").isArray() && albumNode.get("images").size() > 0) {
                    tr.setAlbumImageUrl(albumNode.get("images").get(0).get("url").asText());
                }
            }
            tr.setSynced(false);
            return tr;
        });
    }

    // --- SAVED ALBUMS ---

    public List<AlbumResponse> getSavedAlbums(String userSessionId, AccountType accountType) {
        String url = new StringBuilder(spotifyApiHost).append("/v1/me/albums?limit=").append(albumLimit).toString();
        return spotifyApiUtils.fetchAllItems(userSessionId, accountType, url, "Failed to fetch saved albums", item -> {
            JsonNode albumNode = item.get("album");
            if (albumNode == null || albumNode.isNull()) return null;
            
            AlbumResponse ar = new AlbumResponse();
            ar.setSpotifyAlbumId(albumNode.get("id").asText());
            ar.setName(albumNode.get("name").asText());
            ar.setReleaseDate(albumNode.has("release_date") ? albumNode.get("release_date").asText() : "");
            ar.setTotalTracks(albumNode.has("total_tracks") ? albumNode.get("total_tracks").asInt() : 0);
            
            if (albumNode.has("artists") && albumNode.get("artists").isArray() && albumNode.get("artists").size() > 0) {
                ar.setArtistName(albumNode.get("artists").get(0).get("name").asText());
            }
            
            if (albumNode.has("images") && albumNode.get("images").isArray() && albumNode.get("images").size() > 0) {
                ar.setImageUrl(albumNode.get("images").get(0).get("url").asText());
            }
            ar.setSynced(false);
            return ar;
        });
    }
}
