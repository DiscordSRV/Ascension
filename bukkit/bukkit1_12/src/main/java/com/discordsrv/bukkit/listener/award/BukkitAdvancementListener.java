package com.discordsrv.bukkit.listener.award;

import com.discordsrv.common.DiscordSRV;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public class BukkitAdvancementListener extends AbstractBukkitAwardListener {

    public BukkitAdvancementListener(DiscordSRV discordSRV, IBukkitAwardForwarder forwarder) {
        super(discordSRV, forwarder);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        // TODO:
        // - NMS: check if the advancement should be broadcasted to chat, returning if not
        // - run checkIfShouldSkip from parent
        // - NMS: get advancement and/or 'advancement award message'
        // - run forwarder.publishEvent
    }
}
