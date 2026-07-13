package com.spotifysync.entity;

import com.spotifysync.enums.AccountType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

import java.time.LocalDateTime;

@Document(collection = "spotify_accounts")
@Data
public class SpotifyAccount {
    @Id
    private String id;
    
    private AccountType accountType; // SOURCE, DESTINATION
    
    private String spotifyUserId;
    private String displayName;
    private String email;
    private String profileImageUrl;
    
    private String accessToken;
    
    private String refreshToken;
    
    private LocalDateTime expiresAt;
    private String systemUserId; // Link to SystemUser.id
}
