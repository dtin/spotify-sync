package com.spotifysync.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class SpotifyApiClient {
    @Value("${spotify.api.max-retries:3}")
    private int maxRetries;

    private final RestTemplate restTemplate = new RestTemplate();

    public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity, Class<T> responseType) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return restTemplate.exchange(url, method, requestEntity, responseType);
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt == maxRetries) {
                    throw e; // Give up after max retries
                }
                
                String retryAfterHeader = e.getResponseHeaders() != null ? e.getResponseHeaders().getFirst("Retry-After") : null;
                int sleepSeconds = 2; // Default to 2 seconds if header is missing
                if (retryAfterHeader != null) {
                    try {
                        sleepSeconds = Integer.parseInt(retryAfterHeader);
                    } catch (NumberFormatException ignored) {}
                }
                
                log.warn("Spotify API Rate Limit Hit (429). Retrying after {} seconds... (Attempt {}/{})", sleepSeconds, attempt, maxRetries);
                
                try {
                    Thread.sleep((sleepSeconds + 1) * 1000L); // Add 1 second buffer
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted during rate limit sleep", ie);
                }
            }
        }
        throw new IllegalStateException("Max retries reached");
    }
    
    public <T> ResponseEntity<T> postForEntity(String url, HttpEntity<?> requestEntity, Class<T> responseType) {
        return exchange(url, HttpMethod.POST, requestEntity, responseType);
    }
    
    // --- Helper Methods to reduce boilerplate ---
    
    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    public <T> ResponseEntity<T> get(String url, String token, Class<T> responseType) {
        return exchange(url, HttpMethod.GET, new HttpEntity<>(createAuthHeaders(token)), responseType);
    }

    public <T> ResponseEntity<T> post(String url, String token, Object body, Class<T> responseType) {
        HttpHeaders headers = createAuthHeaders(token);
        headers.set("Content-Type", "application/json");
        return exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), responseType);
    }

    public <T> ResponseEntity<T> put(String url, String token, Object body, Class<T> responseType) {
        HttpHeaders headers = createAuthHeaders(token);
        headers.set("Content-Type", "application/json");
        return exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers), responseType);
    }
}
