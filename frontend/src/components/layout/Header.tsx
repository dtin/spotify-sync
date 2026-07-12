import { Music } from "lucide-react";

export function Header() {
    return (
        <header className="flex items-center gap-3 py-6 px-4 md:px-8 border-b">
            <div className="bg-primary p-2 rounded-full text-black">
                <Music size={24} />
            </div>
            <h1 className="text-2xl font-bold tracking-tight">Spotify Playlist Sync</h1>
        </header>
    );
}
