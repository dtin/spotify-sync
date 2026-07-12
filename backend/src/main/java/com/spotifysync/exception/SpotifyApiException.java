package com.spotifysync.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class SpotifyApiException extends RuntimeException {
    public SpotifyApiException(String message) {
        super(message);
    }

    public SpotifyApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
