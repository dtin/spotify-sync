import { useState, useEffect } from 'react';
import { API_BASE_URL } from '../lib/api';

export function useSystemAuth() {
    const [token, setToken] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const storedToken = localStorage.getItem('system_token');
        if (storedToken) {
            setToken(storedToken);
        }
        setLoading(false);
    }, []);

    const login = (newToken: string) => {
        localStorage.setItem('system_token', newToken);
        setToken(newToken);
    };

    const logout = () => {
        localStorage.removeItem('system_token');
        setToken(null);
    };

    return { token, loading, login, logout, isAuthenticated: !!token };
}
