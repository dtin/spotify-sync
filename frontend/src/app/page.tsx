"use client";

import { useState, useEffect, Suspense } from "react";
import { Header } from "@/components/layout/Header";
import { AccountCard } from "@/components/layout/AccountCard";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { LikedSongsTab, TrackResponse } from "@/components/sync/LikedSongsTab";
import { PlaylistsTab, PlaylistResponse } from "@/components/sync/PlaylistsTab";
import { AlbumsTab, AlbumResponse } from "@/components/sync/AlbumsTab";
import { SyncProgressDashboard } from "@/components/sync/SyncProgressDashboard";
import { Button } from "@/components/ui/button";
import { fetchWithAuth } from "@/lib/api";
import { useSpotifyAuth } from "@/hooks/use-spotify-auth";
import { useSystemAuth } from "@/hooks/use-system-auth";
import { useSyncWebSocket } from "@/hooks/use-sync-websocket";
import { SystemLogin } from "@/components/auth/SystemLogin";
import { Heart, ListMusic, Disc3, Rocket } from "lucide-react";
import { toast } from "sonner";

function HomeContent() {
    const { status, loading: authLoading, login, logout } = useSpotifyAuth();
    const { progress, connected: wsConnected } = useSyncWebSocket();

    const [playlists, setPlaylists] = useState<PlaylistResponse[]>([]);
    const [likedSongs, setLikedSongs] = useState<TrackResponse[]>([]);
    const [albums, setAlbums] = useState<AlbumResponse[]>([]);
    const [dataLoading, setDataLoading] = useState(false);

    // Sync Selection State
    const [selectedLikedSongs, setSelectedLikedSongs] = useState<string[]>([]);
    const [selectedPlaylists, setSelectedPlaylists] = useState<string[]>([]);
    const [selectedAlbums, setSelectedAlbums] = useState<string[]>([]);
    const [isSyncing, setIsSyncing] = useState(false);

    useEffect(() => {
        if (status?.source.connected) {
            fetchSourceData();
        }
    }, [status?.source.connected]);

    useEffect(() => {
        if (progress?.status === 'COMPLETED' || progress?.status === 'FAILED') {
            setIsSyncing(false);
            if (progress.status === 'COMPLETED') {
                toast.success("Sync completed successfully!");
            }
        } else if (progress?.status === 'IN_PROGRESS') {
            setIsSyncing(true);
        }
    }, [progress?.status]);

    const fetchSourceData = async () => {
        setDataLoading(true);
        try {
            const [plRes, lsRes, alRes] = await Promise.all([
                fetchWithAuth('/api/sync/playlists').catch(() => []),
                fetchWithAuth('/api/sync/liked-songs').catch(() => []),
                fetchWithAuth('/api/sync/albums').catch(() => [])
            ]);
            setPlaylists(plRes);
            setLikedSongs(lsRes);
            setAlbums(alRes);
        } catch (error) {
            toast.error("Failed to fetch source data");
        } finally {
            setDataLoading(false);
        }
    };

    const handleStartSync = async () => {
        if (!status?.destination.connected) {
            toast.error("Please connect destination account first");
            return;
        }

        if (status?.source.spotifyId && status?.destination.spotifyId && status.source.spotifyId === status.destination.spotifyId) {
            toast.error("Source and destination cannot be the same account");
            return;
        }

        if (selectedLikedSongs.length === 0 && selectedPlaylists.length === 0 && selectedAlbums.length === 0) {
            toast.warning("Please select at least one item to sync");
            return;
        }

        try {
            setIsSyncing(true);
            const ignoredSongIds = likedSongs.map(t => t.trackId).filter(id => !selectedLikedSongs.includes(id));
            const ignoredPlaylistIds = playlists.map(p => p.spotifyPlaylistId).filter(id => !selectedPlaylists.includes(id));
            const ignoredAlbumIds = albums.map(a => a.spotifyAlbumId).filter(id => !selectedAlbums.includes(id));

            // Build track metadata map for live progress display
            const trackMeta: Record<string, { name: string; artist: string }> = {};
            likedSongs.forEach(t => {
                trackMeta[t.trackId] = { name: t.name, artist: t.artistName };
            });

            await fetchWithAuth('/api/sync/start', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    likedSongIds: selectedLikedSongs,
                    ignoredSongIds: ignoredSongIds,
                    playlistIds: selectedPlaylists,
                    ignoredPlaylistIds: ignoredPlaylistIds,
                    albumIds: selectedAlbums,
                    ignoredAlbumIds: ignoredAlbumIds,
                    trackMeta: trackMeta
                })
            });
            toast.info("Sync process started in the background");
        } catch (error) {
            toast.error("Failed to start sync");
            setIsSyncing(false);
        }
    };

    if (authLoading) return <div className="flex h-screen items-center justify-center">Loading...</div>;

    const canSync = status?.source.connected && status?.destination.connected;
    const totalSelectedItems = (selectedLikedSongs.length > 0 ? 1 : 0) + selectedPlaylists.length + selectedAlbums.length;

    return (
        <div className="min-h-screen bg-background text-foreground pb-10 flex flex-col">
            <Header />
            
            <main className="container max-w-6xl mx-auto px-4 py-8 space-y-8 flex-grow">
                {/* Accounts Section */}
                <div className="grid md:grid-cols-2 gap-6">
                    <AccountCard 
                        title="Source Account (Copy From)" 
                        account={status?.source!} 
                        onLogin={() => login('SOURCE')} 
                        onLogout={() => logout('SOURCE')} 
                    />
                    <AccountCard 
                        title="Destination Account (Copy To)" 
                        account={status?.destination!} 
                        onLogin={() => login('DESTINATION')} 
                        onLogout={() => logout('DESTINATION')} 
                    />
                </div>

                {/* Content Section */}
                {status?.source.connected && (
                    <div className="space-y-6">
                        <Tabs defaultValue="liked-songs" className="w-full flex flex-col">
                            <TabsList className="grid w-full grid-cols-3 max-w-md bg-muted/50 p-1">
                                <TabsTrigger value="liked-songs" className="flex items-center gap-2">
                                    <Heart className="w-4 h-4" /> Liked Songs
                                </TabsTrigger>
                                <TabsTrigger value="playlists" className="flex items-center gap-2">
                                    <ListMusic className="w-4 h-4" /> Playlists
                                </TabsTrigger>
                                <TabsTrigger value="albums" className="flex items-center gap-2">
                                    <Disc3 className="w-4 h-4" /> Albums
                                </TabsTrigger>
                            </TabsList>
                            
                            <div className="mt-6 border rounded-xl bg-card/50 p-1 shadow-sm">
                                <TabsContent value="liked-songs" className="m-0 border-0 p-0">
                                    <LikedSongsTab 
                                        tracks={likedSongs} 
                                        onSelectionChange={setSelectedLikedSongs} 
                                        loading={dataLoading} 
                                    />
                                </TabsContent>
                                <TabsContent value="playlists" className="m-0 border-0 p-0">
                                    <PlaylistsTab 
                                        playlists={playlists} 
                                        onSelectionChange={setSelectedPlaylists} 
                                        loading={dataLoading} 
                                    />
                                </TabsContent>
                                <TabsContent value="albums" className="m-0 border-0 p-0">
                                    <AlbumsTab 
                                        albums={albums} 
                                        onSelectionChange={setSelectedAlbums} 
                                        loading={dataLoading} 
                                    />
                                </TabsContent>
                            </div>
                        </Tabs>

                        {/* Action Bar */}
                        <div className="flex flex-col sm:flex-row items-center justify-between p-6 bg-card rounded-xl border shadow-sm gap-4">
                            <div>
                                <h3 className="font-semibold text-lg">Ready to Sync</h3>
                                <p className="text-muted-foreground text-sm">
                                    {totalSelectedItems} tasks selected for synchronization
                                </p>
                            </div>
                            <Button 
                                size="lg" 
                                className="w-full sm:w-auto font-bold tracking-wide"
                                disabled={!canSync || totalSelectedItems === 0 || isSyncing}
                                onClick={handleStartSync}
                            >
                                <Rocket className="w-5 h-5 mr-2" />
                                {isSyncing ? "Syncing..." : "Start Sync"}
                            </Button>
                        </div>

                        {/* Progress Dashboard */}
                        {(isSyncing || progress) && (
                            <SyncProgressDashboard progress={progress} />
                        )}
                    </div>
                )}
            </main>

            <footer className="w-full flex items-center justify-center py-6 text-sm text-muted-foreground mt-auto">
                From Tin Dam with love <Heart className="w-4 h-4 text-red-500 mx-1 fill-current" />
            </footer>
        </div>
    );
}

export default function Home() {
    const { isAuthenticated, loading, login } = useSystemAuth();

    if (loading) {
        return <div className="flex h-screen items-center justify-center">Loading...</div>;
    }

    if (!isAuthenticated) {
        return <SystemLogin onLoginSuccess={login} />;
    }

    return (
        <Suspense fallback={<div className="flex h-screen items-center justify-center">Loading...</div>}>
            <HomeContent />
        </Suspense>
    );
}
