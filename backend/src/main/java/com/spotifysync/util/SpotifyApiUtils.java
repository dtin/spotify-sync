package com.spotifysync.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotifysync.enums.AccountType;
import com.spotifysync.exception.SpotifyApiException;
import com.spotifysync.service.SpotifyApiClient;
import com.spotifysync.service.SpotifyAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class SpotifyApiUtils {

    private final SpotifyAuthService authService;
    private final SpotifyApiClient restTemplate;
    private final ObjectMapper objectMapper;

    public <T> List<T> fetchAllItems(String userSessionId, AccountType accountType, String initialUrl, String errorMessage, Function<JsonNode, T> mapper) {
        String accessToken = authService.getValidAccessToken(userSessionId, accountType);
        List<T> results = new ArrayList<>();
        String url = initialUrl;

        try {
            while (url != null && !url.equals("null")) {
                ResponseEntity<String> response = restTemplate.get(url, accessToken, String.class);
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode items = root.get("items");
                
                if (items != null && items.isArray()) {
                    for (JsonNode item : items) {
                        if (item == null || item.isNull()) continue;
                        T mappedObj = mapper.apply(item);
                        if (mappedObj != null) {
                            results.add(mappedObj);
                        }
                    }
                }
                url = root.has("next") && !root.get("next").isNull() ? root.get("next").asText() : null;
            }
        } catch (Exception e) {
            throw new SpotifyApiException(errorMessage, e);
        }
        return results;
    }
}
