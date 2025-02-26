package com.discordsrv.bukkit.listener;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.debug.EventObserver;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.jetbrains.annotations.NotNull;

public class BukkitWorldChangeListener extends AbstractBukkitListener<PlayerChangedWorldEvent>{

    public BukkitWorldChangeListener(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "WORLD_CHANGE_LISTENER"));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerChangedWorldEvent event) {
        handleEventWithErrorHandling(event);
    }

    @Override
    protected void handleEvent(@NotNull PlayerChangedWorldEvent event, Void __) {
        IPlayer player = discordSRV.playerProvider().player(event.getPlayer());
        discordSRV.eventBus().publish(new com.discordsrv.common.events.player.PlayerChangedWorldEvent(player));
    }

    private EventObserver<PlayerChangedWorldEvent, Boolean> observer;

    @Override
    protected void observeEvents(boolean enable) {
        observer = observeEvent(observer, PlayerChangedWorldEvent.class, event -> event.getFrom() == null , enable);
    }
}
