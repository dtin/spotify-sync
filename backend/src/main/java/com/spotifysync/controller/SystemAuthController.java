package com.spotifysync.controller;

import com.spotifysync.dto.request.AuthRequest;
import com.spotifysync.dto.response.AuthResponse;
import com.spotifysync.service.SystemAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system-auth")
@RequiredArgsConstructor
public class SystemAuthController {

    private final SystemAuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
