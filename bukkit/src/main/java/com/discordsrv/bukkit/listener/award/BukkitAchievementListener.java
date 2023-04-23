package com.discordsrv.bukkit.listener.award;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;

public class BukkitAchievementListener extends AbstractBukkitAwardListener {

    public BukkitAchievementListener(DiscordSRV discordSRV, IBukkitAwardForwarder forwarder) {
        super(discordSRV, forwarder);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAchievementAwarded(PlayerAchievementAwardedEvent event) {
        if (checkIfShouldSkip(event.getPlayer())) {
            return;
        }

        MinecraftComponent name = ComponentUtil.toAPI(BukkitComponentSerializer.legacy().deserialize(event.getAchievement().name()));
        forwarder.publishEvent(event.getPlayer(), null, name, event.isCancelled());
    }
}
