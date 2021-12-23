package com.discordsrv.api.event.events.message.receive.game;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import org.jetbrains.annotations.NotNull;

public class ServerSwitchMessageReceiveEvent extends AbstractGameMessageReceiveEvent {

    private final DiscordSRVPlayer player;

    public ServerSwitchMessageReceiveEvent(DiscordSRVPlayer player, @NotNull MinecraftComponent message, boolean cancelled) {
        super(message, cancelled);
        this.player = player;
    }

    public DiscordSRVPlayer getPlayer() {
        return player;
    }
}
