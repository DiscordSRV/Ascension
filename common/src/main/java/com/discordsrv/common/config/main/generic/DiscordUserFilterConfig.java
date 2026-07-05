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

package com.discordsrv.common.config.main.generic;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.common.config.configurate.annotation.Order;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A generic config to filter for certain users.
 */
@ConfigSerializable
public class DiscordUserFilterConfig {

    @Comment("Filters to pick users. For ids you can use user ids (including bots), role ids or webhook ids.\n"
            + "If multiple ids are specified for given a filter, all ids must be met. At least one id must be provided for the filter to be active\n"
            + "Users are selected so that at least one of the \"whitelist\" filters matches (if any)\n"
            + "and none of the \"blacklist\" filters match any given user")
    public List<SingleFilter> filters = new ArrayList<>(Collections.singletonList(new SingleFilter(FilterMode.WHITELIST)));

    public DiscordUserFilterConfig() {}

    public final boolean included(@NonNull ReceivedDiscordMessage message) {
        return included(message.isWebhookMessage(), message.getAuthor(), message.getMember());
    }

    public final boolean included(boolean webhookMessages, @NonNull DiscordUser author, @Nullable DiscordGuildMember member) {
        List<Long> roleIds = Collections.emptyList();
        if (member != null) {
            roleIds = member.getRoles().stream()
                    .map(DiscordRole::getId)
                    .collect(Collectors.toList());
        }
        return included(webhookMessages, author.isBot(), author.getId(), roleIds);
    }

    private List<SingleFilter> getValidFilters() {
        List<SingleFilter> validFilters = new ArrayList<>(filters.size());
        for (SingleFilter filter : filters) {
            if (filter.ids.isEmpty()) {
                continue;
            }

            validFilters.add(filter);
        }
        return validFilters;
    }

    protected boolean hasAnyWhitelist() {
        for (SingleFilter filter : getValidFilters()) {
            if (filter.mode.isWhitelist()) {
                return true;
            }
        }
        return false;
    }

    public boolean included(boolean webhookMessage, boolean bot, long userId, List<Long> roleIds) {
        boolean anyWhitelistFilter = hasAnyWhitelist();
        boolean whitelisted = false;

        List<SingleFilter> validFilters = getValidFilters();
        if (validFilters.isEmpty()) {
            // Must have a filter
            return false;
        }

        for (SingleFilter filter : validFilters) {
            List<Long> ids = filter.ids;

            boolean allMatch = true;
            for (Long id : ids) {
                boolean match = userId == id || roleIds.contains(id);
                if (!match) {
                    allMatch = false;
                    break;
                }
            }
            if (!allMatch) {
                // At least one didn't match
                continue;
            }

            if (filter.mode.isWhitelist()) {
                whitelisted = true;
            } else {
                return false;
            }
        }

        return !anyWhitelistFilter || whitelisted;
    }

    @ConfigSerializable
    public static class SingleFilter {
        public List<Long> ids = new ArrayList<>();
        public FilterMode mode = FilterMode.WHITELIST;

        public SingleFilter() {}

        public SingleFilter(FilterMode mode) {
            this(new ArrayList<>(), mode);
        }

        public SingleFilter(long id, FilterMode mode) {
            this(new ArrayList<>(Collections.singletonList(id)), mode);
        }

        public SingleFilter(List<Long> ids, FilterMode mode) {
            this.ids = ids;
            this.mode = mode;
        }
    }

    @ConfigSerializable
    public static class WithBots extends DiscordUserFilterConfig {

        @Comment("\"blacklist\" (exclude all bots) or \"whitelist\" (based on above \"filters\")")
        @Order(1)
        public FilterMode bots = FilterMode.WHITELIST;

        @Comment("\"blacklist\" (exclude all webhooks) or \"whitelist\" (based on above \"filters\")")
        @Order(1)
        public FilterMode webhooks = FilterMode.BLACKLIST;

        @Override
        public boolean included(boolean webhookMessage, boolean bot, long userId, List<Long> roleIds) {
            if (webhookMessage && !webhooks.isWhitelist()) {
                // Webhooks blacklisted
                return false;
            }
            if ((bot && !webhookMessage) && !bots.isWhitelist()) {
                // Bots blacklisted
                return false;
            }

            return super.included(webhookMessage, bot, userId, roleIds);
        }
    }

    /**
     * Must have at least one whitelist.
     */
    @ConfigSerializable
    public static class Strict extends DiscordUserFilterConfig {

        @Override
        public boolean included(boolean webhookMessage, boolean bot, long userId, List<Long> roleIds) {
            if (!hasAnyWhitelist()) {
                return false;
            }

            return super.included(webhookMessage, bot, userId, roleIds);
        }
    }
}
