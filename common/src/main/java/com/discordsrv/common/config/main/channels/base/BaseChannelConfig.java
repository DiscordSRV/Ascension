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

package com.discordsrv.common.config.main.channels.base;

import com.discordsrv.common.config.configurate.annotation.Order;
import com.discordsrv.common.config.main.channels.*;
import com.discordsrv.common.config.main.generic.MentionsConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public class BaseChannelConfig {

    @Order(0)
    public MinecraftToDiscordChatConfig minecraftToDiscord = new MinecraftToDiscordChatConfig();

    @Order(0)
    public DiscordToMinecraftChatConfig discordToMinecraft = new DiscordToMinecraftChatConfig();

    // joinMessages
    public JoinMessageConfig joinMessages() {
        return new JoinMessageConfig();
    }

    @Order(2)
    public LeaveMessageConfig leaveMessages = new LeaveMessageConfig();

    // Award, death, server switching

    @Order(20)
    public StartMessageConfig startMessage = new StartMessageConfig();
    @Order(20)
    public StopMessageConfig stopMessage = new StopMessageConfig();

    @Order(30)
    @Comment("Settings for synchronizing messages between the defined Discord channels and threads")
    public MirroringConfig mirroring = new MirroringConfig();

    @Order(50)
    public ChannelLockingConfig channelLocking = new ChannelLockingConfig();

    @Order(80)
    @Comment("Selection for roles which should be shown in-game")
    public RoleSelection roleSelection = new RoleSelection();

    public static class RoleSelection {
        public List<Long> ids = new ArrayList<>();
        public boolean blacklist = true;
    }

    @Comment("The representations of Discord mentions in-game")
    @Order(100)
    public MentionsConfig mentions = new MentionsConfig();
}
