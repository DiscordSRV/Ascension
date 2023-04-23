package com.discordsrv.bukkit.listener.award;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.bukkit.component.PaperComponentHandle;
import com.discordsrv.common.DiscordSRV;
import io.papermc.paper.advancement.AdvancementDisplay;
import org.bukkit.advancement.Advancement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public class PaperModernAdvancementListener extends AbstractBukkitAwardListener {

    private static final PaperComponentHandle<PlayerAdvancementDoneEvent> MESSAGE_HANDLE;
    private static final PaperComponentHandle<Advancement> DISPLAY_NAME_HANDLE;

    static {
        MESSAGE_HANDLE = new PaperComponentHandle<>(
                PlayerAdvancementDoneEvent.class,
                "message",
                null
        );
        DISPLAY_NAME_HANDLE = new PaperComponentHandle<>(
                Advancement.class,
                "displayName",
                null
        );
    }

    private final DiscordSRV discordSRV;

    public PaperModernAdvancementListener(DiscordSRV discordSRV, IBukkitAwardForwarder forwarder) {
        super(discordSRV, forwarder);
        this.discordSRV = discordSRV;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        Advancement advancement = event.getAdvancement();
        AdvancementDisplay display = advancement.getDisplay();
        if (display == null || !display.doesAnnounceToChat()) {
            logger.trace("Skipping advancement display of \"" + advancement.getKey().getKey() + "\" for "
                                 + event.getPlayer() + ": advancement display == null or does not broadcast to chat");
            return;
        }

        if (checkIfShouldSkip(event.getPlayer())) {
            return;
        }

        MinecraftComponent message = MESSAGE_HANDLE.getComponent(discordSRV, event);
        MinecraftComponent displayName = DISPLAY_NAME_HANDLE.getComponent(discordSRV, advancement);
        forwarder.publishEvent(event.getPlayer(), message, displayName, false);
    }
}
