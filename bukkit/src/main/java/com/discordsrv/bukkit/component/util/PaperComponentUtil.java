/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.bukkit.component.util;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

public final class PaperComponentUtil {

    public static final boolean IS_PAPER_ADVENTURE;

    static {
        boolean isPaperAdventure = false;
        try {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            isPaperAdventure = true;
        } catch (ClassNotFoundException ignored) {}
        IS_PAPER_ADVENTURE = isPaperAdventure;
    }

    private PaperComponentUtil() {}

    public static <T> MinecraftComponent getComponent(
            DiscordSRV discordSRV, T source, String methodName, Function<T, String> legacy) {
        if (!IS_PAPER_ADVENTURE) {
            return getLegacy(legacy.apply(source));
        }

        return getComponent(discordSRV, source, methodName);
    }

    private static MinecraftComponent getLegacy(String legacy) {
        return ComponentUtil.toAPI(BukkitComponentSerializer.legacy().deserialize(legacy));
    }

    public static MinecraftComponent getComponent(DiscordSRV discordSRV, Object source, String methodName) {
        if (!IS_PAPER_ADVENTURE) {
            return null;
        }

        Class<?> eventClass = source.getClass();
        Object unrelocated;
        try {
            Method method = eventClass.getMethod(methodName);
            unrelocated = method.invoke(source);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to invoke method reflectively", e);
        }

        MinecraftComponent component = discordSRV.componentFactory().empty();
        MinecraftComponent.Adapter adapter = component.unrelocatedAdapter().orElse(null);
        if (adapter == null) {
            throw new IllegalStateException("Unrelocated adventure unavailable");
        }

        adapter.setComponent(unrelocated);
        return component;
    }
}
