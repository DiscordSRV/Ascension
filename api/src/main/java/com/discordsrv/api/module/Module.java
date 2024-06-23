/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.api.module;

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.api.discord.connection.details.DiscordCacheFlag;
import com.discordsrv.api.discord.connection.details.DiscordGatewayIntent;
import com.discordsrv.api.discord.connection.details.DiscordMemberCachePolicy;
import com.discordsrv.api.event.bus.EventListener;
import com.discordsrv.api.event.events.discord.message.AbstractDiscordMessageEvent;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.member.GenericGuildMemberEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.function.Consumer;

public interface Module {

    /**
     * Determines if this {@link Module} should be enabled at the instant this method is called, this will be used
     * to determine when modules should be enabled or disabled when DiscordSRV enabled, disables and reloads.
     * @return the current enabled status the module should be in currently
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Provides a {@link Collection} of {@link DiscordGatewayIntent}s that are required for this {@link Module}.
     * This defaults to determining intents based on the events listened to in this class via {@link com.discordsrv.api.event.bus.Subscribe} methods.
     * @return the collection of gateway intents required by this module at the time this method is called
     */
    @SuppressWarnings("unchecked") // Class generic cast
    @NotNull
    default Collection<DiscordGatewayIntent> requiredIntents() {
        DiscordSRVApi api = DiscordSRVApi.get();
        if (api == null) {
            return Collections.emptyList();
        }

        Collection<? extends EventListener> listeners = api.eventBus().getListeners(this);
        EnumSet<DiscordGatewayIntent> intents = EnumSet.noneOf(DiscordGatewayIntent.class);

        for (EventListener listener : listeners) {
            Class<?> eventClass = listener.eventClass();

            // DiscordSRV
            if (AbstractDiscordMessageEvent.class.isAssignableFrom(eventClass)) {
                intents.addAll(EnumSet.of(DiscordGatewayIntent.GUILD_MESSAGES, DiscordGatewayIntent.DIRECT_MESSAGES));
            }
            if (GenericGuildMemberEvent.class.isAssignableFrom(eventClass)) {
                intents.add(DiscordGatewayIntent.GUILD_MEMBERS);
            }

            // JDA
            if (!GenericEvent.class.isAssignableFrom(eventClass)) {
                continue;
            }

            EnumSet<GatewayIntent> jdaIntents = GatewayIntent.fromEvents((Class<? extends GenericEvent>) listener.eventClass());
            for (GatewayIntent jdaIntent : jdaIntents) {
                try {
                    intents.add(DiscordGatewayIntent.getByJda(jdaIntent));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Below are some intents that will not be added by event reference (and have to be specified intentionally)

        // Direct messages are rarely used by bots (and Guild & Direct message events are the same)
        intents.remove(DiscordGatewayIntent.DIRECT_MESSAGES);
        intents.remove(DiscordGatewayIntent.DIRECT_MESSAGE_REACTIONS);
        intents.remove(DiscordGatewayIntent.DIRECT_MESSAGE_TYPING);

        // Presences change A LOT
        intents.remove(DiscordGatewayIntent.GUILD_PRESENCES);

        return intents;
    }

    /**
     * Provides a {@link Collection} of {@link DiscordCacheFlag}s that are required for this {@link Module}.
     * {@link DiscordGatewayIntent}s required by the cache flags will be required automatically.
     * @return the collection of cache flags required by this module at the time this method is called
     */
    @NotNull
    default Collection<DiscordCacheFlag> requiredCacheFlags() {
        return Collections.emptyList();
    }

    /**
     * Provides a {@link Collection} of {@link DiscordMemberCachePolicy DiscordMemberCachePolicies} that are required for this {@link Module},
     * if a policy other than {@link DiscordMemberCachePolicy#OWNER} or {@link DiscordMemberCachePolicy#VOICE} is provided the {@link DiscordGatewayIntent#GUILD_MEMBERS} intent will be required automatically.
     * @return the collection of member caching policies required by this module at the time this method is called
     */
    @NotNull
    default Collection<DiscordMemberCachePolicy> requiredMemberCachingPolicies() {
        return Collections.emptyList();
    }

    /**
     * Returns the priority of this Module given the lookup type.
     * @param type the type being looked up this could be an interface
     * @return the priority of this module, higher is more important. Default is 0
     */
    @SuppressWarnings("unused") // API
    default int priority(Class<?> type) {
        return 0;
    }

    /**
     * Determines the order which this module should shut down in compared to other modules.
     * @return the shutdown order of this module, higher values will be shut down first. The default is the same as {@link #priority(Class)} with the type of the class.
     */
    default int shutdownOrder() {
        return priority(getClass());
    }

    /**
     * Called by DiscordSRV to enable this module.
     */
    default void enable() {}

    /**
     * Called by DiscordSRV to disable this module.
     */
    default void disable() {}

    /**
     * Called by DiscordSRV to reload this module. This is called when the module is enabled as well.
     * @param resultConsumer a consumer to supply results to, if any apply
     */
    default void reload(Consumer<DiscordSRVApi.ReloadResult> resultConsumer) {}
}
