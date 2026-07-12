package com.spotifysync.controller;

import com.spotifysync.dto.response.AuthStatusResponse;
import com.spotifysync.enums.AccountType;
import com.spotifysync.service.SpotifyAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SpotifyAuthService authService;
    
    @Value("${spotify.frontend-url}")
    private String frontendUrl;

    @GetMapping("/login/{accountType}")
    public RedirectView login(@PathVariable AccountType accountType, 
                              @RequestParam(required = false, defaultValue = "new") String userSessionId) {
        String authUrl = authService.generateAuthUrl(accountType, userSessionId);
        return new RedirectView(authUrl);
    }

    @GetMapping("/callback")
    public RedirectView callback(@RequestParam String code, @RequestParam String state) {
        String userSessionId = authService.handleCallback(code, state);
        // Redirect back to frontend with the session token
        return new RedirectView(frontendUrl + "?token=" + userSessionId);
    }

    @GetMapping("/status")
    public ResponseEntity<AuthStatusResponse> getStatus(@RequestHeader("Authorization") String authHeader) {
        String userSessionId = extractToken(authHeader);
        return ResponseEntity.ok(authService.getAuthStatus(userSessionId));
    }

    @PostMapping("/logout/{accountType}")
    public ResponseEntity<Void> logout(@PathVariable AccountType accountType, 
                                       @RequestHeader("Authorization") String authHeader) {
        String userSessionId = extractToken(authHeader);
        authService.logout(userSessionId, accountType);
        return ResponseEntity.ok().build();
    }
    
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Missing or invalid Authorization header");
    }
}
