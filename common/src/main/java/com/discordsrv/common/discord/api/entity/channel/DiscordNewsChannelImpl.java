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

import com.discordsrv.api.discord.entity.channel.DiscordNewsChannel;
import com.discordsrv.common.DiscordSRV;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;

public class DiscordNewsChannelImpl extends AbstractDiscordThreadedGuildMessageChannel<NewsChannel> implements DiscordNewsChannel {

    public DiscordNewsChannelImpl(DiscordSRV discordSRV, NewsChannel channel) {
        super(discordSRV, channel);
    }

    @Override
    public NewsChannel asJDA() {
        return channel;
    }

    @Override
    public String toString() {
        return "NewsChannel:" + getName() + "(" + Long.toUnsignedString(getId()) + ")";
    }
}
