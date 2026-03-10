/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.bukkit.component;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.unrelocate.net.kyori.adventure.text.Component;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static com.discordsrv.bukkit.component.PaperComponentCheck.UNRELOCATED_COMPONENT_CLASS;
import static com.discordsrv.bukkit.component.PaperComponentCheck.IS_AVAILABLE;

/**
 * Helper class for using unrelocated Adventure Components in a relocated environment.
 * @param <T> the type the method is on
 *
 * @see PaperComponentCheck#IS_AVAILABLE
 */
public class PaperComponentHandle<T> {

    public static <T> PaperComponentHandle.Get<T> getOrNull(Class<T> targetClass, String methodName) {
        if (!IS_AVAILABLE) {
            return null;
        }
        try {
            return new Get<>(targetClass, methodName);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public static <T> PaperComponentHandle.Get<T> get(Class<T> targetClass, String methodName) {
        if (!IS_AVAILABLE) {
            throw new IllegalStateException("Paper components not available");
        }
        try {
            return new Get<>(targetClass, methodName);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to get component handle", e);
        }
    }

    public static <T> PaperComponentHandle.Set<T> setOrNull(Class<T> targetClass, String methodName) {
        if (!IS_AVAILABLE) {
            return null;
        }
        try {
            return new Set<>(targetClass, methodName);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    public static <T> PaperComponentHandle.Set<T> set(Class<T> targetClass, String methodName) {
        if (!IS_AVAILABLE) {
            throw new IllegalStateException("Paper components not available");
        }
        try {
            return new Set<>(targetClass, methodName);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to get component handle", e);
        }
    }

    protected final MethodHandle methodHandle;

    private PaperComponentHandle(Class<T> targetClass, String methodName, boolean set) throws ReflectiveOperationException {
        MethodType methodType = set
                ? MethodType.methodType(void.class, UNRELOCATED_COMPONENT_CLASS)
                : MethodType.methodType(UNRELOCATED_COMPONENT_CLASS);

        this.methodHandle = MethodHandles.lookup().findVirtual(targetClass, methodName, methodType);
    }

    public static class Get<T> extends PaperComponentHandle<T> {

        private Get(Class<T> targetClass, String methodName) throws ReflectiveOperationException {
            super(targetClass, methodName, false);
        }

        public Component getRaw(T target) {
            try {
                return (Component) methodHandle.invoke(target);
            } catch (Throwable e) {
                throw new RuntimeException("Failed to call component method", e);
            }
        }

        public MinecraftComponent getAPI(T target) {
            return MinecraftComponent.fromAdventure(getRaw(target));
        }
    }

    public static class Set<T> extends PaperComponentHandle<T> {

        private Set(Class<T> targetClass, String methodName) throws ReflectiveOperationException {
            super(targetClass, methodName, true);
        }

        public void call(T target, Component component) {
            try {
                methodHandle.invoke(target, component);
            } catch (Throwable e) {
                throw new RuntimeException("Failed to call component method", e);
            }
        }

        public void call(T target, MinecraftComponent component) {
            call(target, component.asAdventure());
        }
    }
}
