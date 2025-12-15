package com.discordsrv.bukkit.module;

import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.channel.GameChannelLookupEvent;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.feature.channel.world.WorldChannel;
import org.bukkit.World;

public class BukkitWorldLookupModule extends AbstractModule<BukkitDiscordSRV> {

    public BukkitWorldLookupModule(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Subscribe(priority = EventPriorities.LATE)
    public void onGameChannelLookup(GameChannelLookupEvent event) {
        for (World world : discordSRV.server().getWorlds()) {
            String worldName = world.getName();
            if (event.getChannelName().equalsIgnoreCase(worldName)) {
                event.process(new WorldChannel(discordSRV, worldName));
                return;
            }
        }
    }
}
