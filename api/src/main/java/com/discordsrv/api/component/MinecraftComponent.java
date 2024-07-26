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

package com.discordsrv.api.component;

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.unrelocate.net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A Minecraft json text component. Use {@link DiscordSRVApi#componentFactory()} to get an instance.<br/>
 * <br/>
 * This is designed to work with Adventure, see {@link #asAdventure(Class, Class)} and {@link #asAdventure(MinecraftComponentAdapter)}
 * but is compatible with anything able to handle Minecraft's json format.
 * Legacy is <b>not supported</b>.
 */
@SuppressWarnings("unused") // API
@ApiStatus.NonExtendable
public interface MinecraftComponent {

    /**
     * Creates a {@link MinecraftComponent} from the provided JSON.
     *
     * @param json the json
     * @return a new {@link MinecraftComponent}
     */
    @NotNull
    static MinecraftComponent fromJson(@NotNull String json) {
        return DiscordSRVApi.get()
                .componentFactory()
                .fromJson(json);
    }

    /**
     * Creates a {@link MinecraftComponent} from the given unrelocated Adventure Component.
     *
     * @param unrelocatedAdventureComponent the <b>unrelocated</b> adventure Component
     * @return a new {@link MinecraftComponent}
     */
    @NotNull
    static MinecraftComponent fromAdventure(@NotNull Component unrelocatedAdventureComponent) {
        MinecraftComponentAdapter<Component> adapter = MinecraftComponentAdapter.unrelocated();
        if (adapter == null) {
            throw new IllegalStateException("Unrelocated Adventure GSON serializer not available");
        }
        return adapter.toDiscordSRV(unrelocatedAdventureComponent);
    }

    /**
     * Gets this component as json.
     * @return json of this component
     */
    @NotNull
    String asJson();

    /**
     * Gets this message as a plain {@link String} losing any coloring, click and hover components it may have had.
     * Use for reference only, <b>this is not a substitute for legacy</b>.
     * @return the plain message
     */
    @NotNull
    String asPlainString();

    /**
     * Converts this {@link MinecraftComponent} to an Adventure Component of the provided GSON serializer.
     * Prefer using {@link #asAdventure(MinecraftComponentAdapter)}.
     *
     * @param gsonSerializerClass the gson serializer class
     * @return an adapter that will convert to/from relocated or unrelocated adventure classes to/from json
     * @throws IllegalArgumentException if the provided class is not an Adventure GsonComponentSerializer
     * @see #asAdventure(Class, Class)
     */
    @NotNull
    default Object asAdventure(@NotNull Class<?> gsonSerializerClass) {
        return asAdventure(gsonSerializerClass, null);
    }

    /**
     * Converts this {@link MinecraftComponent} to an Adventure Component of the provided GSON serializer.
     * Prefer using {@link #asAdventure(MinecraftComponentAdapter)}.
     *
     * @param gsonSerializerClass the {@code GsonComponentSerializer} class
     * @param componentClass the {@code Component} class that's returned by the given gson component serializer
     * @return the component from the GSON serializers output
     * @throws IllegalArgumentException if the provided class is not an Adventure {@code GsonComponentSerializer}
     * or if the provided {@code Component} class isn't the one returned by the serializer
     */
    @SuppressWarnings("unchecked")
    @NotNull
    default <T> T asAdventure(@NotNull Class<?> gsonSerializerClass, Class<T> componentClass) {
        return (T) MinecraftComponentAdapter.create(gsonSerializerClass).toAdventure(this);
    }

    /**
     * Creates an Adventure adapter from a {@link MinecraftComponentAdapter} for convenience.
     *
     * @param adapter the pre-made {@link MinecraftComponentAdapter}
     * @return a new Adventure Component
     */
    @NotNull
    default <T> T asAdventure(@NotNull MinecraftComponentAdapter<T> adapter) {
        return adapter.toAdventure(this);
    }

    /**
     * Creates an Adventure adapter for the unrelocated adventure.
     *
     * @return the <b>unrelocated</b> Adventure Component, {@code null} if not available
     */
    @Nullable
    @ApiStatus.NonExtendable
    default Component asAdventure() {
        MinecraftComponentAdapter<Component> adapter = MinecraftComponentAdapter.unrelocated();
        if (adapter == null) {
            return null;
        }
        return asAdventure(adapter);
    }

}
