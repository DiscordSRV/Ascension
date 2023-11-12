package com.discordsrv.bukkit.listener.award;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public class SpigotModernAdvancementListener extends AbstractBukkitAwardListener {

    public SpigotModernAdvancementListener(DiscordSRV discordSRV, IBukkitAwardForwarder forwarder) {
        super(discordSRV, forwarder);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        Advancement advancement = event.getAdvancement();
        AdvancementDisplay display = advancement.getDisplay();
        if (display == null || !display.shouldAnnounceChat()) {
            logger.trace("Skipping advancement display of \"" + advancement.getKey().getKey() + "\" for "
                                 + event.getPlayer() + ": advancement display == null or does not broadcast to chat");
            return;
        }

        MinecraftComponent title = ComponentUtil.toAPI(BukkitComponentSerializer.legacy().deserialize(display.getTitle())) ;
        forwarder.publishEvent(event, event.getPlayer(), title, null, false);
    }
}
