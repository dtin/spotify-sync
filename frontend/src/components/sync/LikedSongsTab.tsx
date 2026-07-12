import { Switch } from "@/components/ui/switch";
import { Label } from "@/components/ui/label";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { Badge } from "@/components/ui/badge";
import { Heart } from "lucide-react";

export interface TrackResponse {
    trackId: string;
    name: string;
    artistName: string;
    albumName: string;
    albumImageUrl: string;
    addedAt: string;
    synced: boolean;
}

interface LikedSongsTabProps {
    tracks: TrackResponse[];
    syncEnabled: boolean;
    onToggleSync: (enabled: boolean) => void;
    loading: boolean;
}

export function LikedSongsTab({ tracks, syncEnabled, onToggleSync, loading }: LikedSongsTabProps) {
    if (loading) {
        return <div className="p-8 text-center text-muted-foreground animate-pulse">Loading liked songs...</div>;
    }

    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between p-4 bg-card rounded-lg border">
                <div className="flex items-center gap-3">
                    <div className="bg-primary/20 p-2 rounded-full text-primary">
                        <Heart className="w-5 h-5 fill-current" />
                    </div>
                    <div>
                        <Label htmlFor="sync-liked-songs" className="text-base font-semibold cursor-pointer">
                            Sync all Liked Songs
                        </Label>
                        <p className="text-sm text-muted-foreground">
                            {tracks.length} tracks will be copied (Oldest to Newest)
                        </p>
                    </div>
                </div>
                <Switch 
                    id="sync-liked-songs" 
                    checked={syncEnabled} 
                    onCheckedChange={onToggleSync} 
                />
            </div>
            
            <Separator />
            
            <div className="rounded-md border">
                <div className="bg-muted/50 p-3 grid grid-cols-[auto_1fr_auto] gap-4 items-center text-sm font-medium text-muted-foreground border-b">
                    <div className="w-10 text-center">#</div>
                    <div>Title</div>
                    <div className="w-24 text-center">Status</div>
                </div>
                <ScrollArea className="h-[400px]">
                    <div className="p-2 space-y-1">
                        {tracks.map((track, i) => (
                            <div key={track.trackId} className="grid grid-cols-[auto_1fr_auto] gap-4 items-center p-2 hover:bg-muted/50 rounded-md group transition-colors">
                                <div className="w-10 text-center text-sm text-muted-foreground">{i + 1}</div>
                                <div className="flex items-center gap-3 overflow-hidden">
                                    <img src={track.albumImageUrl || "/placeholder.svg"} alt={track.albumName} className="w-10 h-10 rounded object-cover shadow-sm" />
                                    <div className="truncate">
                                        <p className="font-medium truncate text-foreground">{track.name}</p>
                                        <p className="text-sm text-muted-foreground truncate">{track.artistName} • {track.albumName}</p>
                                    </div>
                                </div>
                                <div className="w-24 flex justify-center">
                                    {track.synced ? (
                                        <Badge variant="secondary" className="bg-primary/20 text-primary hover:bg-primary/30">Synced</Badge>
                                    ) : (
                                        <span className="text-xs text-muted-foreground">—</span>
                                    )}
                                </div>
                            </div>
                        ))}
                        {tracks.length === 0 && (
                            <div className="text-center py-12 text-muted-foreground">
                                No liked songs found.
                            </div>
                        )}
                    </div>
                </ScrollArea>
            </div>
        </div>
    );
}
