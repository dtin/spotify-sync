package com.spotifysync.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncedItem {
    private String spotifyId;
    private LocalDateTime timestamp;
}
