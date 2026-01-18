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

package com.discordsrv.common.feature.mention.cache;

import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.MinecraftToDiscordChatConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.config.main.generic.DestinationConfig;
import com.discordsrv.common.feature.mention.Mention;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class MentionCache<T extends ISnowflake> {

    private final Map<Long, Map<Long, Mention>> cache = new ConcurrentHashMap<>();

    private final DiscordSRV discordSRV;
    private final Predicate<MinecraftToDiscordChatConfig.Mentions> typeEnabledPredicate;
    private final Function<T, Guild> guildSupplier;
    private final Function<Guild, List<T>> loadCache;
    private final Function<T, Mention> convert;

    public MentionCache(
            DiscordSRV discordSRV,
            Predicate<MinecraftToDiscordChatConfig.Mentions> typeEnabledPredicate,
            Function<T, Guild> guildSupplier,
            Function<Guild, List<T>> loadCache,
            Function<T, Mention> convert
    ) {
        this.discordSRV = discordSRV;
        this.typeEnabledPredicate = typeEnabledPredicate;
        this.guildSupplier = guildSupplier;
        this.loadCache = loadCache;
        this.convert = convert;
    }

    public void clear() {
        this.cache.clear();
    }

    public void checkNoLongerNeededCaches() {
        for (Long guildId : new HashSet<>(cache.keySet())) {
            if (shouldNotCache(guildId)) {
                cache.remove(guildId);
            }
        }
    }

    public boolean shouldNotCache(long guildId) {
        DiscordGuild guild = discordSRV.discordAPI().getGuildById(guildId);
        if (guild == null) {
            return false;
        }

        for (BaseChannelConfig config : discordSRV.channelConfig().getAllChannels()) {
            if (!(config instanceof IChannelConfig)) {
                continue;
            }

            DestinationConfig destination = ((IChannelConfig) config).destination();
            if (!destination.contains(guild)) {
                continue;
            }

            MinecraftToDiscordChatConfig chatConfig = config.minecraftToDiscord;
            if (!chatConfig.enabled) {
                continue;
            }

            if (typeEnabledPredicate.test(chatConfig.mentions)) {
                return false;
            }
        }
        return true;
    }

    public Mention convert(T entity) {
        return convert.apply(entity);
    }

    public void removeGuild(long guildId) {
        cache.remove(guildId);
    }

    public Map<Long, Mention> getGuildCache(Guild guild) {
        return getOrCreateGuildCache(guild);
    }

    public Mention get(Guild guild, long entityId) {
        return getGuildCache(guild).get(entityId);
    }

    private Map<Long, Mention> getOrCreateGuildCache(Guild guild) {
        return cache.computeIfAbsent(guild.getIdLong(), key -> {
            Map<Long, Mention> mentions = new LinkedHashMap<>();
            for (T entity : loadCache.apply(guild)) {
                mentions.put(entity.getIdLong(), convert(entity));
            }
            return mentions;
        });
    }

    private void alterCache(T entity, Consumer<Map<Long, Mention>> guildCacheAlterer) {
        Guild guild = guildSupplier.apply(entity);
        if (shouldNotCache(guild.getIdLong())) {
            return;
        }

        guildCacheAlterer.accept(getOrCreateGuildCache(guild));
    }

    public void addOrUpdate(T entity) {
        alterCache(entity, cache -> cache.put(entity.getIdLong(), convert.apply(entity)));
    }

    public void remove(Guild guild, long entityId) {
        Map<Long, Mention> guildCache = cache.get(guild.getIdLong());
        if (guildCache == null) {
            return;
        }

        guildCache.remove(entityId);
    }
}
