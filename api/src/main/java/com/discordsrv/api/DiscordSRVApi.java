/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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
import com.discordsrv.api.discord.connection.jda.DiscordConnectionDetails;
import com.discordsrv.api.event.bus.EventBus;
import com.discordsrv.api.placeholder.PlaceholderService;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.api.player.IPlayerProvider;
import com.discordsrv.api.profile.IProfileManager;
import net.dv8tion.jda.api.JDA;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * The DiscordSRV API.
 *
 * Use your platform's service provider or {@link #get()} / {@link #optional()} to get the instance.
 */
@SuppressWarnings("unused") // API
public interface DiscordSRVApi {

    /**
     * Gets the instance of {@link DiscordSRVApi}.
     * @return the DiscordSRV api, or {@code null} if not available
     * @see #isAvailable()
     */
    @Nullable
    static DiscordSRVApi get() {
        return ApiInstanceHolder.API;
    }

    /**
     * Returns a {@link Optional} of {@link DiscordSRVApi}.
     * @return the DiscordSRV api in an optional
     * @see #isAvailable()
     */
    @NotNull
    static Optional<DiscordSRVApi> optional() {
        return Optional.ofNullable(ApiInstanceHolder.API);
    }

    /**
     * Checks if the API instance is available.
     * @return true if {@link #get()} and {@link #optional()} will return the API instance
     */
    static boolean isAvailable() {
        return ApiInstanceHolder.API != null;
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
     * Access to {@link JDA}, the Discord library used by DiscordSRV.
     * @return the JDA instance, if available
     *
     * <p>
     * JDA is an external API, using DiscordSRV's APIs where possible is recommended.
     * Please see <a href="https://github.com/DV8FromTheWorld/JDA#deprecation-policy">JDA's deprecation policy</a>,
     * additionally DiscordSRV may update the major version of JDA, which will have breaking changes.
     *
     * @see #discordAPI()
     * @see #isReady()
     * @see #discordConnectionDetails()
     */
    @NotNull
    Optional<JDA> jda();

    /**
     * Discord connection detail manager, specify {@link net.dv8tion.jda.api.requests.GatewayIntent}s and {@link net.dv8tion.jda.api.utils.cache.CacheFlag}s you need here.
     * @return the {@link DiscordConnectionDetails} instance
     */
    @NotNull
    DiscordConnectionDetails discordConnectionDetails();

    /**
     * {@link #status()} = {@link Status#CONNECTED}.
     * @return if DiscordSRV is ready
     */
    @ApiStatus.NonExtendable
    default boolean isReady() {
        return status().isReady();
    }

    /**
     * {@link #status()} = {@link Status#SHUTTING_DOWN} or {@link Status#SHUTDOWN}.
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
         * DiscordSRV is connected to Discord & is ready.
         * @see #isReady()
         */
        CONNECTED,

        /**
         * DiscordSRV failed to load its configuration.
         * @see #isError()
         * @see #isStartupError()
         */
        FAILED_TO_LOAD_CONFIG(true),

        /**
         * DiscordSRV failed to start, unless the configuration failed to load, in that case the status will be {@link #FAILED_TO_LOAD_CONFIG}.
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
            return this == FAILED_TO_START || this == FAILED_TO_LOAD_CONFIG;
        }

        public boolean isReady() {
            return this == CONNECTED;
        }

    }

    enum ReloadFlag {
        CONFIG(false),
        LINKED_ACCOUNT_PROVIDER(false),
        STORAGE(true),
        DISCORD_CONNECTION(true),
        MODULES(false),

        ;

        public static final Set<ReloadFlag> ALL = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(values())));
        public static final Set<ReloadFlag> DEFAULT_FLAGS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(CONFIG, MODULES)));

        private final boolean requiresConfirm;

        ReloadFlag(boolean requiresConfirm) {
            this.requiresConfirm = requiresConfirm;
        }

        public boolean requiresConfirm() {
            return requiresConfirm;
        }
    }
}
