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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.jetbrains.annotations.Nullable;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.unrelocate.net.draycia.carbon.api.channels.ChannelRegistry;
import com.discordsrv.unrelocate.net.kyori.adventure.key.Key;
import com.discordsrv.unrelocate.net.kyori.adventure.text.Component;

import net.draycia.carbon.api.channels.ChatChannel;
import net.draycia.carbon.api.event.events.CarbonChatEvent;

final class CarbonChatKeyHelper {
    private static final MessageAccessor MESSAGE_ACCESSOR = messageAccessor();

    private CarbonChatKeyHelper() {
    }

    static @Nullable ChatChannel findChannel(net.draycia.carbon.api.channels.ChannelRegistry registry,
            String channelName) {
        ChannelRegistry unrelocatedRegistry = (ChannelRegistry) (Object) registry;

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
        return MESSAGE_ACCESSOR.get(event);
    }

    private static MessageAccessor messageAccessor() {
        MessageAccessor paperComponentAccessor = paperComponentAccessor();
        if (paperComponentAccessor != null) {
            return paperComponentAccessor;
        }

        return minecraftComponentAccessor();
    }

    private static @Nullable MessageAccessor paperComponentAccessor() {
        try {
            Class<?> handleClass = Class.forName("com.discordsrv.bukkit.component.PaperComponentHandle");
            Object handle = handleClass
                    .getMethod("get", Class.class, String.class)
                    .invoke(null, CarbonChatEvent.class, "message");

            MethodHandle getAPI = MethodHandles.publicLookup()
                    .findVirtual(handle.getClass(), "getAPI", MethodType.methodType(MinecraftComponent.class, Object.class))
                    .bindTo(handle);

            return event -> {
                try {
                    return (MinecraftComponent) getAPI.invoke(event);
                } catch (Throwable e) {
                    throw new RuntimeException("Failed to call component method", e);
                }
            };
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static MessageAccessor minecraftComponentAccessor() {
        try {
            MethodHandle message = MethodHandles.lookup().findVirtual(
                    CarbonChatEvent.class,
                    "message",
                    MethodType.methodType(Component.class));

            return event -> {
                try {
                    return MinecraftComponent.fromAdventure((Component) message.invoke(event));
                } catch (Throwable e) {
                    throw new RuntimeException("Failed to call component method", e);
                }
            };
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to get component handle", e);
        }
    }

    private interface MessageAccessor {
        MinecraftComponent get(CarbonChatEvent event);
    }
}
