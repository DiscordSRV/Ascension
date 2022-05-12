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

package com.discordsrv.common.discord.api.entity.channel;

import com.discordsrv.api.discord.entity.channel.DiscordTextChannel;
import com.discordsrv.common.DiscordSRV;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.Nullable;

public class DiscordTextChannelImpl extends AbstractDiscordThreadedGuildMessageChannel<TextChannel>
        implements DiscordTextChannel {

    public DiscordTextChannelImpl(DiscordSRV discordSRV, TextChannel textChannel) {
        super(discordSRV, textChannel);
    }

    @Override
    public @Nullable String getTopic() {
        return channel.getTopic();
    }

    @Override
    public TextChannel getAsJDATextChannel() {
        return channel;
    }

    @Override
    public String toString() {
        return "TextChannel:" + getName() + "(" + Long.toUnsignedString(getId()) + ")";
    }
}
