/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.velocity.plugin;

import com.discordsrv.common.abstraction.plugin.Plugin;
import com.discordsrv.common.abstraction.plugin.PluginManager;
import com.discordsrv.velocity.VelocityDiscordSRV;
import com.velocitypowered.api.plugin.PluginDescription;

import java.util.List;
import java.util.stream.Collectors;

public class VelocityPluginManager implements PluginManager {

    private final VelocityDiscordSRV discordSRV;

    public VelocityPluginManager(VelocityDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public boolean isPluginEnabled(String pluginName) {
        return discordSRV.proxy().getPluginManager().isLoaded(pluginName);
    }

    @Override
    public List<Plugin> getPlugins() {
        return discordSRV.proxy().getPluginManager().getPlugins().stream()
                .map(container -> {
                    PluginDescription description = container.getDescription();
                    String id = description.getId();
                    return new Plugin(
                            id,
                            description.getName().orElse(id),
                            description.getVersion().orElse("Unknown"),
                            description.getAuthors()
                    );
                })
                .collect(Collectors.toList());
    }
}
