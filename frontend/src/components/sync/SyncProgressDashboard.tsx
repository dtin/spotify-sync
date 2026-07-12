import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Separator } from "@/components/ui/separator";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { CheckCircle2, Loader2, XCircle } from "lucide-react";
import { SyncProgressDTO } from "@/hooks/use-sync-websocket";

export function SyncProgressDashboard({ progress }: { progress: SyncProgressDTO | null }) {
    if (!progress) return null;

    const isComplete = progress.status === 'COMPLETED';
    const isError = progress.status === 'FAILED';

    return (
        <Card className="w-full mt-6 border-primary/20 shadow-lg">
            <CardHeader className="pb-3 bg-muted/30">
                <div className="flex items-center justify-between">
                    <CardTitle className="flex items-center gap-2">
                        {isComplete ? <CheckCircle2 className="text-primary" /> : 
                         isError ? <XCircle className="text-destructive" /> : 
                         <Loader2 className="animate-spin text-primary" />}
                        Sync Progress
                    </CardTitle>
                    <Badge variant={isComplete ? "default" : isError ? "destructive" : "secondary"}>
                        {progress.status}
                    </Badge>
                </div>
            </CardHeader>
            <CardContent className="pt-6 space-y-6">
                <div className="space-y-2">
                    <div className="flex justify-between text-sm">
                        <span className="font-medium">Overall Progress</span>
                        <span className="text-muted-foreground">{Math.round(progress.overallProgressPercent)}% ({progress.completedTasks}/{progress.totalTasks} tasks)</span>
                    </div>
                    <Progress value={progress.overallProgressPercent} className="h-3" />
                </div>

                <Separator />

                <div className="space-y-3">
                    <h4 className="text-sm font-medium uppercase tracking-wider text-muted-foreground">Detailed Tasks</h4>
                    <ScrollArea className="h-[250px] pr-4">
                        <div className="space-y-4">
                            {progress.tasks.map((task) => (
                                <div key={task.taskId} className="space-y-2 bg-muted/20 p-3 rounded-lg">
                                    <div className="flex items-center gap-3">
                                        {task.itemImageUrl ? (
                                            <img src={task.itemImageUrl} alt="" className="w-8 h-8 rounded object-cover" />
                                        ) : (
                                            <div className="w-8 h-8 rounded bg-primary/20 flex items-center justify-center text-xs font-bold text-primary">
                                                {task.type.substring(0, 1)}
                                            </div>
                                        )}
                                        <div className="flex-1 min-w-0">
                                            <div className="flex justify-between items-center mb-1">
                                                <span className="font-medium truncate text-sm">{task.itemName}</span>
                                                <span className="text-xs text-muted-foreground ml-2 whitespace-nowrap">
                                                    {task.syncedTracks} / {task.totalTracks} tracks
                                                </span>
                                            </div>
                                            <Progress value={task.progressPercent} className="h-1.5" />
                                        </div>
                                    </div>
                                    {task.errorMessage && (
                                        <p className="text-xs text-destructive mt-1">Error: {task.errorMessage}</p>
                                    )}
                                </div>
                            ))}
                        </div>
                    </ScrollArea>
                </div>
            </CardContent>
        </Card>
    );
}
