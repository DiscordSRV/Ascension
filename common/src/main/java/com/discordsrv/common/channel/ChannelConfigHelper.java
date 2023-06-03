/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.channel.DiscordTextChannel;
import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;
import com.discordsrv.api.event.events.channel.GameChannelLookupEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.ChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.config.main.channels.base.ThreadConfig;
import com.discordsrv.common.config.manager.MainConfigManager;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class ChannelConfigHelper {

    private final DiscordSRV discordSRV;

    // game channel name eg. "global" -> game channel ("discordsrv:global")
    private final LoadingCache<String, GameChannel> nameToChannelCache;

    // game channel name -> config
    private final Map<String, BaseChannelConfig> configs;

    // caches for Discord channel -> config
    private final Map<Long, Map<String, BaseChannelConfig>> textChannelToConfigMap;
    private final Map<Pair<Long, String>, Map<String, BaseChannelConfig>> threadToConfigMap;

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
        this.configs = new HashMap<>();
        this.textChannelToConfigMap = new HashMap<>();
        this.threadToConfigMap = new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private BaseChannelConfig map(BaseChannelConfig defaultConfig, BaseChannelConfig config)
            throws SerializationException {
        MainConfigManager<?> configManager = discordSRV.configManager();

        CommentedConfigurationNode defaultNode = CommentedConfigurationNode.root(configManager.configNodeOptions());
        CommentedConfigurationNode target = CommentedConfigurationNode.root(configManager.configNodeOptions());

        configManager.configObjectMapper()
                .get((Class<BaseChannelConfig>) defaultConfig.getClass())
                .save(defaultConfig, defaultNode);

        ObjectMapper<BaseChannelConfig> mapper = configManager.configObjectMapper()
                .get((Class<BaseChannelConfig>) config.getClass());

        mapper.save(config, target);
        target.mergeFrom(defaultNode);

        return mapper.load(target);
    }

    public void reload() throws SerializationException {
        Map<String, BaseChannelConfig> configChannels = discordSRV.config().channels;
        BaseChannelConfig defaultConfig = configChannels.computeIfAbsent(ChannelConfig.DEFAULT_KEY, key -> discordSRV.config().createDefaultBaseChannel());

        Map<String, BaseChannelConfig> configs = new HashMap<>();
        for (Map.Entry<String, BaseChannelConfig> entry : configChannels.entrySet()) {
            if (Objects.equals(entry.getKey(), ChannelConfig.DEFAULT_KEY)) {
                continue;
            }

            BaseChannelConfig mapped = map(defaultConfig, entry.getValue());
            configs.put(entry.getKey(), mapped);
        }

        synchronized (this.configs) {
            this.configs.clear();
            this.configs.putAll(configs);
        }

        Map<Long, Map<String, BaseChannelConfig>> text = new HashMap<>();
        Map<Pair<Long, String>, Map<String, BaseChannelConfig>> thread = new HashMap<>();

        for (Map.Entry<String, BaseChannelConfig> entry : channels().entrySet()) {
            String channelName = entry.getKey();
            BaseChannelConfig value = entry.getValue();
            if (value instanceof IChannelConfig) {
                IChannelConfig channelConfig = (IChannelConfig) value;

                List<Long> channelIds = channelConfig.channelIds();
                if (channelIds != null) {
                    for (long channelId : channelIds) {
                        text.computeIfAbsent(channelId, key -> new LinkedHashMap<>())
                                .put(channelName, value);
                    }
                }

                List<ThreadConfig> threads = channelConfig.threads();
                if (threads != null) {
                    for (ThreadConfig threadConfig : threads) {
                        Pair<Long, String> pair = Pair.of(
                                threadConfig.channelId,
                                threadConfig.threadName.toLowerCase(Locale.ROOT)
                        );
                        thread.computeIfAbsent(pair, key -> new LinkedHashMap<>())
                                .put(channelName, value);
                    }
                }
            }
        }

        synchronized (textChannelToConfigMap) {
            textChannelToConfigMap.clear();
            textChannelToConfigMap.putAll(text);
        }
        synchronized (threadToConfigMap) {
            threadToConfigMap.clear();
            threadToConfigMap.putAll(thread);
        }
    }

    private Map<String, BaseChannelConfig> channels() {
        synchronized (configs) {
            return configs;
        }
    }

    private BaseChannelConfig findChannel(String key) {
        Map<String, BaseChannelConfig> channels = channels();
        BaseChannelConfig byExact = channels.get(key);
        if (byExact != null) {
            return byExact;
        }

        for (Map.Entry<String, BaseChannelConfig> entry : channels.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public Set<String> getKeys() {
        Set<String> keys = new LinkedHashSet<>(channels().keySet());
        keys.remove(ChannelConfig.DEFAULT_KEY);
        return keys;
    }

    public Set<BaseChannelConfig> getAllChannels() {
        Set<BaseChannelConfig> channelConfigs = new HashSet<>();
        for (Map.Entry<String, BaseChannelConfig> entry : channels().entrySet()) {
            if (entry.getKey().equals(ChannelConfig.DEFAULT_KEY)) {
                continue;
            }
            channelConfigs.add(entry.getValue());
        }
        return channelConfigs;
    }

    public BaseChannelConfig get(GameChannel gameChannel) {
        return resolve(gameChannel.getOwnerName(), gameChannel.getChannelName());
    }

    public BaseChannelConfig resolve(String ownerName, String channelName) {
        if (ownerName != null) {
            ownerName = ownerName.toLowerCase(Locale.ROOT);

            // Check if there is a channel defined like this: "owner:channel"
            BaseChannelConfig config = findChannel(ownerName + ":" + channelName);
            if (config != null) {
                return config;
            }

            // Check if this owner has the highest priority for this channel name
            GameChannel gameChannel = nameToChannelCache.get(channelName);
            if (gameChannel != null && gameChannel.getOwnerName().equalsIgnoreCase(ownerName)) {
                config = findChannel(channelName);
                return config;
            }
            return null;
        }

        // Get the highest priority owner for this channel name and relookup
        GameChannel gameChannel = nameToChannelCache.get(channelName);
        return gameChannel != null ? get(gameChannel) : null;
    }

    public Map<GameChannel, BaseChannelConfig> resolve(DiscordMessageChannel channel) {
        Map<String, BaseChannelConfig> pairs = get(channel);
        if (pairs == null || pairs.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<GameChannel, BaseChannelConfig> channels = new LinkedHashMap<>();
        for (Map.Entry<String, BaseChannelConfig> entry : pairs.entrySet()) {
            GameChannel gameChannel = nameToChannelCache.get(entry.getKey());
            if (gameChannel == null) {
                continue;
            }

            channels.put(gameChannel, entry.getValue());
        }

        return channels;
    }

    private Map<String, BaseChannelConfig> get(DiscordMessageChannel channel) {
        Map<String, BaseChannelConfig> pairs = null;
        if (channel instanceof DiscordTextChannel) {
            pairs = getByTextChannel((DiscordTextChannel) channel);
        } else if (channel instanceof DiscordThreadChannel) {
            pairs = getByThreadChannel((DiscordThreadChannel) channel);
        }

        return pairs;
    }

    private Map<String, BaseChannelConfig> getByTextChannel(DiscordTextChannel channel) {
        synchronized (textChannelToConfigMap) {
            return textChannelToConfigMap.get(channel.getId());
        }
    }

    private Map<String, BaseChannelConfig> getByThreadChannel(DiscordThreadChannel channel) {
        Pair<Long, String> pair = Pair.of(
                channel.getParentChannel().getId(),
                channel.getName().toLowerCase(Locale.ROOT)
        );
        synchronized (threadToConfigMap) {
            return threadToConfigMap.get(pair);
        }
    }
}
