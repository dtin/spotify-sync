package com.spotifysync.exception;

import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SpotifyApiException.class)
    public ResponseEntity<Map<String, String>> handleSpotifyApiException(SpotifyApiException ex) {
        if (ex.getCause() instanceof RestClientResponseException restEx) {
            log.error("Spotify API Exception: {} - Response body: {}", ex.getMessage(), restEx.getResponseBodyAsString(), ex);
        } else {
            log.error("Spotify API Exception: {}", ex.getMessage(), ex);
        }
        Sentry.captureException(ex);
        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        String exName = ex.getClass().getName();
        if (exName.contains("ClientAbortException") || exName.contains("AsyncRequestNotUsableException")) {
            log.warn("Client aborted connection: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        Sentry.captureException(ex);
        Map<String, String> body = new HashMap<>();
        body.put("error", "Internal server error: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
