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

package com.discordsrv.bungee.plugin;

import com.discordsrv.bungee.BungeeDiscordSRV;
import com.discordsrv.common.plugin.Plugin;
import com.discordsrv.common.plugin.PluginManager;
import net.md_5.bungee.api.plugin.PluginDescription;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BungeePluginManager implements PluginManager {

    private final BungeeDiscordSRV discordSRV;

    public BungeePluginManager(BungeeDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public boolean isPluginEnabled(String pluginName) {
        return discordSRV.proxy().getPluginManager().getPlugin(pluginName) != null;
    }

    @Override
    public List<Plugin> getPlugins() {
        return discordSRV.proxy().getPluginManager().getPlugins().stream()
                .map(plugin -> {
                    PluginDescription description = plugin.getDescription();
                    return new Plugin(
                            description.getName(),
                            description.getVersion(),
                            Collections.singletonList(description.getAuthor())
                    );
                })
                .collect(Collectors.toList());
    }
}
