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

import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.common.DiscordSRV;
import net.dv8tion.jda.api.entities.*;

import java.util.Objects;

public abstract class AbstractDiscordMessageChannel<T extends MessageChannel>
        implements DiscordMessageChannel {

    public static AbstractDiscordMessageChannel<?> get(DiscordSRV discordSRV, MessageChannel messageChannel) {
        if (messageChannel instanceof TextChannel) {
            return new DiscordTextChannelImpl(discordSRV, (TextChannel) messageChannel);
        } else if (messageChannel instanceof ThreadChannel) {
            return new DiscordThreadChannelImpl(discordSRV, (ThreadChannel) messageChannel);
        } else if (messageChannel instanceof PrivateChannel) {
            return new DiscordDMChannelImpl(discordSRV, (PrivateChannel) messageChannel);
        } else if (messageChannel instanceof NewsChannel) {
            return new DiscordNewsChannelImpl(discordSRV, (NewsChannel) messageChannel);
        } else {
            throw new IllegalArgumentException("Unmappable MessageChannel type: " + messageChannel.getClass().getName());
        }
    }

    protected final DiscordSRV discordSRV;
    protected final T channel;

    public AbstractDiscordMessageChannel(DiscordSRV discordSRV, T channel) {
        this.discordSRV = discordSRV;
        this.channel = channel;
    }

    @Override
    public long getId() {
        return channel.getIdLong();
    }

    @Override
    public MessageChannel getAsJDAMessageChannel() {
        return channel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractDiscordGuildMessageChannel<?> that = (AbstractDiscordGuildMessageChannel<?>) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
