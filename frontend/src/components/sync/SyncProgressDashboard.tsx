'use client';
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Separator } from "@/components/ui/separator";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { CheckCircle2, Loader2, XCircle, Music2, Check, SkipForward, AlertTriangle } from "lucide-react";
import { SyncProgressDTO, SyncTaskDTO, SyncedTrackInfo } from "@/hooks/use-sync-websocket";

function TaskStatusIcon({ status }: { status: SyncTaskDTO['status'] }) {
    if (status === 'COMPLETED') return <CheckCircle2 className="w-4 h-4 text-green-500 shrink-0" />;
    if (status === 'FAILED') return <XCircle className="w-4 h-4 text-destructive shrink-0" />;
    if (status === 'IN_PROGRESS') return <Loader2 className="w-4 h-4 text-primary animate-spin shrink-0" />;
    return <div className="w-4 h-4 rounded-full border-2 border-muted-foreground/40 shrink-0" />;
}

function TrackFeed({ task }: { task: SyncTaskDTO }) {
    const isActive = task.status === 'IN_PROGRESS';
    const hasTracks = (task.recentlySyncedTracks?.length ?? 0) > 0 || task.currentTrackName;

    if (!hasTracks && task.status === 'PENDING') {
        return (
            <p className="text-xs text-muted-foreground italic px-1 py-2">Waiting to start...</p>
        );
    }
    if (!hasTracks) return null;

    return (
        <div className="mt-2 space-y-1 max-h-[180px] overflow-y-auto pr-1 custom-scrollbar">
            {/* Current syncing track - animated */}
            {isActive && task.currentTrackName && (
                <div className="flex items-center gap-2 px-2 py-1.5 rounded-md bg-primary/10 border border-primary/20 animate-pulse">
                    <div className="flex items-center gap-1.5 shrink-0">
                        <Music2 className="w-3 h-3 text-primary animate-bounce" />
                        <span className="text-[10px] font-semibold text-primary uppercase tracking-wider">Syncing</span>
                    </div>
                    <div className="flex-1 min-w-0">
                        <p className="text-xs font-medium text-foreground truncate">{task.currentTrackName}</p>
                        {task.currentArtistName && (
                            <p className="text-[10px] text-muted-foreground truncate">{task.currentArtistName}</p>
                        )}
                    </div>
                </div>
            )}

            {/* Recently synced tracks */}
            {task.recentlySyncedTracks?.map((track, idx) => (
                <TrackRow key={`${track.trackName}-${idx}`} track={track} />
            ))}
        </div>
    );
}

function TrackRow({ track }: { track: SyncedTrackInfo }) {
    const isSynced = track.status === 'SYNCED';
    return (
        <div className="flex items-center gap-2 px-2 py-1 rounded hover:bg-muted/30 transition-colors group">
            <div className={`w-4 h-4 rounded-full flex items-center justify-center shrink-0 ${isSynced ? 'bg-green-500/15' : 'bg-muted'}`}>
                {isSynced
                    ? <Check className="w-2.5 h-2.5 text-green-500" />
                    : <SkipForward className="w-2.5 h-2.5 text-muted-foreground" />
                }
            </div>
            <div className="flex-1 min-w-0">
                <p className="text-xs text-foreground/80 truncate">{track.trackName}</p>
                {track.artistName && (
                    <p className="text-[10px] text-muted-foreground truncate">{track.artistName}</p>
                )}
            </div>
            <span className={`text-[10px] font-medium shrink-0 ${isSynced ? 'text-green-500' : 'text-muted-foreground'}`}>
                {isSynced ? 'Synced' : 'Skipped'}
            </span>
        </div>
    );
}

