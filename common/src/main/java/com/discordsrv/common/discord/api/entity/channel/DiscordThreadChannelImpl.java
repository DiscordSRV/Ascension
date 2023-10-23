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

package com.discordsrv.common.discord.api.entity.channel;

import com.discordsrv.api.discord.entity.channel.DiscordChannelType;
import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;
import com.discordsrv.api.discord.entity.channel.DiscordThreadContainer;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.common.DiscordSRV;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.WebhookClient;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class DiscordThreadChannelImpl extends AbstractDiscordGuildMessageChannel<ThreadChannel> implements DiscordThreadChannel {

    private final DiscordThreadContainer threadContainer;
    private final DiscordGuild guild;

    public DiscordThreadChannelImpl(DiscordSRV discordSRV, ThreadChannel thread) {
        super(discordSRV, thread);

        IThreadContainer container = thread.getParentChannel();
        this.threadContainer = (DiscordThreadContainer) discordSRV.discordAPI().getChannel(container);
        this.guild = discordSRV.discordAPI().getGuild(thread.getGuild());
    }

    @Override
    public CompletableFuture<WebhookClient<Message>> queryWebhookClient() {
        return discordSRV.discordAPI()
                .queryWebhookClient(getParentChannel().getId());
    }

    @Override
    protected <R> WebhookMessageCreateAction<R> mapAction(WebhookMessageCreateAction<R> action) {
        return super.mapAction(action).setThreadId(getId());
    }

    @Override
    protected <R> WebhookMessageEditAction<R> mapAction(WebhookMessageEditAction<R> action) {
        return super.mapAction(action).setThreadId(getId());
    }

    @Override
    public @NotNull DiscordGuild getGuild() {
        return guild;
    }

    @Override
    public @NotNull DiscordThreadContainer getParentChannel() {
        return threadContainer;
    }

    @Override
    public String toString() {
        return "Thread:" + getName() + "(" + Long.toUnsignedString(getId()) + " in " + threadContainer + ")";
    }

    @Override
    public ThreadChannel asJDA() {
        return channel;
    }

    @Override
    public DiscordChannelType getType() {
        ChannelType type = channel.getType();
        for (DiscordChannelType value : DiscordChannelType.values()) {
            if (value.asJDA() == type) {
                return value;
            }
        }
        throw new IllegalStateException("Unknown channel type");
    }
}
