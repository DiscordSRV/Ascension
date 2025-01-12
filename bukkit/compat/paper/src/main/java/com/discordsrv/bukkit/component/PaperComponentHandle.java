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

package com.discordsrv.bukkit.component;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.unrelocate.net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class PaperComponentHandle<T> {

    private static final Class<?> COMPONENT_CLASS = componentClass();

    private static Class<?> componentClass() {
        try {
            return Class.forName(String.join(".", "net", "kyori", "adventure", "text", "Component"));
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @ApiStatus.AvailableSince("Paper 1.16")
    public static boolean IS_AVAILABLE = COMPONENT_CLASS != null;

    public static <T> PaperComponentHandle.Get<T> getOrNull(Class<T> targetClass, String methodName) {
        try {
            return new Get<>(targetClass, methodName);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public static <T> PaperComponentHandle.Get<T> get(Class<T> targetClass, String methodName) {
        try {
            return new Get<>(targetClass, methodName);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to get component method", e);
        }
    }

    public static <T> PaperComponentHandle.Set<T> setOrNull(Class<T> targetClass, String methodName) {
        try {
            return new Set<>(targetClass, methodName);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    public static <T> PaperComponentHandle.Set<T> set(Class<T> targetClass, String methodName) {
        try {
            return new Set<>(targetClass, methodName);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to set component method", e);
        }
    }

    protected final MethodHandle methodHandle;

    private PaperComponentHandle(Class<T> targetClass, String methodName, boolean set) throws ReflectiveOperationException {
        MethodType methodType = set
                ? MethodType.methodType(void.class, COMPONENT_CLASS)
                : MethodType.methodType(COMPONENT_CLASS);

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
