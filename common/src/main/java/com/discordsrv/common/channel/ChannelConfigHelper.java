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
import com.discordsrv.api.discord.api.entity.channel.DiscordTextChannel;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.channel.GameChannelLookupEvent;
import com.discordsrv.api.event.events.lifecycle.DiscordSRVReloadEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.ChannelConfig;
import com.discordsrv.common.function.OrDefault;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ChannelConfigHelper {

    private final DiscordSRV discordSRV;
    private final LoadingCache<String, GameChannel> nameToChannelCache;
    private final Map<String, Pair<String, ChannelConfig>> discordToConfigMap;

    public ChannelConfigHelper(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.nameToChannelCache = discordSRV.caffeineBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .expireAfterAccess(30, TimeUnit.SECONDS)
                .refreshAfterWrite(10, TimeUnit.SECONDS)
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
        this.discordToConfigMap = new ConcurrentHashMap<>();

        discordSRV.eventBus().subscribe(this);
    }

    @Subscribe
    public void onReload(DiscordSRVReloadEvent event) {
        if (!event.isConfig()) {
            return;
        }

        Map<String, Pair<String, ChannelConfig>> newMap = new HashMap<>();
        for (Map.Entry<String, BaseChannelConfig> entry : channels().entrySet()) {
            String channelName = entry.getKey();
            BaseChannelConfig value = entry.getValue();
            if (value instanceof ChannelConfig) {
                ChannelConfig channelConfig = (ChannelConfig) value;
                for (String channelId : channelConfig.channelIds) {
                    newMap.put(channelId, Pair.of(channelName, channelConfig));
                }
            }
        }

        synchronized (discordToConfigMap) {
            discordToConfigMap.clear();
            for (Map.Entry<String, Pair<String, ChannelConfig>> entry : newMap.entrySet()) {
                discordToConfigMap.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private Map<String, BaseChannelConfig> channels() {
        return discordSRV.config().channels;
    }

    public OrDefault<BaseChannelConfig> orDefault(GameChannel gameChannel) {
        return orDefault(gameChannel.getOwnerName(), gameChannel.getChannelName());
    }

    public OrDefault<Pair<GameChannel, BaseChannelConfig>> orDefault(DiscordTextChannel discordTextChannel) {
        return new OrDefault<>(
                getDiscordResolved(discordTextChannel),
                Pair.of(null, getDefault())
        );
    }

    private BaseChannelConfig getDefault() {
        return channels().computeIfAbsent(
                "default", key -> new BaseChannelConfig());
    }

    public OrDefault<BaseChannelConfig> orDefault(String ownerName, String channelName) {
        BaseChannelConfig defaultConfig = getDefault();

        return new OrDefault<>(
                get(ownerName, channelName),
                defaultConfig
        );
    }

    public BaseChannelConfig get(GameChannel gameChannel) {
        return get(gameChannel.getOwnerName(), gameChannel.getChannelName());
    }

    public BaseChannelConfig get(String ownerName, String channelName) {
        if (ownerName != null) {
            BaseChannelConfig config = channels().get(ownerName + ":" + channelName);
            if (config != null) {
                return config;
            }

            GameChannel gameChannel = nameToChannelCache.get(channelName);
            if (gameChannel != null && gameChannel.getOwnerName().equals(ownerName)) {
                config = channels().get(channelName);
                return config;
            }
            return null;
        }

        GameChannel gameChannel = nameToChannelCache.get(channelName);
        return gameChannel != null ? get(gameChannel) : null;
    }

    public Pair<GameChannel, BaseChannelConfig> getDiscordResolved(DiscordTextChannel channel) {
        Pair<String, ? extends BaseChannelConfig> pair = getDiscord(channel);
        if (pair == null) {
            return null;
        }

        GameChannel gameChannel = nameToChannelCache.get(pair.getKey());
        if (gameChannel == null) {
            return null;
        }

        return Pair.of(gameChannel, pair.getValue());
    }

    public Pair<String, ? extends BaseChannelConfig> getDiscord(DiscordTextChannel channel) {
        synchronized (discordToConfigMap) {
            return discordToConfigMap.get(channel.getId());
        }
    }
}
