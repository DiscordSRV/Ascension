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

package com.discordsrv.common.integration.chat;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.unrelocate.net.kyori.adventure.key.Key;
import net.draycia.carbon.api.channels.ChatChannel;
import net.draycia.carbon.api.event.events.CarbonChatEvent;
import org.jetbrains.annotations.Nullable;

final class CarbonChatKeyHelper {
    private CarbonChatKeyHelper() {
    }

    static @Nullable ChatChannel findChannel(net.draycia.carbon.api.channels.ChannelRegistry registry,
            String channelName) {
        com.discordsrv.unrelocate.net.draycia.carbon.api.channels.ChannelRegistry unrelocatedRegistry = (com.discordsrv.unrelocate.net.draycia.carbon.api.channels.ChannelRegistry) (Object) registry;

        for (Key key : unrelocatedRegistry.keys()) {
            ChatChannel channel = (ChatChannel) (Object) unrelocatedRegistry.channel(key);
            if (channel == null) {
                continue;
            }

            if (key.asString().equalsIgnoreCase(channelName)) {
                return channel;
            }
        }

        for (Key key : unrelocatedRegistry.keys()) {
            ChatChannel channel = (ChatChannel) (Object) unrelocatedRegistry.channel(key);
            if (channel == null) {
                continue;
            }

            if (key.value().equalsIgnoreCase(channelName)) {
                return channel;
            }
        }

        for (Key key : unrelocatedRegistry.keys()) {
            ChatChannel channel = (ChatChannel) (Object) unrelocatedRegistry.channel(key);
            if (channel == null || channel.commandName() == null) {
                continue;
            }

            if (channel.commandName().equalsIgnoreCase(channelName)) {
                return channel;
            }
        }

        return null;
    }

    static String channelName(ChatChannel channel) {
        return ((com.discordsrv.unrelocate.net.draycia.carbon.api.channels.ChatChannel) (Object) channel).key().value();
    }

    static MinecraftComponent message(CarbonChatEvent event) {
        return MinecraftComponent.fromAdventure(
                ((com.discordsrv.unrelocate.net.draycia.carbon.api.event.events.CarbonChatEvent) (Object) event)
                        .message());
    }
}
