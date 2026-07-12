package com.spotifysync.service;

import com.spotifysync.dto.response.SyncProgressDTO;
import com.spotifysync.dto.response.SyncTaskDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SyncProgressService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendProgress(String userSessionId, SyncProgressDTO progress) {
        String destination = "/topic/sync-progress/" + userSessionId;
        messagingTemplate.convertAndSend(destination, progress);
    }
}
