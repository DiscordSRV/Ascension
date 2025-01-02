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

package com.discordsrv.api.component;

import com.discordsrv.api.DiscordSRVApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A persistent Adventure adapter for {@link MinecraftComponent}s, this is more efficient than using {@link MinecraftComponent#asAdventure(Class)}.
 * @see MinecraftComponent#asAdventure(MinecraftComponentAdapter)
 * @param <Component> the Adventure Component type, unrelocated or relocated
 */
public interface MinecraftComponentAdapter<Component> {

    /**
     * Create a new {@link MinecraftComponentAdapter} for the given GSONComponentSerializer class.
     *
     * @param gsonSerializerClass the serializer class
     * @return a new {@link MinecraftComponentAdapter}
     * @param <Component> the type of Adventure Component the serializer handles
     */
    @NotNull
    static <Component> MinecraftComponentAdapter<Component> create(Class<?> gsonSerializerClass) {
        return create(gsonSerializerClass, null);
    }

    /**
     * Create a new {@link MinecraftComponentAdapter} for the given GSONComponentSerializer class.
     *
     * @param gsonSerializerClass the serializer class
     * @param componentClass the {@code Component} class that's returned by the given gson component serializer
     * @return a new {@link MinecraftComponentAdapter}
     * @param <T> the type of Adventure Component the serializer returns
     * @throws IllegalArgumentException if the provided componentClass does not match the gsonSerializerClasses Component
     */
    @NotNull
    static <T> MinecraftComponentAdapter<T> create(Class<?> gsonSerializerClass, Class<T> componentClass) {
        return DiscordSRVApi.get()
                .componentFactory()
                .makeAdapter(gsonSerializerClass, componentClass);
    }

    /**
     * Create a new {@link MinecraftComponentAdapter} for unrelocated Adventure.
     *
     * @return the shared instance of {@link MinecraftComponentAdapter} unrelocated Adventure, or {@code null}
     */
    @SuppressWarnings("unchecked")
    @Nullable
    static MinecraftComponentAdapter<com.discordsrv.unrelocate.net.kyori.adventure.text.Component> unrelocated() {
        return (MinecraftComponentAdapter<com.discordsrv.unrelocate.net.kyori.adventure.text.Component>) (Object) MinecraftComponentAdapterUnrelocated.INSTANCE;
    }

    /**
     * Converts a {@link MinecraftComponent} to an Adventure Component.
     * @param component the {@link MinecraftComponent}
     * @return a new Adventure Component
     */
    Component toAdventure(MinecraftComponent component);

    /**
     * Converts an Adventure Component into a {@link MinecraftComponent}.
     * @param component the Adventure Component
     * @return a new {@link MinecraftComponent}
     */
    MinecraftComponent toDiscordSRV(Component component);
}
