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

package com.discordsrv.fabric.config.main;

import com.discordsrv.common.config.configurate.annotation.Order;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.main.PresenceUpdaterConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.server.ServerBaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.server.ServerChannelConfig;
import com.discordsrv.common.config.main.linking.ServerRequiredLinkingConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class FabricConfig extends MainConfig {

    @Order(5)
    public ServerRequiredLinkingConfig requiredLinking = new ServerRequiredLinkingConfig();

    @Override
    public BaseChannelConfig createDefaultBaseChannel() {
        return new ServerBaseChannelConfig();
    }

    @Override
    public BaseChannelConfig createDefaultChannel() {
        return new ServerChannelConfig();
    }

    @Override
    public PresenceUpdaterConfig defaultPresenceUpdater() {
        return new PresenceUpdaterConfig.Server();
    }
}
