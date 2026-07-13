import { AccountInfo } from "@/hooks/use-spotify-auth";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { LogIn, LogOut } from "lucide-react";

interface AccountCardProps {
    title: string;
    account: AccountInfo;
    onLogin: () => void;
    onLogout: () => void;
}

export function AccountCard({ title, account, onLogin, onLogout }: AccountCardProps) {
    return (
        <Card className="w-full">
            <CardHeader className="pb-3">
                <CardTitle className="text-sm font-medium text-muted-foreground uppercase tracking-wider">
                    {title}
                </CardTitle>
            </CardHeader>
            <CardContent>
                {account?.connected ? (
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-4">
                            <Avatar className="h-12 w-12 border-2 border-primary/20">
                                <AvatarImage src={account.profileImageUrl} alt={account.displayName} />
                                <AvatarFallback className="bg-primary/20 text-primary">
                                    {account.displayName?.[0]?.toUpperCase() || "U"}
                                </AvatarFallback>
                            </Avatar>
                            <div>
                                <p className="font-semibold">{account.displayName}</p>
                                <p className="text-sm text-muted-foreground">{account.email}</p>
                                {account.spotifyId && (
                                    <p className="text-[11px] text-muted-foreground/60 font-mono mt-0.5">
                                        ID: {account.spotifyId}
                                    </p>
                                )}
                            </div>
                        </div>
                        <Button variant="outline" size="sm" onClick={onLogout} className="group">
                            <LogOut className="w-4 h-4 mr-2 group-hover:text-destructive transition-colors" />
                            Disconnect
                        </Button>
                    </div>
                ) : (
                    <div className="flex flex-col items-center justify-center py-4 space-y-4">
                        <p className="text-sm text-muted-foreground">Not connected</p>
                        <Button onClick={onLogin} className="w-full sm:w-auto bg-primary hover:bg-primary/90 text-black font-semibold">
                            <LogIn className="w-4 h-4 mr-2" />
                            Connect Spotify
                        </Button>
                    </div>
                )}
            </CardContent>
        </Card>
    );
}
