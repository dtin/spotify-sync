package com.spotifysync.entity;

import com.spotifysync.enums.AccountType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class SpotifyAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    private AccountType accountType; // SOURCE, DESTINATION
    
    private String spotifyUserId;
    private String displayName;
    private String email;
    private String profileImageUrl;
    
    @Column(columnDefinition = "TEXT")
    private String accessToken;
    
    @Column(columnDefinition = "TEXT")
    private String refreshToken;
    
    private LocalDateTime expiresAt;
    
    private String userSessionId; // Bearer token identifier for the frontend
}
