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

package com.discordsrv.common.config.configurate.manager.abstraction;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.configurate.manager.MainConfigManager;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.config.main.channels.base.server.ServerBaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.server.ServerChannelConfig;
import org.spongepowered.configurate.objectmapping.ObjectMapper;

import java.util.function.Supplier;

public class ServerConfigManager<C extends MainConfig> extends MainConfigManager<C> {

    public ServerConfigManager(DiscordSRV discordSRV, Supplier<C> configSupplier) {
        super(discordSRV, configSupplier);
    }

    @Override
    public IChannelConfig.Serializer getChannelConfigSerializer(ObjectMapper.Factory mapperFactory) {
        return new IChannelConfig.Serializer(mapperFactory, ServerBaseChannelConfig.class, ServerChannelConfig.class);
    }
}
