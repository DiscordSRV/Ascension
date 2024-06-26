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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A Minecraft json text component. Use {@link DiscordSRVApi#componentFactory()} to get an instance.<br/>
 * <br/>
 * This is designed to work with Adventure, see {@link #adventureAdapter(Class, Class)} and {@link #adventureAdapter(MinecraftComponentAdapter)}
 * but is compatible with anything able to handle Minecraft's json format.
 * Legacy is <b>not supported</b>.
 */
@SuppressWarnings("unused") // API
@ApiStatus.NonExtendable
public interface MinecraftComponent {

    /**
     * Gets this component as json.
     * @return json of this component
     */
    @NotNull
    String asJson();

    /**
     * Sets this component from json.
     * @param json valid Minecraft message component json
     * @throws IllegalArgumentException if the provided json is not valid
     */
    void setJson(@NotNull String json);

    /**
     * Gets this message as a plain {@link String} losing any coloring, click and hover components it may have had.
     * Use for reference only, <b>this is not a substitute for legacy</b>.
     * @return the plain message
     */
    @NotNull
    String asPlainString();

    /**
     * Creates an Adventure adapter for convenience.
     *
     * @param gsonSerializerClass the gson serializer class
     * @return an adapter that will convert to/from relocated or unrelocated adventure classes to/from json
     * @throws IllegalArgumentException if the provided class is not an Adventure GsonComponentSerializer
     * @see #adventureAdapter(Class, Class)
     */
    @NotNull
    default Adapter<Object> adventureAdapter(@NotNull Class<?> gsonSerializerClass) {
        return adventureAdapter(gsonSerializerClass, null);
    }

    /**
     * Creates an Adventure adapter for convenience.
     *
     * @param gsonSerializerClass the {@code GsonComponentSerializer} class
     * @param componentClass the {@code Component} class that's returned by the given gson component serializer
     * @return an adapter that will convert to/from relocated or unrelocated adventure classes to/from json
     * @throws IllegalArgumentException if the provided class is not an Adventure {@code GsonComponentSerializer}
     * or if the provided {@code Component} class isn't the one returned by the serializer
     */
    @NotNull
    <T> Adapter<T> adventureAdapter(@NotNull Class<?> gsonSerializerClass, Class<T> componentClass);

    /**
     * Creates an Adventure adapter from a {@link MinecraftComponentAdapter} for convenience.
     *
     * @param adapter the pre-made {@link MinecraftComponentAdapter}
     * @return a {@link Adapter} for this component using the given {@link MinecraftComponentAdapter}
     */
    @NotNull
    <T> Adapter<T> adventureAdapter(@NotNull MinecraftComponentAdapter<T> adapter);

    /**
     * Creates an Adventure adapter for the unrelocated adventure.
     *
     * @return a {@link Adapter} for this component using the unrelocated adventure, {@code null} if not available
     */
    @Nullable
    @ApiStatus.NonExtendable
    default Adapter<Object> unrelocatedAdapter() {
        MinecraftComponentAdapter<Object> adapter = MinecraftComponentAdapter.UNRELOCATED;
        if (adapter == null) {
            return null;
        }
        return adventureAdapter(adapter);
    }

    /**
     * An Adventure adapter, converts from/to given adventure components from/to json.
     */
    interface Adapter<Component> {

        /**
         * Returns the Adventure Component returned by the gson serializer of this adapter.
         * @return the {@code net.kyori.adventure.text.Component} (or relocated), cast this to your end class
         */
        @NotNull
        Component getComponent();

        /**
         * Sets the component to the component that can be serialized by the gson serializer for this class.
         * @param adventureComponent the component
         * @throws IllegalArgumentException if the provided component cannot be processed by the gson serializer of this adapter
         */
        void setComponent(@NotNull Component adventureComponent);
    }

}
