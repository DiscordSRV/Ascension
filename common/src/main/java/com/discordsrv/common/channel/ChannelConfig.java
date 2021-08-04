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

package com.discordsrv.common.channel;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.event.events.channel.GameChannelLookupEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.ChannelConfigHolder;
import com.discordsrv.common.function.OrDefault;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ChannelConfig {

    private final DiscordSRV discordSRV;
    private final LoadingCache<String, GameChannel> channels;

    public ChannelConfig(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.channels = discordSRV.caffeineBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .build(new CacheLoader<String, GameChannel>() {
                    @Override
                    public @Nullable GameChannel load(@NonNull String channelName) {
                        GameChannelLookupEvent event = new GameChannelLookupEvent(null, channelName);
                        discordSRV.eventBus().publish(event);
                        if (!event.isProcessed()) {
                            return null;
                        }

                        return event.getChannelFromProcessing();
                    }
                });
    }

    private Map<String, ChannelConfigHolder> channels() {
        return discordSRV.config().channels;
    }

    public OrDefault<BaseChannelConfig> orDefault(GameChannel gameChannel) {
        return orDefault(gameChannel.getOwnerName(), gameChannel.getChannelName());
    }

    public OrDefault<BaseChannelConfig> orDefault(String ownerName, String channelName) {
        ChannelConfigHolder defaultConfig = channels().computeIfAbsent(
                "default", key -> new ChannelConfigHolder(new BaseChannelConfig()));

        return new OrDefault<>(
                get(ownerName, channelName),
                defaultConfig.get()
        );
    }

    public BaseChannelConfig get(GameChannel gameChannel) {
        return get(gameChannel.getOwnerName(), gameChannel.getChannelName());
    }

    public BaseChannelConfig get(String ownerName, String channelName) {
        if (ownerName != null) {
            ChannelConfigHolder config = channels().get(ownerName + ":" + channelName);
            if (config != null) {
                return config.get();
            }

            GameChannel gameChannel = channels.get(channelName);
            if (gameChannel != null && gameChannel.getOwnerName().equals(ownerName)) {
                config = channels().get(channelName);
                return config != null ? config.get() : null;
            }
            return null;
        }

        GameChannel gameChannel = channels.get(channelName);
        return gameChannel != null ? get(gameChannel) : null;
    }
}
