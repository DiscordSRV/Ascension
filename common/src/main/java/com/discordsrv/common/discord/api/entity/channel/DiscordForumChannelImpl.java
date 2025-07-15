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

package com.discordsrv.common.discord.api.entity.channel;

import com.discordsrv.api.discord.entity.channel.DiscordChannelType;
import com.discordsrv.api.discord.entity.channel.DiscordForumChannel;
import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.discord.api.entity.message.util.SendableDiscordMessageUtil;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumPost;

public class DiscordForumChannelImpl extends AbstractDiscordForumChannel<ForumChannel> implements DiscordForumChannel {

    public DiscordForumChannelImpl(DiscordSRV discordSRV, ForumChannel channel) {
        super(discordSRV, channel);
    }

    @Override
    public DiscordChannelType getType() {
        return DiscordChannelType.FORUM;
    }

    @Override
    public ForumChannel asJDA() {
        return channel;
    }

    @Override
    public Task<DiscordThreadChannel> createPost(String name, SendableDiscordMessage message) {
        return thread(
                channel -> channel.createForumPost(name, SendableDiscordMessageUtil.toJDASend(message)),
                ForumPost::getThreadChannel
        );
    }

    @Override
    public String toString() {
        return "Forum:" + getName() + "(" + Long.toUnsignedString(getId()) + ")";
    }
}
