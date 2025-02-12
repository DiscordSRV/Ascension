/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.discordsrv.api;

import com.discordsrv.api.component.MinecraftComponentFactory;
import com.discordsrv.api.discord.DiscordAPI;
import com.discordsrv.api.discord.connection.details.DiscordConnectionDetails;
import com.discordsrv.api.eventbus.EventBus;
import com.discordsrv.api.placeholder.PlaceholderService;
import com.discordsrv.api.placeholder.format.PlainPlaceholderFormat;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.api.player.IPlayerProvider;
import com.discordsrv.api.profile.IProfileManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDAInfo;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * The DiscordSRV API.
 * <p>
 * Use your platform's service provider or {@link #get()} / {@link #optional()} to get the instance.
 */
@SuppressWarnings("unused") // API
public interface DiscordSRVApi {

    /**
     * Gets the instance of {@link DiscordSRVApi}.
     * @return the DiscordSRV api
     * @see #isAvailable()
     * @throws IllegalStateException if DiscordSRV has not been initialized yet
     */
    @NotNull
    static DiscordSRVApi get() {
        DiscordSRVApi api = InstanceHolder.API;
        if (api == null) {
            throw new IllegalStateException("DiscordSRV has not been initialized yet");
        }
        return api;
    }

    /**
     * Returns a {@link Optional} of {@link DiscordSRVApi}.
     * @return the DiscordSRV api in an optional
     * @see #isAvailable()
     */
    @NotNull
    static Optional<DiscordSRVApi> optional() {
        return Optional.ofNullable(InstanceHolder.API);
    }

    /**
     * Checks if the API instance is available.
     * @return true if {@link #get()} and {@link #optional()} will return the API instance
     */
    static boolean isAvailable() {
        return InstanceHolder.API != null;
    }

    /**
     * The status of this DiscordSRV instance.
     * @return the current status of this DiscordSRV instance
     */
    @NotNull
    Status status();

    /**
     * The event bus, can be used for listening to DiscordSRV and JDA events.
     * @return the event bus
     */
    @NotNull
    EventBus eventBus();

    /**
     * The profile manager, access the profiles of players and/or users.
     * @return the instance of {@link IProfileManager}
     */
    @NotNull
    IProfileManager profileManager();

    /**
     * DiscordSRV's own placeholder service.
     * @return the {@link PlaceholderService} instance.
     */
    @NotNull
    PlaceholderService placeholderService();

    /**
     * Provides the {@link PlainPlaceholderFormat} instance.
     * @return the {@link PlainPlaceholderFormat} instance
     */
    @NotNull
    PlainPlaceholderFormat discordMarkdownFormat();

    /**
     * A provider for {@link com.discordsrv.api.component.MinecraftComponent}s.
     * @return the {@link com.discordsrv.api.component.MinecraftComponentFactory} instance.
     */
    @NotNull
    MinecraftComponentFactory componentFactory();

    /**
     * A provider for {@link DiscordSRVPlayer} instances.
     * @return the {@link IPlayerProvider} instance
     */
    @NotNull
    IPlayerProvider playerProvider();

    /**
     * Gets DiscordSRV's first party API wrapper for Discord. This contains limited methods but is less likely to break compared to {@link #jda()}.
     * @return the {@link DiscordAPI} instance
     * @see #isReady()
     */
    @NotNull
    DiscordAPI discordAPI();

    /**
     * The current JDA version being used by DiscordSRV.
     * @return the JDA version
     * @see #jda()
     */
    @NotNull
    default String jdaVersion() {
        return JDAInfo.VERSION;
    }

    /**
     * Access to {@link JDA}, the Discord library used by DiscordSRV.
     * @return the JDA instance, if available
     *
     * <p>
     * JDA is an external library and comes with its own versioning and deprecation policies, using DiscordSRV's own APIs where possible is recommended.
     * DiscordSRV will upgrade JDA as needed, including breaking changes and major version upgrades.
     * <a href="https://github.com/DV8FromTheWorld/JDA#deprecation-policy">JDA's deprecation policy</a>
     *
     * @see #discordAPI() discordAPI() for the first party api
     * @see #discordConnectionDetails() discordConnectionDetails() to use specific GatewayIntents and CacheFlags
     * @see #jdaVersion() jdaVersion() to get the current jda version being used
     */
    @Nullable
    JDA jda();

    /**
     * Discord connection detail manager, specify {@link net.dv8tion.jda.api.requests.GatewayIntent}s and {@link net.dv8tion.jda.api.utils.cache.CacheFlag}s you need here.
     * @return the {@link DiscordConnectionDetails} instance
     */
    @NotNull
    DiscordConnectionDetails discordConnectionDetails();

    /**
     * Checks if {@link #status()} is {@link Status#CONNECTED}.
     * @return if DiscordSRV is ready
     */
    @ApiStatus.NonExtendable
    default boolean isReady() {
        return status().isReady();
    }

    /**
     * Checks if {@link #status()} is {@link Status#SHUTTING_DOWN} or {@link Status#SHUTDOWN}.
     * @return if DiscordSRV is shutting down or has shutdown
     */
    @ApiStatus.NonExtendable
    default boolean isShutdown() {
        return status().isShutdown();
    }

    enum Status {

        /**
         * DiscordSRV has not yet started.
         */
        INITIALIZED,

        /**
         * DiscordSRV is attempting to connect to Discord.
         */
        ATTEMPTING_TO_CONNECT,

        /**
         * DiscordSRV is connected to Discord and is ready.
         * @see #isReady()
         */
        CONNECTED,

        /**
         * DiscordSRV has not been configured.
         * @see #isError()
         * @see #isStartupError()
         */
        NOT_CONFIGURED(true),

        /**
         * DiscordSRV failed to start.
         * @see #isError()
         * @see #isStartupError()
         */
        FAILED_TO_START(true),

        /**
         * DiscordSRV failed to connect to Discord.
         * @see #isError()
         */
        FAILED_TO_CONNECT(true),

        /**
         * DiscordSRV is shutting down.
         * @see #isShutdown()
         */
        SHUTTING_DOWN,

        /**
         * DiscordSRV has shutdown.
         * @see #isShutdown()
         */
        SHUTDOWN,

        ;

        private final boolean error;

        Status() {
            this(false);
        }

        Status(boolean error) {
            this.error = error;
        }

        public boolean isError() {
            return error;
        }

        public boolean isShutdown() {
            return this == SHUTDOWN || this == SHUTTING_DOWN;
        }

        public boolean isStartupError() {
            return this == FAILED_TO_START || this == NOT_CONFIGURED;
        }

        public boolean isReady() {
            return this == CONNECTED;
        }

    }

    @ApiStatus.Internal
    @SuppressWarnings("unused") // API, Reflection
    final class InstanceHolder {

        static DiscordSRVApi API;

        private InstanceHolder() {}

        private static void provide(DiscordSRVApi api) {
            InstanceHolder.API = api;
        }
    }
}
