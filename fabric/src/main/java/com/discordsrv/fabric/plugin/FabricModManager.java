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

package com.discordsrv.fabric.plugin;

import com.discordsrv.fabric.FabricDiscordSRV;
import com.discordsrv.common.abstraction.plugin.Plugin;
import com.discordsrv.common.abstraction.plugin.PluginManager;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.Person;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class FabricModManager implements PluginManager {

    private final FabricDiscordSRV discordSRV;

    public FabricModManager(FabricDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public boolean isPluginEnabled(String modIdentifier) {
        return FabricLoader.getInstance().isModLoaded(modIdentifier.toLowerCase(Locale.ROOT));
    }

    @Override
    public List<Plugin> getPlugins() {
        return FabricLoader.getInstance().getAllMods().stream()
                .map(modContainer -> {
                    String id = modContainer.getMetadata().getId();
                    return new Plugin(
                            id,
                            modContainer.getMetadata().getName(),
                            modContainer.getMetadata().getVersion().toString(),
                            modContainer.getMetadata().getAuthors().stream().map(Person::getName).collect(Collectors.toList())
                    );
                })
                .collect(Collectors.toList());
    }
}
