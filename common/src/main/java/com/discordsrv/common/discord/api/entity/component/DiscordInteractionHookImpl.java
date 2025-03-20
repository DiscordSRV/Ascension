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

package com.discordsrv.common.discord.api.entity.component;

import com.discordsrv.api.discord.entity.interaction.DiscordInteractionHook;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.discord.api.entity.message.ReceivedDiscordMessageImpl;
import com.discordsrv.common.discord.api.entity.message.util.SendableDiscordMessageUtil;
import net.dv8tion.jda.api.interactions.InteractionHook;

public class DiscordInteractionHookImpl implements DiscordInteractionHook {

    private final DiscordSRV discordSRV;
    private final InteractionHook hook;

    public DiscordInteractionHookImpl(DiscordSRV discordSRV, InteractionHook hook) {
        this.discordSRV = discordSRV;
        this.hook = hook;
    }

    @Override
    public InteractionHook asJDA() {
        return hook;
    }

    @Override
    public long getExpiryTime() {
        return hook.getExpirationTimestamp();
    }

    @Override
    public boolean isExpired() {
        return hook.isExpired();
    }

    @Override
    public Task<ReceivedDiscordMessage> editOriginal(SendableDiscordMessage message) {
        return discordSRV.discordAPI().toTask(() -> hook.editOriginal(SendableDiscordMessageUtil.toJDAEdit(message)))
                .thenApply(msg -> ReceivedDiscordMessageImpl.fromJDA(discordSRV, msg));
    }

    @Override
    public Task<ReceivedDiscordMessage> sendMessage(SendableDiscordMessage message, boolean ephemeral) {
        return discordSRV.discordAPI().toTask(() -> hook.sendMessage(SendableDiscordMessageUtil.toJDASend(message)).setEphemeral(ephemeral))
                .thenApply(msg -> ReceivedDiscordMessageImpl.fromJDA(discordSRV, msg));
    }
}
