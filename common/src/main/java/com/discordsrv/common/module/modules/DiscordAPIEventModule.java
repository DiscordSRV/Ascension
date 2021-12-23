/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.module.modules;

import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.discord.events.DiscordMessageReceivedEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.discord.api.channel.DiscordMessageChannelImpl;
import com.discordsrv.common.discord.api.message.ReceivedDiscordMessageImpl;
import com.discordsrv.common.module.type.AbstractModule;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class DiscordAPIEventModule extends AbstractModule {

    public DiscordAPIEventModule(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Subscribe
    public void onMessageReceivedEvent(MessageReceivedEvent event) {
        discordSRV.eventBus().publish(new DiscordMessageReceivedEvent(
                ReceivedDiscordMessageImpl.fromJDA(discordSRV, event.getMessage()),
                DiscordMessageChannelImpl.get(discordSRV, event.getChannel()))
        );
    }
}
