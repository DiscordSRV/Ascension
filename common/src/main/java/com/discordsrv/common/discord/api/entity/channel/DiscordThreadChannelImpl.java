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

import club.minnced.discord.webhook.WebhookClient;
import com.discordsrv.api.discord.api.entity.channel.DiscordTextChannel;
import com.discordsrv.api.discord.api.entity.channel.DiscordThreadChannel;
import com.discordsrv.api.discord.api.entity.guild.DiscordGuild;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.discord.api.entity.guild.DiscordGuildImpl;
import net.dv8tion.jda.api.entities.IThreadContainer;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class DiscordThreadChannelImpl extends AbstractDiscordGuildMessageChannel<ThreadChannel>
        implements DiscordThreadChannel {

    private final DiscordTextChannel textChannel;
    private final DiscordGuild guild;

    public DiscordThreadChannelImpl(DiscordSRV discordSRV, ThreadChannel thread) {
        super(discordSRV, thread);

        IThreadContainer container = thread.getParentChannel();
        this.textChannel = container instanceof TextChannel
                           ? new DiscordTextChannelImpl(discordSRV, (TextChannel) container)
                           : null;
        this.guild = new DiscordGuildImpl(discordSRV, thread.getGuild());
    }

    @Override
    public CompletableFuture<WebhookClient> queryWebhookClient() {
        return discordSRV.discordAPI()
                .queryWebhookClient(getParentChannel().getId())
                .thenApply(client -> client.onThread(getId()));
    }

    @Override
    public @NotNull String getName() {
        return channel.getName();
    }

    @Override
    public @NotNull DiscordGuild getGuild() {
        return guild;
    }

    @Override
    public @NotNull DiscordTextChannel getParentChannel() {
        return textChannel;
    }

    @Override
    public ThreadChannel getAsJDAThreadChannel() {
        return channel;
    }

    @Override
    public String toString() {
        return "Thread:" + getName() + "(" + Long.toUnsignedString(getId()) + " in " + textChannel + ")";
    }
}
