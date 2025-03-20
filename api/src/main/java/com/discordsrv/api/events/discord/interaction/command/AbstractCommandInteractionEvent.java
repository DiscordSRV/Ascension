/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.discordsrv.api.events.discord.interaction.command;

import com.discordsrv.api.DiscordSRV;
import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.channel.DiscordChannel;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.api.discord.entity.interaction.DiscordInteractionHook;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.events.discord.interaction.AbstractInteractionWithHookEvent;
import com.discordsrv.api.task.Task;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractCommandInteractionEvent<E extends GenericCommandInteractionEvent>
        extends AbstractInteractionWithHookEvent<E> {

    private final DiscordSRV discordSRV;

    public AbstractCommandInteractionEvent(
            DiscordSRV discordSRV,
            E jdaEvent,
            ComponentIdentifier identifier,
            DiscordUser user,
            DiscordGuildMember member,
            DiscordMessageChannel channel,
            DiscordInteractionHook interaction
    ) {
        super(jdaEvent, identifier, user, member, channel, interaction);
        this.discordSRV = discordSRV;
    }

    public abstract Task<DiscordInteractionHook> reply(SendableDiscordMessage message, boolean ephemeral);

    public Task<DiscordInteractionHook> reply(SendableDiscordMessage message) {
        return reply(message, false);
    }

    public abstract Task<DiscordInteractionHook> deferReply(boolean ephemeral);

    @Nullable
    public String getOptionAsString(String name) {
        OptionMapping mapping = jdaEvent.getOption(name);
        return mapping != null ? mapping.getAsString() : null;
    }

    @Nullable
    public DiscordUser getOptionAsUser(String name) {
        OptionMapping mapping = jdaEvent.getOption(name);
        if (mapping == null) {
            return null;
        }
        long id = mapping.getAsLong();
        return discordSRV.discordAPI().getUserById(id);
    }

    @Nullable
    public DiscordRole getOptionAsRole(String name) {
        OptionMapping mapping = jdaEvent.getOption(name);
        if (mapping == null) {
            return null;
        }
        long id = mapping.getAsLong();
        return discordSRV.discordAPI().getRoleById(id);
    }

    @Nullable
    public DiscordChannel getOptionAsChannel(String name) {
        OptionMapping mapping = jdaEvent.getOption(name);
        if (mapping == null) {
            return null;
        }
        long id = mapping.getAsLong();
        return discordSRV.discordAPI().getChannelById(id);
    }

    @Nullable
    public Long getOptionAsLong(String name) {
        OptionMapping mapping = jdaEvent.getOption(name);
        if (mapping == null) {
            return null;
        }
        return mapping.getAsLong();
    }

    @Nullable
    public Double getOptionAsDouble(String name) {
        OptionMapping mapping = jdaEvent.getOption(name);
        if (mapping == null) {
            return null;
        }
        return mapping.getAsDouble();
    }

    @Nullable
    public Boolean getOptionAsBoolean(String name) {
        OptionMapping mapping = jdaEvent.getOption(name);
        if (mapping == null) {
            return null;
        }
        return mapping.getAsBoolean();
    }
}
