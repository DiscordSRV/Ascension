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

package com.discordsrv.common.feature.linking.requirelinking.requirement.type;

import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.feature.linking.requirelinking.RequiredLinkingModule;
import com.discordsrv.common.feature.linking.requirelinking.requirement.RequirementType;
import com.discordsrv.common.helper.Someone;
import me.minecraftauth.lib.AuthService;
import me.minecraftauth.lib.account.platform.twitch.SubTier;
import me.minecraftauth.lib.exception.LookupException;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class MinecraftAuthRequirementType<T> extends RequirementType<MinecraftAuthRequirementType.Reference<T>> {

    private static final Reference<?> NULL_VALUE = new Reference<>(null);

    public static List<RequirementType<?>> createRequirements(RequiredLinkingModule<?> module) {
        List<RequirementType<?>> requirementTypes = new ArrayList<>();

        // Patreon
        requirementTypes.add(new MinecraftAuthRequirementType<>(
                module,
                Provider.PATREON,
                "PatreonSubscriber",
                AuthService::isSubscribedPatreon,
                AuthService::isSubscribedPatreon
        ));

        // Glimpse
        requirementTypes.add(new MinecraftAuthRequirementType<>(
                module,
                Provider.GLIMPSE,
                "GlimpseSubscriber",
                AuthService::isSubscribedGlimpse,
                AuthService::isSubscribedGlimpse
        ));

        // Twitch
        requirementTypes.add(new MinecraftAuthRequirementType<>(
                module,
                Provider.TWITCH,
                "TwitchFollower",
                AuthService::isFollowingTwitch
        ));
        requirementTypes.add(new MinecraftAuthRequirementType<>(
                module,
                Provider.TWITCH,
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
        requirementTypes.add(new MinecraftAuthRequirementType<>(
                module,
                Provider.YOUTUBE,
                "YouTubeSubscriber",
                AuthService::isSubscribedYouTube
        ));
        requirementTypes.add(new MinecraftAuthRequirementType<>(
                module,
                Provider.YOUTUBE,
                "YouTubeMember",
                AuthService::isMemberYouTube,
                AuthService::isMemberYouTube
        ));

        return requirementTypes;
    }

    private final Provider provider;
    private final String name;
    private final Test test;
    private final TestSpecific<T> testSpecific;
    private final Function<String, T> parse;

    public MinecraftAuthRequirementType(
            RequiredLinkingModule<? extends DiscordSRV> module,
            Provider provider,
            String name,
            Test test
    ) {
        this(module, provider, name, test, null, null);
    }

    @SuppressWarnings("unchecked")
    public MinecraftAuthRequirementType(
            RequiredLinkingModule<? extends DiscordSRV> module,
            Provider provider,
            String name,
            Test test,
            TestSpecific<String> testSpecific
    ) {
        this(module, provider, name, test, (TestSpecific<T>) testSpecific, t -> (T) t);
    }

    public MinecraftAuthRequirementType(
            RequiredLinkingModule<? extends DiscordSRV> module,
            Provider provider,
            String name,
            Test test,
            TestSpecific<T> testSpecific,
            Function<String, T> parse
    ) {
        super(module);
        this.provider = provider;
        this.name = name;
        this.test = test;
        this.testSpecific = testSpecific;
        this.parse = parse;
    }

    @Override
    public String name() {
        return name;
    }

    public Provider getProvider() {
        return provider;
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
    public Task<Boolean> isMet(Reference<T> atomicReference, Someone.Resolved someone) {
        String token = module.discordSRV().connectionConfig().minecraftAuth.token;
        T value = atomicReference.getValue();
        return module.discordSRV().scheduler().supply(() -> {
            if (value == null) {
                return test.test(token, someone.playerUUID());
            } else {
                return testSpecific.test(token, someone.playerUUID(), value);
            }
        });
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

    public enum Provider {
        PATREON('p'),
        GLIMPSE('g'),
        TWITCH('t'),
        YOUTUBE('y');

        private final char character;

        Provider(char character) {
            this.character = character;
        }

        public char character() {
            return character;
        }
    }
}
