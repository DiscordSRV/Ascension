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

package com.discordsrv.common.helper;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;
import com.discordsrv.api.events.channel.GameChannelLookupEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.configurate.manager.MainConfigManager;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.ChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.config.main.generic.DestinationConfig;
import com.discordsrv.common.config.main.generic.ThreadConfig;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class ChannelConfigHelper {

    private final DiscordSRV discordSRV;
    private final Logger logger;

    // game channel name eg. "global" -> game channel ("discordsrv:global")
    private final LoadingCache<String, GameChannel> nameToChannelCache;

    // game channel name -> config
    private final Map<String, BaseChannelConfig> configs;

    // caches for Discord channel -> config
    private final Map<Long, Map<String, BaseChannelConfig>> messageChannelToConfigMap;
    private final Map<Pair<Long, String>, Map<String, BaseChannelConfig>> threadToConfigMap;

    public ChannelConfigHelper(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "CHANNEL_CONFIG_HELPER");

        this.nameToChannelCache = discordSRV.caffeineBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .expireAfterAccess(30, TimeUnit.SECONDS)
                .refreshAfterWrite(10, TimeUnit.SECONDS)
                .build(new CacheLoader<String, GameChannel>() {

                    @Override
                    public @Nullable GameChannel load(@NotNull String channelAtom) {
                        Pair<String, String> channelPair = parseOwnerAndChannel(channelAtom);

                        GameChannelLookupEvent event = new GameChannelLookupEvent(channelPair.getKey(), channelPair.getValue());
                        discordSRV.eventBus().publish(event);
                        if (!event.isProcessed()) {
                            return null;
                        }

                        GameChannel channel = event.getChannelFromProcessing();
                        logger.trace(channelAtom + " looked up to " + GameChannel.toString(channel));
                        return channel;
                    }
                });
        this.configs = new HashMap<>();
        this.messageChannelToConfigMap = new HashMap<>();
        this.threadToConfigMap = new LinkedHashMap<>();
    }

    private Pair<String, String> parseOwnerAndChannel(String channelAtom) {
        String[] split = channelAtom.split(":", 2);
        String channelName = split[split.length - 1];
        String ownerName = split.length == 2 ? split[0] : null;

        return Pair.of(ownerName, channelName);
    }

    @SuppressWarnings("unchecked")
    private BaseChannelConfig map(BaseChannelConfig defaultConfig, BaseChannelConfig config)
            throws SerializationException {
        MainConfigManager<?> configManager = discordSRV.configManager();

        CommentedConfigurationNode defaultNode = CommentedConfigurationNode.root(configManager.nodeOptions(true));
        CommentedConfigurationNode target = CommentedConfigurationNode.root(configManager.nodeOptions(true));

        configManager.objectMapper()
                .get((Class<BaseChannelConfig>) defaultConfig.getClass())
                .save(defaultConfig, defaultNode);

        ObjectMapper<BaseChannelConfig> mapper = configManager.objectMapper()
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

        Map<Long, Map<String, BaseChannelConfig>> messageChannel = new HashMap<>();
        Map<Pair<Long, String>, Map<String, BaseChannelConfig>> thread = new HashMap<>();

        for (Map.Entry<String, BaseChannelConfig> entry : channels().entrySet()) {
            String channelName = entry.getKey();
            BaseChannelConfig value = entry.getValue();
            if (value instanceof IChannelConfig) {
                DestinationConfig destination = ((IChannelConfig) value).destination();

                List<Long> channelIds = destination.channelIds;
                if (channelIds != null) {
                    for (long channelId : channelIds) {
                        messageChannel.computeIfAbsent(channelId, key -> new LinkedHashMap<>())
                                .put(channelName, value);
                    }
                }

                List<ThreadConfig> threads = destination.threads;
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

        synchronized (messageChannelToConfigMap) {
            messageChannelToConfigMap.clear();
            messageChannelToConfigMap.putAll(messageChannel);
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

    @Nullable
    public BaseChannelConfig get(@NotNull GameChannel gameChannel) {
        return resolve(gameChannel.getOwnerName(), gameChannel.getChannelName());
    }

    @Nullable
    public BaseChannelConfig resolve(@NotNull String channelAtom) {
        Pair<String, String> channelPair = parseOwnerAndChannel(channelAtom);
        return resolve(channelPair.getKey(), channelPair.getValue());
    }

    @Nullable
    public BaseChannelConfig resolve(@Nullable String ownerName, @NotNull String channelName) {
        if (ownerName != null) {
            ownerName = ownerName.toLowerCase(Locale.ROOT);

            // Check if there is a channel defined like this: "owner:channel"
            BaseChannelConfig config = findChannel(ownerName + ":" + channelName);
            if (config != null) {
                return config;
            }

            // Check if this owner has the highest priority for this channel name
            // in case they are, we can also use "channel" config directly
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

    @NotNull
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
        if (channel instanceof DiscordThreadChannel) {
            return getByThreadChannel((DiscordThreadChannel) channel);
        }

        return getByMessageChannel(channel);
    }

    private Map<String, BaseChannelConfig> getByMessageChannel(DiscordMessageChannel channel) {
        synchronized (messageChannelToConfigMap) {
            return messageChannelToConfigMap.get(channel.getId());
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
