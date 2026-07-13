package com.spotifysync.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Document(collection = "system_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemUser {
    @Id
    private String id;
    
    private String username;
    private String passwordHash; // BCrypt hash
    private LocalDateTime createdAt;
}
