/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.fabric.module;

import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.channel.GameChannelLookupEvent;
import com.discordsrv.common.core.debug.DebugGenerateEvent;
import com.discordsrv.common.core.debug.file.TextDebugFile;
import com.discordsrv.common.feature.channel.world.WorldChannel;
import com.discordsrv.fabric.FabricDiscordSRV;
import net.kyori.adventure.key.Keyed;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.stream.Collectors;

public class FabricWorldChannelLookupModule extends AbstractFabricModule {

    public FabricWorldChannelLookupModule(FabricDiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Subscribe(priority = EventPriorities.LATE)
    public void onGameChannelLookup(GameChannelLookupEvent event) {
        for (ResourceKey<Level> levelKey : discordSRV.getServer().levelKeys()) {
            if (event.getChannelName().equals(levelKey.location().getPath())) {
                event.process(new WorldChannel(discordSRV, levelKey.location()));
                return;
            }
        }
    }

    @Subscribe
    public void onDebugGenerateDebugInfo(DebugGenerateEvent event) {
        StringBuilder worldList = new StringBuilder();
        worldList.append(discordSRV.getServer().levelKeys().stream().map(ResourceKey::location)
                .map(ResourceLocation::toString)
                .collect(Collectors.joining("\n")));
        event.addFile("integrated-worlds.txt", new TextDebugFile(worldList));
    }
}