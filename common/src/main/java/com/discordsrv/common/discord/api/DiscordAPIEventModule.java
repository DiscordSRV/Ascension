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

package com.discordsrv.common.discord.api;

import com.discordsrv.api.discord.events.DiscordMessageDeleteEvent;
import com.discordsrv.api.discord.events.DiscordMessageReceiveEvent;
import com.discordsrv.api.discord.events.DiscordMessageUpdateEvent;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.discord.api.entity.channel.DiscordMessageChannelImpl;
import com.discordsrv.common.discord.api.entity.message.ReceivedDiscordMessageImpl;
import com.discordsrv.common.module.type.AbstractModule;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;

public class DiscordAPIEventModule extends AbstractModule<DiscordSRV> {

    public DiscordAPIEventModule(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Subscribe
    public void onMessageReceived(MessageReceivedEvent event) {
        discordSRV.eventBus().publish(new DiscordMessageReceiveEvent(
                DiscordMessageChannelImpl.get(discordSRV, event.getChannel()),
                ReceivedDiscordMessageImpl.fromJDA(discordSRV, event.getMessage())
        ));
    }

    @Subscribe
    public void onMessageUpdate(MessageUpdateEvent event) {
        discordSRV.eventBus().publish(new DiscordMessageUpdateEvent(
                DiscordMessageChannelImpl.get(discordSRV, event.getChannel()),
                ReceivedDiscordMessageImpl.fromJDA(discordSRV, event.getMessage())
        ));
    }

    @Subscribe
    public void onMessageDelete(MessageDeleteEvent event) {
        discordSRV.eventBus().publish(new DiscordMessageDeleteEvent(
                DiscordMessageChannelImpl.get(discordSRV, event.getChannel()),
                event.getMessageIdLong()
        ));
    }
}
