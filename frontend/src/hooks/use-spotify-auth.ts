import { useState, useEffect } from 'react';
import { fetchWithAuth, API_BASE_URL } from '../lib/api';
import { useRouter, useSearchParams } from 'next/navigation';

export interface AccountInfo {
    displayName: string;
    email: string;
    profileImageUrl: string;
    connected: boolean;
}

export interface AuthStatus {
    source: AccountInfo;
    destination: AccountInfo;
}

export function useSpotifyAuth() {
    const [status, setStatus] = useState<AuthStatus | null>(null);
    const [loading, setLoading] = useState(true);
    const router = useRouter();
    const searchParams = useSearchParams();

    useEffect(() => {
        // Check for token in URL after callback
        const tokenFromUrl = searchParams.get('token');
        if (tokenFromUrl) {
            localStorage.setItem('spotify_session_token', tokenFromUrl);
            router.replace('/'); // Clean URL
        }

        checkStatus();
    }, [searchParams, router]);

    const checkStatus = async () => {
        try {
            const data = await fetchWithAuth('/api/auth/status');
            setStatus(data);
        } catch (error) {
            console.error("Failed to fetch auth status", error);
            setStatus({
                source: { connected: false } as AccountInfo,
                destination: { connected: false } as AccountInfo
            });
        } finally {
            setLoading(false);
        }
    };

    const login = (accountType: 'SOURCE' | 'DESTINATION') => {
        const token = localStorage.getItem('spotify_session_token') || 'new';
        window.location.href = `${API_BASE_URL}/api/auth/login/${accountType}?userSessionId=${token}`;
    };

    const logout = async (accountType: 'SOURCE' | 'DESTINATION') => {
        try {
            await fetchWithAuth(`/api/auth/logout/${accountType}`, { method: 'POST' });
            await checkStatus();
        } catch (error) {
            console.error("Failed to logout", error);
        }
    };

    return { status, loading, login, logout, refreshStatus: checkStatus };
}
