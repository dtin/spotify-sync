package com.spotifysync.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotifysync.dto.response.AccountInfo;
import com.spotifysync.dto.response.AuthStatusResponse;
import com.spotifysync.entity.SpotifyAccount;
import com.spotifysync.enums.AccountType;
import com.spotifysync.exception.SpotifyApiException;
import com.spotifysync.repository.SpotifyAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpotifyAuthService {
    
    @Value("${spotify.client-id}")
    private String clientId;
    
    @Value("${spotify.client-secret}")
    private String clientSecret;
    
    @Value("${spotify.redirect-uri}")
    private String redirectUri;
    
    @Value("${spotify.scopes}")
    private String scopes;
    
    private final SpotifyAccountRepository accountRepository;
    
    @Value("${spotify.api.host:https://api.spotify.com}")
    private String spotifyApiHost;
    
    @Value("${spotify.api.accounts-host:https://accounts.spotify.com}")
    private String spotifyAccountsHost;
    
    @Value("${spotify.api.paths.authorize:/authorize}")
    private String pathAuthorize;
    
    @Value("${spotify.api.paths.token:/api/token}")
    private String pathToken;
    
    @Value("${spotify.api.paths.me:/v1/me}")
    private String pathMe;

    private final ObjectMapper objectMapper;
    private final SpotifyApiClient restTemplate;
    
    public String generateAuthUrl(AccountType accountType, String userSessionId) {
        String state = accountType.name() + "_" + userSessionId;
        return new StringBuilder(spotifyAccountsHost)
                .append(pathAuthorize)
                .append("?response_type=code")
                .append("&client_id=").append(clientId)
                .append("&scope=").append(scopes)
                .append("&redirect_uri=").append(redirectUri)
                .append("&state=").append(state)
                .append("&show_dialog=true")
                .toString();
    }
    
    public String handleCallback(String code, String state) {
        String[] parts = state.split("_", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid state");
        }
        
        AccountType accountType = AccountType.valueOf(parts[0]);
        String userSessionId = parts[1];
        if (userSessionId.equals("new")) {
            userSessionId = UUID.randomUUID().toString();
        }
        
        // Exchange code for tokens
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String auth = clientId + ":" + clientSecret;
        headers.setBasicAuth(Base64.getEncoder().encodeToString(auth.getBytes()));
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        
        try {
            String url = new StringBuilder(spotifyAccountsHost).append(pathToken).toString();
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            
            String accessToken = jsonNode.get("access_token").asText();
            String refreshToken = jsonNode.get("refresh_token").asText();
            int expiresIn = jsonNode.get("expires_in").asInt();
            
            // Get user profile
            JsonNode profile = getUserProfile(accessToken);
            String spotifyUserId = profile.get("id").asText();
            String displayName = profile.has("display_name") ? profile.get("display_name").asText() : spotifyUserId;
            String email = profile.has("email") ? profile.get("email").asText() : "";
            
            String profileImageUrl = "";
            if (profile.has("images") && profile.get("images").isArray() && profile.get("images").size() > 0) {
                profileImageUrl = profile.get("images").get(0).get("url").asText();
            }
            
            // Save to DB
            Optional<SpotifyAccount> existingOpt = accountRepository.findByUserSessionIdAndAccountType(userSessionId, accountType);
            SpotifyAccount account = existingOpt.orElse(new SpotifyAccount());
            account.setAccountType(accountType);
            account.setUserSessionId(userSessionId);
            account.setSpotifyUserId(spotifyUserId);
            account.setDisplayName(displayName);
            account.setEmail(email);
            account.setProfileImageUrl(profileImageUrl);
            account.setAccessToken(accessToken);
            account.setRefreshToken(refreshToken);
            account.setExpiresAt(LocalDateTime.now().plusSeconds(expiresIn - 60)); // minus 60s for safety
            
            accountRepository.save(account);
            
            return userSessionId;
        } catch (Exception e) {
            throw new SpotifyApiException("Failed to authenticate with Spotify: " + e.getMessage(), e);
        }
    }
    
    private JsonNode getUserProfile(String accessToken) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> request = new HttpEntity<>(headers);
        String url = new StringBuilder(spotifyApiHost).append(pathMe).toString();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        return objectMapper.readTree(response.getBody());
    }
    
    public String getValidAccessToken(String userSessionId, AccountType accountType) {
        SpotifyAccount account = accountRepository.findByUserSessionIdAndAccountType(userSessionId, accountType)
                .orElseThrow(() -> new SpotifyApiException("Account not connected: " + accountType));
                
        if (LocalDateTime.now().isAfter(account.getExpiresAt())) {
            return refreshAccessToken(account);
        }
        return account.getAccessToken();
    }
    
    private String refreshAccessToken(SpotifyAccount account) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String auth = clientId + ":" + clientSecret;
        headers.setBasicAuth(Base64.getEncoder().encodeToString(auth.getBytes()));
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", account.getRefreshToken());
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        
        try {
            String url = new StringBuilder(spotifyAccountsHost).append(pathToken).toString();
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            
            String accessToken = jsonNode.get("access_token").asText();
            int expiresIn = jsonNode.get("expires_in").asInt();
            
            if (jsonNode.has("refresh_token")) {
                account.setRefreshToken(jsonNode.get("refresh_token").asText());
            }
            account.setAccessToken(accessToken);
            account.setExpiresAt(LocalDateTime.now().plusSeconds(expiresIn - 60));
            accountRepository.save(account);
            
            return accessToken;
        } catch (Exception e) {
            throw new SpotifyApiException("Failed to refresh token", e);
        }
    }
    
    public AuthStatusResponse getAuthStatus(String userSessionId) {
        AuthStatusResponse response = new AuthStatusResponse();
        
        Optional<SpotifyAccount> sourceOpt = accountRepository.findByUserSessionIdAndAccountType(userSessionId, AccountType.SOURCE);
        if (sourceOpt.isPresent()) {
            SpotifyAccount s = sourceOpt.get();
            response.setSource(new AccountInfo(s.getSpotifyUserId(), s.getDisplayName(), s.getEmail(), s.getProfileImageUrl(), true));
        } else {
            response.setSource(new AccountInfo(null, null, null, null, false));
        }
        
        Optional<SpotifyAccount> destOpt = accountRepository.findByUserSessionIdAndAccountType(userSessionId, AccountType.DESTINATION);
        if (destOpt.isPresent()) {
            SpotifyAccount d = destOpt.get();
            response.setDestination(new AccountInfo(d.getSpotifyUserId(), d.getDisplayName(), d.getEmail(), d.getProfileImageUrl(), true));
        } else {
            response.setDestination(new AccountInfo(null, null, null, null, false));
        }
        
        return response;
    }
    
    @Transactional
    public void logout(String userSessionId, AccountType accountType) {
        accountRepository.deleteByUserSessionIdAndAccountType(userSessionId, accountType);
    }
}
