/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.discord.details;

import com.discordsrv.api.discord.connection.DiscordConnectionDetails;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.exception.util.ExceptionUtil;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DiscordConnectionDetailsImpl implements DiscordConnectionDetails {

    private final DiscordSRV discordSRV;
    private final Set<GatewayIntent> gatewayIntents = new HashSet<>();
    private final Set<CacheFlag> cacheFlags = new HashSet<>();

    public DiscordConnectionDetailsImpl(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public boolean readyToTakeDetails() {
        return discordSRV.discordConnectionManager().areDetailsAccepted();
    }

    private void check() {
        if (!readyToTakeDetails()) {
            throw new IllegalStateException("Too late. Please use DiscordConnectionDetails#readyToTakeDetails " +
                    "to check if the method can be used");
        }
    }

    @Override
    public @NotNull Set<GatewayIntent> getGatewayIntents() {
        return gatewayIntents;
    }

    @Override
    public void requestGatewayIntent(@NotNull GatewayIntent gatewayIntent, GatewayIntent... gatewayIntents) {
        check();

        List<GatewayIntent> intents = new ArrayList<>(Collections.singleton(gatewayIntent));
        intents.addAll(Arrays.asList(gatewayIntents));

        this.gatewayIntents.addAll(intents);
    }

    @Override
    public @NotNull Set<CacheFlag> getCacheFlags() {
        return cacheFlags;
    }

    @Override
    public void requestCacheFlag(@NotNull CacheFlag cacheFlag, CacheFlag... cacheFlags) {
        check();

        List<CacheFlag> flags = new ArrayList<>(Collections.singleton(cacheFlag));
        flags.addAll(Arrays.asList(cacheFlags));

        List<Throwable> suppressed = new ArrayList<>();
        for (CacheFlag flag : flags) {
            GatewayIntent requiredIntent = flag.getRequiredIntent();
            if (requiredIntent != null && !gatewayIntents.contains(requiredIntent)) {
                suppressed.add(ExceptionUtil.minifyException(new IllegalArgumentException("CacheFlag "
                        + flag.getRequiredIntent().name() + " requires GatewayIntent " + requiredIntent.name())));
            }
        }

        if (!suppressed.isEmpty()) {
            IllegalArgumentException exception = new IllegalArgumentException();
            suppressed.forEach(exception::addSuppressed);
            throw exception;
        }

        this.cacheFlags.addAll(flags);
    }
}