function TaskCard({ task }: { task: SyncTaskDTO }) {
    const isCompleted = task.status === 'COMPLETED';
    const isFailed = task.status === 'FAILED';
    const isActive = task.status === 'IN_PROGRESS';

    return (
        <div className={`rounded-lg border transition-all duration-300 ${
            isActive ? 'border-primary/40 bg-primary/5 shadow-sm shadow-primary/10' :
            isCompleted ? 'border-green-500/30 bg-green-500/5' :
            isFailed ? 'border-destructive/30 bg-destructive/5' :
            'border-muted bg-muted/10'
        }`}>
            <div className="p-3 space-y-2">
                {/* Task header */}
                <div className="flex items-center gap-2">
                    <TaskStatusIcon status={task.status} />
                    {task.itemImageUrl ? (
                        <img src={task.itemImageUrl} alt="" className="w-8 h-8 rounded object-cover shadow-sm" />
                    ) : (
                        <div className="w-8 h-8 rounded bg-primary/20 flex items-center justify-center text-xs font-bold text-primary shrink-0">
                            {task.type === 'LIKED_SONGS' ? '♥' : task.type.substring(0, 1)}
                        </div>
                    )}
                    <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between gap-2">
                            <span className="font-medium text-sm truncate">{task.itemName}</span>
                            <Badge
                                variant="outline"
                                className={`text-[10px] px-1.5 py-0 shrink-0 ${
                                    isCompleted ? 'border-green-500/50 text-green-500' :
                                    isFailed ? 'border-destructive/50 text-destructive' :
                                    isActive ? 'border-primary/50 text-primary' :
                                    'text-muted-foreground'
                                }`}
                            >
                                {task.status}
                            </Badge>
                        </div>
                        <div className="flex items-center justify-between mt-0.5">
                            <span className="text-xs text-muted-foreground">
                                {task.syncedTracks} / {task.totalTracks} tracks
                            </span>
                            <span className="text-xs text-muted-foreground">
                                {Math.round(task.progressPercent)}%
                            </span>
                        </div>
                    </div>
                </div>

                {/* Progress bar */}
                <Progress
                    value={task.progressPercent}
                    className={`h-1.5 ${isCompleted ? '[&>div]:bg-green-500' : isFailed ? '[&>div]:bg-destructive' : ''}`}
                />

                {/* Error message */}
                {task.errorMessage && (
                    <div className="flex items-start gap-1.5 bg-destructive/10 rounded px-2 py-1.5">
                        <AlertTriangle className="w-3 h-3 text-destructive mt-0.5 shrink-0" />
                        <p className="text-xs text-destructive break-all">{task.errorMessage}</p>
                    </div>
                )}

                {/* Live track feed */}
                <TrackFeed task={task} />
            </div>
        </div>
    );
}

export function SyncProgressDashboard({ progress }: { progress: SyncProgressDTO | null }) {
    if (!progress) return null;

    const isComplete = progress.status === 'COMPLETED';
    const isError = progress.status === 'FAILED';
    const inProgress = progress.status === 'IN_PROGRESS';

    return (
        <Card className="w-full mt-6 border-primary/20 shadow-lg overflow-hidden">
            <CardHeader className="pb-3 bg-muted/30">
                <div className="flex items-center justify-between">
                    <CardTitle className="flex items-center gap-2 text-base">
                        {isComplete ? <CheckCircle2 className="text-green-500" /> :
                         isError ? <XCircle className="text-destructive" /> :
                         <Loader2 className="animate-spin text-primary" />}
                        Sync Progress
                    </CardTitle>
                    <Badge
                        className={`font-semibold text-xs ${
                            isComplete ? 'bg-green-500 hover:bg-green-500 text-white' :
                            isError ? 'bg-destructive text-destructive-foreground' :
                            inProgress ? 'bg-primary text-primary-foreground' :
                            ''
                        }`}
                    >
                        {progress.status}
                    </Badge>
                </div>
            </CardHeader>

            <CardContent className="pt-5 space-y-5">
                {/* Overall progress */}
                <div className="space-y-2">
                    <div className="flex justify-between text-sm">
                        <span className="font-medium text-muted-foreground">Overall</span>
                        <span className="text-muted-foreground tabular-nums">
                            {progress.completedTasks}/{progress.totalTasks} tasks
                            {progress.failedTasks > 0 && (
                                <span className="text-destructive ml-1">({progress.failedTasks} failed)</span>
                            )}
                        </span>
                    </div>
                    <Progress
                        value={progress.overallProgressPercent}
                        className={`h-2.5 ${isComplete ? '[&>div]:bg-green-500' : ''}`}
                    />
                    <p className="text-right text-xs text-muted-foreground tabular-nums">
                        {Math.round(progress.overallProgressPercent)}% complete
                    </p>
                </div>

                <Separator />

                {/* Task list */}
                <div className="space-y-3">
                    <h4 className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
                        Tasks ({progress.tasks.length})
                    </h4>
                    <ScrollArea className="max-h-[520px] pr-1">
                        <div className="space-y-3 pb-1">
                            {progress.tasks.map((task) => (
                                <TaskCard key={task.taskId} task={task} />
                            ))}
                        </div>
                    </ScrollArea>
                </div>
            </CardContent>
        </Card>
    );
}
