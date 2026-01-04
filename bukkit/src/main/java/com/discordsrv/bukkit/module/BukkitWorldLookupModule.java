/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.discordsrv.bukkit.module;

import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.channel.GameChannelLookupEvent;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.core.debug.DebugGenerateEvent;
import com.discordsrv.common.core.debug.file.TextDebugFile;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.feature.channel.world.WorldChannel;
import net.kyori.adventure.key.Keyed;
import org.bukkit.World;

import java.util.stream.Collectors;

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

    @Subscribe
    public void onDebugGenerateDebugInfo(DebugGenerateEvent event) {
        StringBuilder worldList = new StringBuilder();
        worldList.append(discordSRV.server().getWorlds().stream().map(World::getName)
                .map(name -> WorldChannel.DEFAULT_OWNER_NAME + ":" + name)
                .collect(Collectors.joining("\n")));
        event.addFile("integrated-worlds.txt", new TextDebugFile(worldList));
    }
}
