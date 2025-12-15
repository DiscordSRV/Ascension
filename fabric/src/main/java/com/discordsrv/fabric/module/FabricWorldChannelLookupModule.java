package com.discordsrv.fabric.module;

import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.channel.GameChannelLookupEvent;
import com.discordsrv.common.feature.channel.world.WorldChannel;
import com.discordsrv.fabric.FabricDiscordSRV;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class FabricWorldChannelLookupModule extends AbstractFabricModule {

    public FabricWorldChannelLookupModule(FabricDiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Subscribe(priority = EventPriorities.LATE)
    public void onGameChannelLookup(GameChannelLookupEvent event) {
        for (ResourceKey<Level> levelKey : discordSRV.getServer().levelKeys()) {
            String worldName = levelKey.location().getPath();
            if (event.getChannelName().equalsIgnoreCase(worldName)) {
                event.process(new WorldChannel(discordSRV, worldName));
                return;
            }
        }
    }
}

