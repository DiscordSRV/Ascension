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

package com.discordsrv.common.linking.requirelinking.requirement;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.function.CheckedSupplier;
import me.minecraftauth.lib.AuthService;
import me.minecraftauth.lib.account.platform.twitch.SubTier;
import me.minecraftauth.lib.exception.LookupException;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class MinecraftAuthRequirement<T> implements Requirement<MinecraftAuthRequirement.Reference<T>> {

    private static final Reference<?> NULL_VALUE = new Reference<>(null);

    public static List<Requirement<?>> createRequirements(DiscordSRV discordSRV) {
        List<Requirement<?>> requirements = new ArrayList<>();

        // Patreon
        requirements.add(new MinecraftAuthRequirement<>(
                discordSRV,
                Type.PATREON,
                "PatreonSubscriber",
                AuthService::isSubscribedPatreon,
                AuthService::isSubscribedPatreon
        ));

        // Glimpse
        requirements.add(new MinecraftAuthRequirement<>(
                discordSRV,
                Type.GLIMPSE,
                "GlimpseSubscriber",
                AuthService::isSubscribedGlimpse,
                AuthService::isSubscribedGlimpse
        ));

        // Twitch
        requirements.add(new MinecraftAuthRequirement<>(
                discordSRV,
                Type.TWITCH,
                "TwitchFollower",
                AuthService::isFollowingTwitch
        ));
        requirements.add(new MinecraftAuthRequirement<>(
                discordSRV,
                Type.TWITCH,
                "TwitchSubscriber",
                AuthService::isSubscribedTwitch,
                AuthService::isSubscribedTwitch,
                string -> {
                    try {
                        int value = Integer.parseInt(string);
                        return SubTier.level(value);
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                }
        ));

        // YouTube
        requirements.add(new MinecraftAuthRequirement<>(
                discordSRV,
                Type.YOUTUBE,
                "YouTubeSubscriber",
                AuthService::isSubscribedYouTube
        ));
        requirements.add(new MinecraftAuthRequirement<>(
                discordSRV,
                Type.YOUTUBE,
                "YouTubeMember",
                AuthService::isMemberYouTube,
                AuthService::isMemberYouTube
        ));

        return requirements;
    }

    private final DiscordSRV discordSRV;
    private final Type type;
    private final String name;
    private final Test test;
    private final TestSpecific<T> testSpecific;
    private final Function<String, T> parse;

    public MinecraftAuthRequirement(
            DiscordSRV discordSRV,
            Type type,
            String name,
            Test test
    ) {
        this(discordSRV, type, name, test, null, null);
    }

    @SuppressWarnings("unchecked")
    public MinecraftAuthRequirement(
            DiscordSRV discordSRV,
            Type type,
            String name,
            Test test,
            TestSpecific<String> testSpecific
    ) {
        this(discordSRV, type, name, test, (TestSpecific<T>) testSpecific, t -> (T) t);
    }

    public MinecraftAuthRequirement(
            DiscordSRV discordSRV,
            Type type,
            String name,
            Test test,
            TestSpecific<T> testSpecific,
            Function<String, T> parse
    ) {
        this.discordSRV = discordSRV;
        this.type = type;
        this.name = name;
        this.test = test;
        this.testSpecific = testSpecific;
        this.parse = parse;
    }

    @Override
    public String name() {
        return name;
    }

    public Type getType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Reference<T> parse(String input) {
        if (StringUtils.isEmpty(input)) {
            return (Reference<T>) NULL_VALUE;
        } else if (parse != null) {
            return new Reference<>(parse.apply(input));
        } else {
            return null;
        }
    }

    @Override
    public CompletableFuture<Boolean> isMet(Reference<T> atomicReference, UUID player, long userId) {
        String token = discordSRV.connectionConfig().minecraftAuth.token;
        T value = atomicReference.getValue();
        if (value == null) {
            return supply(() -> test.test(token, player));
        } else {
            return supply(() -> testSpecific.test(token, player, value));
        }
    }

    private CompletableFuture<Boolean> supply(CheckedSupplier<Boolean> provider) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        discordSRV.scheduler().run(() -> {
            try {
                completableFuture.complete(provider.get());
            } catch (Throwable t) {
                completableFuture.completeExceptionally(t);
            }
        });
        return completableFuture;
    }

    @FunctionalInterface
    public interface Test {
        boolean test(String serverToken, UUID player) throws LookupException;
    }

    @FunctionalInterface
    public interface TestSpecific<T> {
        boolean test(String serverToken, UUID uuid, T specific) throws LookupException;
    }

    public static class Reference<T> {

        private final T value;

        public Reference(T value) {
            this.value = value;
        }

        public T getValue() {
            return value;
        }
    }

    public enum Type {
        PATREON('p'),
        GLIMPSE('g'),
        TWITCH('t'),
        YOUTUBE('y');

        private final char character;

        Type(char character) {
            this.character = character;
        }

        public char character() {
            return character;
        }
    }
}
