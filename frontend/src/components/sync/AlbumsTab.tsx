import { Checkbox } from "@/components/ui/checkbox";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { Badge } from "@/components/ui/badge";
import { Label } from "@/components/ui/label";
import { useSelection } from "@/hooks/use-selection";
import { useEffect, useRef } from "react";
import { useVirtualizer } from "@tanstack/react-virtual";

export interface AlbumResponse {
    spotifyAlbumId: string;
    name: string;
    artistName: string;
    imageUrl: string;
    totalTracks: number;
    releaseDate: string;
    synced: boolean;
}

interface AlbumsTabProps {
    albums: AlbumResponse[];
    onSelectionChange: (selectedIds: string[]) => void;
    loading: boolean;
}

export function AlbumsTab({ albums, onSelectionChange, loading }: AlbumsTabProps) {
    const { 
        selectedIds, 
        isAllSelected, 
        isIndeterminate, 
        toggleAll, 
        toggleItem 
    } = useSelection(albums, 'spotifyAlbumId');

    // Notify parent when selection changes
    useEffect(() => {
        onSelectionChange(selectedIds);
    }, [selectedIds, onSelectionChange]);

    const parentRef = useRef<HTMLDivElement>(null);
    const rowVirtualizer = useVirtualizer({
        count: albums.length,
        getScrollElement: () => parentRef.current,
        estimateSize: () => 64, // 64px height per row
        overscan: 5,
    });

    if (loading) {
        return <div className="p-8 text-center text-muted-foreground animate-pulse">Loading albums...</div>;
    }

    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between p-4 bg-card rounded-lg border">
                <div className="flex items-center gap-3">
                    <Checkbox 
                        id="select-all-albums" 
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
                    <div>
                        <Label htmlFor="select-all-albums" className="text-base font-semibold cursor-pointer">
                            Select All Albums
                        </Label>
                        <p className="text-sm text-muted-foreground">
                            {selectedIds.length} of {albums.length} selected
                        </p>
                    </div>
                </div>
            </div>
            
            <Separator />
            
            <div className="rounded-md border">
                <ScrollArea viewportRef={parentRef} className="h-[400px]">
                    <div
                        className="w-full relative"
                        style={{ height: `${rowVirtualizer.getTotalSize()}px` }}
                    >
                        {rowVirtualizer.getVirtualItems().map((virtualRow) => {
                            const album = albums[virtualRow.index];
                            return (
                                <div 
                                    key={album.spotifyAlbumId}
                                    style={{
                                        position: 'absolute',
                                        top: 0,
                                        left: 0,
                                        width: '100%',
                                        height: `${virtualRow.size}px`,
                                        transform: `translateY(${virtualRow.start}px)`,
                                    }}
                                    className="flex items-center gap-4 p-2 hover:bg-muted/50 transition-colors cursor-pointer border-b border-transparent hover:border-muted"
                                    onClick={() => toggleItem(album.spotifyAlbumId)}
                                >
                                    <Checkbox 
                                        checked={selectedIds.includes(album.spotifyAlbumId)} 
                                        onCheckedChange={() => toggleItem(album.spotifyAlbumId)}
                                        onClick={(e) => e.stopPropagation()}
                                    />
                                    <img src={album.imageUrl || "/placeholder.svg"} alt={album.name} className="w-12 h-12 rounded object-cover shadow-sm" />
                                    <div className="flex-1 min-w-0">
                                        <p className="font-medium text-foreground truncate">{album.name}</p>
                                        <p className="text-sm text-muted-foreground truncate">
                                            {album.artistName} • {album.totalTracks} tracks
                                        </p>
                                    </div>
                                    <div className="w-24 text-right pr-2">
                                        {album.synced ? (
                                            <Badge variant="secondary" className="bg-primary/20 text-primary hover:bg-primary/30">Synced</Badge>
                                        ) : (
                                            <span className="text-xs text-muted-foreground">—</span>
                                        )}
                                    </div>
                                </div>
                            );
                        })}
                        {albums.length === 0 && (
                            <div className="text-center py-12 text-muted-foreground">
                                No saved albums found.
                            </div>
                        )}
                    </div>
                </ScrollArea>
            </div>
        </div>
    );
}
