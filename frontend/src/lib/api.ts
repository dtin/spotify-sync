export const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export async function fetchWithAuth(url: string, options: RequestInit = {}) {
    const token = typeof window !== 'undefined' ? localStorage.getItem('spotify_session_token') : null;
    
    const headers = new Headers(options.headers);
    if (token) {
        headers.set('Authorization', `Bearer ${token}`);
    }
    
    const response = await fetch(`${API_BASE_URL}${url}`, {
        ...options,
        headers
    });
    
    if (!response.ok) {
        throw new Error(`API Error: ${response.statusText}`);
    }
    
    return response.json();
}
