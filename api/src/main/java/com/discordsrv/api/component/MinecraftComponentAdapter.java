/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A persistent Adventure adapter for {@link MinecraftComponent}s, this is more efficient than using {@link MinecraftComponent#adventureAdapter(Class)}.
 * @see MinecraftComponent#adventureAdapter(MinecraftComponentAdapter)
 */
public class MinecraftComponentAdapter<Component> {

    public static final MinecraftComponentAdapter<Object> UNRELOCATED;

    static {
        MinecraftComponentAdapter<Object> unrelocated = null;
        try {
            unrelocated = MinecraftComponentAdapter.create(
                    Class.forName("net.ky".concat("ori.adventure.text.serializer.gson.GsonComponentSerializer"))
            );
        } catch (ClassNotFoundException ignored) {}
        UNRELOCATED = unrelocated;
    }

    /**
     * Creates a new {@link MinecraftComponentAdapter} that can be used with {@link MinecraftComponent}s.
     *
     * @param gsonSerializerClass a GsonComponentSerializer class
     * @return a new {@link MinecraftComponentAdapter} with the provided GsonComponentSerializer
     * @throws IllegalArgumentException if the provided argument is not a GsonComponentSerialize class
     */
    public static MinecraftComponentAdapter<Object> create(Class<?> gsonSerializerClass) {
        return new MinecraftComponentAdapter<>(gsonSerializerClass, null);
    }

    /**
     * Creates a new {@link MinecraftComponentAdapter} that can be used with {@link MinecraftComponent}s.
     *
     * @param gsonSerializerClass a GsonComponentSerializer class
     * @param componentClass the Component class returned by the GsonComponentSerializer
     * @return a new {@link MinecraftComponentAdapter} with the provided GsonComponentSerializer
     * @throws IllegalArgumentException if the provided argument is not a GsonComponentSerialize class
     */
    public static <Component> MinecraftComponentAdapter<Component> create(Class<?> gsonSerializerClass, Class<Component> componentClass) {
        return new MinecraftComponentAdapter<>(gsonSerializerClass, componentClass);
    }

    private final Class<?> gsonSerializerClass;
    private final Object instance;
    private final Method deserialize;
    private final Method serialize;

    private MinecraftComponentAdapter(Class<?> gsonSerializerClass, Class<Component> providedComponentClass) {
        try {
            this.gsonSerializerClass = gsonSerializerClass;
            this.instance = gsonSerializerClass.getDeclaredMethod("gson").invoke(null);
            this.deserialize = gsonSerializerClass.getMethod("deserialize", Object.class);
            Class<?> componentClass = deserialize.getReturnType();
            checkComponentClass(providedComponentClass, componentClass);
            this.serialize = gsonSerializerClass.getMethod("serialize", componentClass);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new IllegalArgumentException("The provided class is not a GsonComponentSerializer", e);
        }
    }

    private static void checkComponentClass(Class<?> provided, Class<?> actual) {
        if (provided == null) {
            // Ignore null
            return;
        }

        String providedName = provided.getName();
        String actualName = actual.getName();
        if (!providedName.equals(actualName)) {
            throw new IllegalArgumentException(
                    "The provided Component class (" + providedName
                            + ") does not match the one returned by the serializer: " + actualName
            );
        }
    }

    public Class<?> serializerClass() {
        return gsonSerializerClass;
    }

    public Object serializerInstance() {
        return instance;
    }

    public Method deserializeMethod() {
        return deserialize;
    }

    public Method serializeMethod() {
        return serialize;
    }
}
