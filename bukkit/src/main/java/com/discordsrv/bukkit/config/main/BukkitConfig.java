/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.bukkit.config.main;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.main.PluginIntegrationConfig;
import com.discordsrv.common.config.main.channels.base.ChannelConfig;
import com.discordsrv.common.config.main.channels.base.server.ServerBaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.server.ServerChannelConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class BukkitConfig extends MainConfig {

    public BukkitConfig() {
        channels.clear();
        channels.put(GameChannel.DEFAULT_NAME, new ServerChannelConfig());
        channels.put(ChannelConfig.DEFAULT_KEY, new ServerBaseChannelConfig());
    }

    public BukkitRequiredLinkingConfig requiredLinking = new BukkitRequiredLinkingConfig();

    public PluginIntegrationConfig integrations = new PluginIntegrationConfig();

    @Override
    public PluginIntegrationConfig integrations() {
        return integrations;
    }
}
