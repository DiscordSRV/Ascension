/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.sponge.plugin;

import com.discordsrv.common.plugin.Plugin;
import com.discordsrv.common.plugin.PluginManager;
import com.discordsrv.sponge.SpongeDiscordSRV;
import org.spongepowered.plugin.metadata.PluginMetadata;
import org.spongepowered.plugin.metadata.model.PluginContributor;

import java.util.List;
import java.util.stream.Collectors;

public class SpongePluginManager implements PluginManager {

    private final SpongeDiscordSRV discordSRV;

    public SpongePluginManager(SpongeDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public boolean isPluginEnabled(String pluginName) {
        return discordSRV.game().pluginManager().plugin(pluginName).isPresent();
    }

    @Override
    public List<Plugin> getPlugins() {
        return discordSRV.game().pluginManager().plugins().stream()
                .map(container -> {
                    PluginMetadata metadata = container.metadata();
                    String id = metadata.id();
                    List<String> authors = metadata.contributors().stream()
                            .map(PluginContributor::name)
                            .collect(Collectors.toList());
                    return new Plugin(id, metadata.name().orElse(id), metadata.version().toString(), authors);
                })
                .collect(Collectors.toList());
    }
}
