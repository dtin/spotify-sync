import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { Badge } from "@/components/ui/badge";
import { Heart } from "lucide-react";
import { useSelection } from "@/hooks/use-selection";
import { useEffect, useRef } from "react";
import { useVirtualizer } from "@tanstack/react-virtual";

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
    onSelectionChange: (selectedIds: string[]) => void;
    loading: boolean;
}

export function LikedSongsTab({ tracks, onSelectionChange, loading }: LikedSongsTabProps) {
    const { 
        selectedIds, 
        isAllSelected, 
        isIndeterminate, 
        toggleAll, 
        toggleItem 
    } = useSelection(tracks, 'trackId');

    // Notify parent when selection changes
    useEffect(() => {
        onSelectionChange(selectedIds);
    }, [selectedIds, onSelectionChange]);

    const parentRef = useRef<HTMLDivElement>(null);
    const rowVirtualizer = useVirtualizer({
        count: tracks.length,
        getScrollElement: () => parentRef.current,
        estimateSize: () => 56, // 56px height per row
        overscan: 5,
    });

    if (loading) {
        return <div className="p-8 text-center text-muted-foreground animate-pulse">Loading liked songs...</div>;
    }

    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between p-4 bg-card rounded-lg border">
                <div className="flex items-center gap-3">
                    <Checkbox 
                        id="select-all-liked" 
                        checked={isAllSelected} 
                        className={isIndeterminate ? "data-[state=unchecked]:bg-primary data-[state=unchecked]:text-primary-foreground" : ""}
                        onClick={(e) => {
                            if (isIndeterminate) {
                                e.preventDefault();
                                toggleAll();
                            }
                        }}
                        onCheckedChange={toggleAll}
                    />
                    <div className="flex items-center gap-2">
                        <div className="bg-primary/20 p-1.5 rounded-full text-primary hidden sm:block">
                            <Heart className="w-4 h-4 fill-current" />
                        </div>
                        <div>
                            <Label htmlFor="select-all-liked" className="text-base font-semibold cursor-pointer">
                                Select All Liked Songs
                            </Label>
                            <p className="text-sm text-muted-foreground">
                                {selectedIds.length} tracks will be copied (Oldest to Newest)
                            </p>
                        </div>
                    </div>
                </div>
            </div>
            
            <Separator />
            
            <div className="rounded-md border">
                <div className="bg-muted/50 p-3 flex gap-4 items-center text-sm font-medium text-muted-foreground border-b pr-8">
                    <div className="w-4"></div>
                    <div className="w-10 text-center">#</div>
                    <div className="flex-1">Title</div>
                    <div className="w-24 text-right">Status</div>
                </div>
                <ScrollArea viewportRef={parentRef} className="h-[400px]">
                    <div
                        className="w-full relative"
                        style={{ height: `${rowVirtualizer.getTotalSize()}px` }}
                    >
                        {rowVirtualizer.getVirtualItems().map((virtualRow) => {
                            const track = tracks[virtualRow.index];
                            return (
                                <div 
                                    key={track.trackId}
                                    style={{
                                        position: 'absolute',
                                        top: 0,
                                        left: 0,
                                        width: '100%',
                                        height: `${virtualRow.size}px`,
                                        transform: `translateY(${virtualRow.start}px)`,
                                    }}
                                    className="flex gap-4 items-center p-2 hover:bg-muted/50 group transition-colors cursor-pointer border-b border-transparent hover:border-muted"
                                    onClick={() => toggleItem(track.trackId)}
                                >
                                    <Checkbox 
                                        checked={selectedIds.includes(track.trackId)} 
                                        onCheckedChange={() => toggleItem(track.trackId)}
                                        onClick={(e) => e.stopPropagation()}
                                    />
                                    <div className="w-10 text-center text-sm text-muted-foreground">{virtualRow.index + 1}</div>
                                    <div className="flex-1 flex items-center gap-3 overflow-hidden">
                                        <img src={track.albumImageUrl || "/placeholder.svg"} alt={track.albumName} className="w-10 h-10 rounded object-cover shadow-sm" />
                                        <div className="truncate">
                                            <p className="font-medium truncate text-foreground">{track.name}</p>
                                            <p className="text-sm text-muted-foreground truncate">{track.artistName} • {track.albumName}</p>
                                        </div>
                                    </div>
                                    <div className="w-24 flex justify-end pr-2">
                                        {track.synced ? (
                                            <Badge variant="secondary" className="bg-primary/20 text-primary hover:bg-primary/30">Synced</Badge>
                                        ) : (
                                            <span className="text-xs text-muted-foreground">—</span>
                                        )}
                                    </div>
                                </div>
                            );
                        })}
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
