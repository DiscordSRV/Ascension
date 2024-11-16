/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.events.discord.interaction.command;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.interaction.DiscordInteractionHook;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.events.discord.interaction.command.DiscordMessageContextInteractionEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.discord.api.entity.component.DiscordInteractionHookImpl;
import com.discordsrv.common.discord.api.entity.message.util.SendableDiscordMessageUtil;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;

import java.util.concurrent.CompletableFuture;

public class DiscordMessageContextInteractionEventImpl extends DiscordMessageContextInteractionEvent {

    private final DiscordSRV discordSRV;

    public DiscordMessageContextInteractionEventImpl(
            DiscordSRV discordSRV,
            MessageContextInteractionEvent jdaEvent,
            ComponentIdentifier identifier,
            DiscordUser user,
            DiscordGuildMember member,
            DiscordMessageChannel channel,
            DiscordInteractionHook interaction
    ) {
        super(discordSRV, jdaEvent, identifier, user, member, channel, interaction);
        this.discordSRV = discordSRV;
    }

    @Override
    public CompletableFuture<DiscordInteractionHook> reply(SendableDiscordMessage message, boolean ephemeral) {
        return discordSRV.discordAPI().mapExceptions(
                () -> jdaEvent.reply(SendableDiscordMessageUtil.toJDASend(message)).setEphemeral(ephemeral).submit()
                        .thenApply(ih -> new DiscordInteractionHookImpl(discordSRV, ih))
        );
    }

    @Override
    public CompletableFuture<DiscordInteractionHook> deferReply(boolean ephemeral) {
        return discordSRV.discordAPI().mapExceptions(
                () -> jdaEvent.deferReply(ephemeral).submit()
                        .thenApply(ih -> new DiscordInteractionHookImpl(discordSRV, ih))
        );
    }
}
