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

package com.discordsrv.common.events.discord.interaction.component;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.interaction.DiscordInteractionHook;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.events.discord.interaction.component.DiscordButtonInteractionEvent;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.discord.api.entity.component.DiscordInteractionHookImpl;
import com.discordsrv.common.discord.api.entity.message.util.SendableDiscordMessageUtil;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class DiscordButtonInteractionEventImpl extends DiscordButtonInteractionEvent {

    private final DiscordSRV discordSRV;

    public DiscordButtonInteractionEventImpl(
            DiscordSRV discordSRV,
            ButtonInteractionEvent jdaEvent,
            ComponentIdentifier identifier,
            DiscordUser user,
            DiscordGuildMember member,
            DiscordMessageChannel channel,
            DiscordInteractionHook hook
    ) {
        super(jdaEvent, identifier, user, member, channel, hook);
        this.discordSRV = discordSRV;
    }

    @Override
    public Task<DiscordInteractionHook> sendMessage(SendableDiscordMessage message, boolean ephemeral) {
        return discordSRV.discordAPI().toTask(() -> jdaEvent.reply(SendableDiscordMessageUtil.toJDASend(message)).setEphemeral(ephemeral))
                .thenApply(interactionHook -> new DiscordInteractionHookImpl(discordSRV, interactionHook));
    }

    @Override
    public Task<DiscordInteractionHook> editMessage(SendableDiscordMessage message) {
        return discordSRV.discordAPI().toTask(() -> jdaEvent.editMessage(SendableDiscordMessageUtil.toJDAEdit(message)))
                .thenApply(interactionHook -> new DiscordInteractionHookImpl(discordSRV, interactionHook));
    }

    @Override
    public Task<DiscordInteractionHook> deferReply() {
        return discordSRV.discordAPI().toTask(jdaEvent::deferReply)
                .thenApply(interactionHook -> new DiscordInteractionHookImpl(discordSRV, interactionHook));
    }

    @Override
    public Task<DiscordInteractionHook> deferEdit() {
        return discordSRV.discordAPI().toTask(jdaEvent::deferEdit)
                .thenApply(interactionHook -> new DiscordInteractionHookImpl(discordSRV, interactionHook));
    }
}
