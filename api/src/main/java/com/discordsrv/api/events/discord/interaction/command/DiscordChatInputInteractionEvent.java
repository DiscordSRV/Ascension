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
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.interaction.DiscordInteractionHook;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public abstract class DiscordChatInputInteractionEvent extends AbstractCommandInteractionEvent<SlashCommandInteractionEvent> {

    public DiscordChatInputInteractionEvent(
            DiscordSRV discordSRV,
            SlashCommandInteractionEvent jdaEvent,
            ComponentIdentifier identifier,
            DiscordUser user,
            DiscordGuildMember member,
            DiscordMessageChannel channel,
            DiscordInteractionHook interaction
    ) {
        super(discordSRV, jdaEvent, identifier, user, member, channel, interaction);
    }
}
