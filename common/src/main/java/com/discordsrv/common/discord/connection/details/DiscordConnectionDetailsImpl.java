/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.discord.connection.details;

import com.discordsrv.api.discord.connection.details.DiscordCacheFlag;
import com.discordsrv.api.discord.connection.details.DiscordConnectionDetails;
import com.discordsrv.api.discord.connection.details.DiscordGatewayIntent;
import com.discordsrv.api.discord.connection.details.DiscordMemberCachePolicy;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.exception.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DiscordConnectionDetailsImpl implements DiscordConnectionDetails {

    private final DiscordSRV discordSRV;
    private final Set<DiscordGatewayIntent> gatewayIntents = new HashSet<>();
    private final Set<DiscordCacheFlag> cacheFlags = new HashSet<>();
    private final Set<DiscordMemberCachePolicy> memberCachePolicies = new HashSet<>();

    public DiscordConnectionDetailsImpl(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.memberCachePolicies.add(DiscordMemberCachePolicy.OWNER);
    }

    private boolean isStatus() {
        return discordSRV.status() == DiscordSRV.Status.INITIALIZED
                || discordSRV.status() == DiscordSRV.Status.ATTEMPTING_TO_CONNECT;
    }

    public @NotNull Set<DiscordGatewayIntent> getGatewayIntents() {
        Set<DiscordGatewayIntent> intents = new HashSet<>(gatewayIntents);
        intents.addAll(discordSRV.moduleManager().requiredIntents());
        return intents;
    }

    @Override
    public boolean requestGatewayIntent(@NotNull DiscordGatewayIntent gatewayIntent, DiscordGatewayIntent... gatewayIntents) {
        List<DiscordGatewayIntent> intents = new ArrayList<>(Collections.singleton(gatewayIntent));
        intents.addAll(Arrays.asList(gatewayIntents));

        this.gatewayIntents.addAll(intents);
        return isStatus();
    }

    public @NotNull Set<DiscordCacheFlag> getCacheFlags() {
        Set<DiscordCacheFlag> flags = new HashSet<>(cacheFlags);
        flags.addAll(discordSRV.moduleManager().requiredCacheFlags());
        return flags;
    }

    @Override
    public boolean requestCacheFlag(@NotNull DiscordCacheFlag cacheFlag, DiscordCacheFlag... cacheFlags) {
        List<DiscordCacheFlag> flags = new ArrayList<>(Collections.singleton(cacheFlag));
        flags.addAll(Arrays.asList(cacheFlags));

        List<Throwable> suppressed = new ArrayList<>();
        for (DiscordCacheFlag flag : flags) {
            DiscordGatewayIntent requiredIntent = flag.requiredIntent();
            if (requiredIntent != null && !gatewayIntents.contains(requiredIntent)) {
                suppressed.add(ExceptionUtil.minifyException(new IllegalArgumentException("CacheFlag "
                        + requiredIntent.name() + " requires GatewayIntent " + requiredIntent.name())));
            }
        }

        if (!suppressed.isEmpty()) {
            IllegalArgumentException exception = new IllegalArgumentException();
            suppressed.forEach(exception::addSuppressed);
            throw exception;
        }

        this.cacheFlags.addAll(flags);
        return isStatus();
    }

    public @NotNull Set<DiscordMemberCachePolicy> getMemberCachePolicies() {
        Set<DiscordMemberCachePolicy> policies = new HashSet<>(memberCachePolicies);
        policies.addAll(discordSRV.moduleManager().requiredMemberCachePolicies());
        return policies;
    }

    @Override
    public boolean requestMemberCachePolicy(@NotNull DiscordMemberCachePolicy memberCachePolicy, @NotNull DiscordMemberCachePolicy... memberCachePolicies) {
        List<DiscordMemberCachePolicy> policies = new ArrayList<>(Collections.singleton(memberCachePolicy));
        policies.addAll(Arrays.asList(memberCachePolicies));

        this.memberCachePolicies.addAll(policies);
        return isStatus();
    }
}
