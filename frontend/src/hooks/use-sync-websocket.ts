import { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { API_BASE_URL } from '../lib/api';

export interface SyncedTrackInfo {
    trackName: string;
    artistName: string;
    status: 'SYNCED' | 'SKIPPED';
}

export interface SyncTaskDTO {
    taskId: number;
    type: 'PLAYLIST' | 'LIKED_SONGS' | 'ALBUM';
    itemName: string;
    itemImageUrl: string;
    status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'SKIPPED' | 'CANCELLED';
    totalTracks: number;
    syncedTracks: number;
    skippedTracks: number;
    progressPercent: number;
    errorMessage: string;
    // Live per-track progress
    currentTrackName: string | null;
    currentArtistName: string | null;
    recentlySyncedTracks: SyncedTrackInfo[];
}

export interface SyncProgressDTO {
    userSessionId: string;
    status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
    totalTasks: number;
    completedTasks: number;
    failedTasks: number;
    overallProgressPercent: number;
    tasks: SyncTaskDTO[];
}

export function useSyncWebSocket() {
    const [progress, setProgress] = useState<SyncProgressDTO | null>(null);
    const [connected, setConnected] = useState(false);
    const clientRef = useRef<Client | null>(null);

    useEffect(() => {
        const token = localStorage.getItem('system_token');
        if (!token) return;

        let systemUserId = '';
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            systemUserId = payload.sub;
        } catch (e) {
            console.error('Failed to parse system_token', e);
            return;
        }

        const socketUrl = `${API_BASE_URL}/ws-sync`;
        
        const client = new Client({
            webSocketFactory: () => new SockJS(socketUrl),
            onConnect: () => {
                setConnected(true);
                client.subscribe(`/topic/sync-progress/${systemUserId}`, (message) => {
                    const data = JSON.parse(message.body);
                    setProgress(data);
                });
            },
            onDisconnect: () => {
                setConnected(false);
            },
            onStompError: (frame) => {
                console.error('STOMP Error:', frame);
            }
        });

        client.activate();
        clientRef.current = client;

        return () => {
            if (clientRef.current) {
                clientRef.current.deactivate();
            }
        };
    }, []);

    return { progress, connected, clearProgress: () => setProgress(null) };
}
