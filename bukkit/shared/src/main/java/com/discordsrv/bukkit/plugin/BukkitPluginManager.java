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

package com.discordsrv.bukkit.plugin;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.abstraction.plugin.Plugin;
import com.discordsrv.common.abstraction.plugin.PluginManager;
import com.discordsrv.common.events.integration.IntegrationLifecycleEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BukkitPluginManager implements PluginManager, Listener {

    private final BukkitDiscordSRV discordSRV;

    public BukkitPluginManager(BukkitDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        discordSRV.server().getPluginManager().registerEvents(this, discordSRV.plugin());
    }

    public void disable() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean isPluginEnabled(String pluginName) {
        return discordSRV.server().getPluginManager().isPluginEnabled(pluginName);
    }

    @Override
    public List<Plugin> getPlugins() {
        return Arrays.stream(discordSRV.server().getPluginManager().getPlugins())
                .map(plugin -> {
                    PluginDescriptionFile description = plugin.getDescription();
                    return new Plugin(plugin.getName(), description.getVersion(), description.getAuthors());
                })
                .collect(Collectors.toList());
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        discordSRV.eventBus().publish(new IntegrationLifecycleEvent(
                event.getPlugin().getName(),
                IntegrationLifecycleEvent.Type.ENABLE
        ));
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        discordSRV.eventBus().publish(new IntegrationLifecycleEvent(
                event.getPlugin().getName(),
                IntegrationLifecycleEvent.Type.DISABLE
        ));
    }
}
