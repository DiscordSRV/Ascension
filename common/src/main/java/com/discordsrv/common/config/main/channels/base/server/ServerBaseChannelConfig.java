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

package com.discordsrv.common.config.main.channels.base.server;

import com.discordsrv.common.config.annotation.Order;
import com.discordsrv.common.config.main.channels.server.AwardMessageConfig;
import com.discordsrv.common.config.main.channels.server.DeathMessageConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.server.ServerJoinMessageConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class ServerBaseChannelConfig extends BaseChannelConfig {

    @Order(1)
    public ServerJoinMessageConfig joinMessages = new ServerJoinMessageConfig();

    @Order(3)
    @Comment("Advancement/Achievement message configuration")
    public AwardMessageConfig awardMessages = new AwardMessageConfig();

    @Order(3)
    public DeathMessageConfig deathMessages = new DeathMessageConfig();

    @Override
    public ServerJoinMessageConfig joinMessages() {
        return joinMessages;
    }
}
