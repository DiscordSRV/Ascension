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

package com.discordsrv.common.core.component;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.component.MinecraftComponentAdapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MinecraftComponentAdapterImpl<T> implements MinecraftComponentAdapter<T> {

    private final Class<?> gsonSerializerClass;
    private final Object instance;
    private final Method deserialize;
    private final Method serialize;

    public MinecraftComponentAdapterImpl(Class<?> gsonSerializerClass, Class<T> providedComponentClass) {
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

    @SuppressWarnings("unchecked")
    private <R> R execute(Method method, Object input) {
        try {
            return (R) method.invoke(instance, input);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Failed to invoke " + gsonSerializerClass.getName() + "." + method.getName(), e.getCause());
        }
    }

    @Override
    public T toAdventure(MinecraftComponent component) {
        String json = component.asJson();
        return execute(deserialize, json);
    }

    @Override
    public MinecraftComponent toDiscordSRV(T o) {
        String json = execute(serialize, o);
        return MinecraftComponent.fromJson(json);
    }
}
