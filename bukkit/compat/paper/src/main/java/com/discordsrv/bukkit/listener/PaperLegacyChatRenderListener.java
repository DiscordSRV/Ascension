package com.discordsrv.bukkit.listener;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.events.message.render.GameChatRenderEvent;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.component.PaperComponentHandle;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.feature.channel.global.GlobalChannel;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;

public class PaperLegacyChatRenderListener extends AbstractBukkitListener<AsyncChatEvent> {

    private static final PaperComponentHandle.Get<AsyncChatEvent> GET_MESSAGE_HANDLE
            = PaperComponentHandle.get(AsyncChatEvent.class, "message");
    private static final PaperComponentHandle.Set<AsyncChatEvent> SET_MESSAGE_HANDLE
            = PaperComponentHandle.set(AsyncChatEvent.class, "message");

    public PaperLegacyChatRenderListener(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "CHAT_RENDER_LISTENER"));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        handleEventWithErrorHandling(event);
    }

    @Override
    protected void handleEvent(@NotNull AsyncChatEvent event, Void __) {
        Player bukkitPlayer = event.getPlayer();

        IPlayer player = discordSRV.playerProvider().player(bukkitPlayer);
        MinecraftComponent message = GET_MESSAGE_HANDLE.getAPI(event);

        GameChatRenderEvent annotateEvent = new GameChatRenderEvent(
                event,
                player,
                new GlobalChannel(discordSRV),
                message
        );

        discordSRV.eventBus().publish(annotateEvent);

        MinecraftComponent annotatedMessage = annotateEvent.getAnnotatedMessage();
        if (annotatedMessage != null) {
            SET_MESSAGE_HANDLE.call(event, annotatedMessage);
        }
    }

    // Already observed via normal chat listener
    @Override
    protected void observeEvents(boolean enable) {}
}
