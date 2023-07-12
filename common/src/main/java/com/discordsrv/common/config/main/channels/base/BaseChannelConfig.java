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

package com.discordsrv.common.config.main.channels.base;

import com.discordsrv.common.config.configurate.annotation.Order;
import com.discordsrv.common.config.main.channels.*;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class BaseChannelConfig {

    @Order(0)
    public MinecraftToDiscordChatConfig minecraftToDiscord = new MinecraftToDiscordChatConfig();
    @Order(0)
    public DiscordToMinecraftChatConfig discordToMinecraft = new DiscordToMinecraftChatConfig();

    public JoinMessageConfig joinMessages() {
        return null;
    }

    @Order(2)
    public LeaveMessageConfig leaveMessages = new LeaveMessageConfig();

    @Order(20)
    public StartMessageConfig startMessage = new StartMessageConfig();
    @Order(20)
    public StopMessageConfig stopMessage = new StopMessageConfig();

    @Order(30)
    @Comment("Settings for synchronizing messages between the defined Discord channels and threads")
    public MirroringConfig mirroring = new MirroringConfig();

    @Order(50)
    public ChannelLockingConfig channelLocking = new ChannelLockingConfig();
}
