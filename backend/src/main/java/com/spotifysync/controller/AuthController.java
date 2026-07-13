package com.spotifysync.controller;

import com.spotifysync.dto.response.AuthStatusResponse;
import com.spotifysync.enums.AccountType;
import com.spotifysync.service.SpotifyAuthService;
import com.spotifysync.security.JwtUtil;
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
    private final JwtUtil jwtUtil;
    
    @Value("${spotify.frontend-url}")
    private String frontendUrl;

    @GetMapping("/login/{accountType}")
    public RedirectView login(@PathVariable AccountType accountType, 
                              @RequestParam("token") String token) {
        String systemUserId = jwtUtil.extractUserId(token);
        String authUrl = authService.generateAuthUrl(accountType, systemUserId);
        return new RedirectView(authUrl);
    }

    @GetMapping("/callback")
    public RedirectView callback(@RequestParam String code, @RequestParam String state) {
        try {
            authService.handleCallback(code, state);
            // Redirect back to frontend, no need to return token as frontend already has it
            return new RedirectView(frontendUrl);
        } catch (com.spotifysync.exception.SpotifyApiException e) {
            if ("same_account".equals(e.getMessage())) {
                return new RedirectView(frontendUrl + "?error=same_account");
            }
            return new RedirectView(frontendUrl + "?error=auth_failed");
        }
    }

    @GetMapping("/status")
    public ResponseEntity<AuthStatusResponse> getStatus(java.security.Principal principal) {
        return ResponseEntity.ok(authService.getAuthStatus(principal.getName()));
    }

    @PostMapping("/logout/{accountType}")
    public ResponseEntity<Void> logout(@PathVariable AccountType accountType, 
                                       java.security.Principal principal) {
        authService.logout(principal.getName(), accountType);
        return ResponseEntity.ok().build();
    }
    

}
