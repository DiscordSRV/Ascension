/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.ComponentFactory;
import com.discordsrv.common.component.util.ComponentUtil;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Function;

public class PaperComponentHandle<T> {

    public static final boolean IS_PAPER_ADVENTURE;
    private static final MethodHandles.Lookup LOOKUP;

    static {
        boolean isPaperAdventure = false;
        try {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            isPaperAdventure = true;
        } catch (ClassNotFoundException ignored) {}
        IS_PAPER_ADVENTURE = isPaperAdventure;
        LOOKUP = IS_PAPER_ADVENTURE ? MethodHandles.lookup() : null;
    }

    private final MethodHandle handle;
    private final Function<T, String> legacy;

    public PaperComponentHandle(Class<T> targetClass, String methodName, Function<T, String> legacy) {
        this.legacy = legacy;

        MethodHandle handle = null;
        if (IS_PAPER_ADVENTURE) {
            try {
                MethodType methodType = MethodType.methodType(ComponentFactory.UNRELOCATED_ADVENTURE_COMPONENT);
                handle = LOOKUP.findVirtual(targetClass, methodName, methodType);
            } catch (Throwable ignored) {}
        }
        this.handle = handle;
    }

    public MinecraftComponent getComponent(DiscordSRV discordSRV, T target) {
        if (handle != null) {
            Object unrelocated = null;
            try {
                unrelocated = handle.invoke(target);
            } catch (Throwable ignored) {}
            if (unrelocated != null) {
                MinecraftComponent component = discordSRV.componentFactory().empty();
                MinecraftComponent.Adapter<Object> adapter = component.unrelocatedAdapter();
                if (adapter == null) {
                    throw new IllegalStateException("Unrelocated adventure unavailable");
                }

                adapter.setComponent(unrelocated);
                return component;
            }
        }

        String legacyOutput = legacy.apply(target);
        return legacyOutput != null
               ? ComponentUtil.toAPI(BukkitComponentSerializer.legacy().deserialize(legacyOutput))
               : null;
    }
}
