/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.component.util;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.component.MinecraftComponentAdapter;
import com.discordsrv.common.component.MinecraftComponentImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * An util class for {@link Component}s and {@link MinecraftComponent}s.
 */
public final class ComponentUtil {

    private static MinecraftComponentAdapter<Component> ADAPTER;

    @NotNull
    private static MinecraftComponentAdapter<Component> getAdapter() {
        return ADAPTER != null ? ADAPTER : (ADAPTER = MinecraftComponentAdapter.create(GsonComponentSerializer.class, Component.class));
    }

    private ComponentUtil() {}

    public static boolean isEmpty(@NotNull Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component).isEmpty();
    }

    public static boolean isEmpty(@NotNull MinecraftComponent component) {
        return component.asPlainString().isEmpty();
    }

    @Contract("null -> null")
    public static MinecraftComponent toAPI(Component component) {
        if (component == null) {
            return null;
        }
        return new MinecraftComponentImpl(component);
    }

    @Contract("null -> null")
    public static Component fromAPI(@Nullable MinecraftComponent component) {
        if (component == null) {
            return null;
        }
        if (component instanceof MinecraftComponentImpl) {
            return ((MinecraftComponentImpl) component).getComponent();
        } else {
            return component.adventureAdapter(getAdapter()).getComponent();
        }
    }

    public static void set(MinecraftComponent minecraftComponent, Component component) {
        if (component instanceof MinecraftComponentImpl) {
            ((MinecraftComponentImpl) component).setComponent(component);
        } else {
            minecraftComponent.adventureAdapter(getAdapter()).setComponent(component);
        }
    }

    public static MinecraftComponent fromUnrelocated(@NotNull Object unrelocatedAdventure) {
        MinecraftComponentImpl component = MinecraftComponentImpl.empty();
        MinecraftComponent.Adapter<Object> adapter = component.unrelocatedAdapter();
        if (adapter == null) {
            throw new IllegalStateException("Could not get unrelocated adventure gson serializer");
        }
        adapter.setComponent(unrelocatedAdventure);
        return component;
    }

    public static Component join(@NotNull Component delimiter, @NotNull Collection<? extends ComponentLike> components) {
        return join(delimiter, components.toArray(new ComponentLike[0]));
    }

    public static Component join(@NotNull Component delimiter, @NotNull ComponentLike[] components) {
        TextComponent.Builder builder = Component.text();
        for (int i = 0; i < components.length; i++) {
            builder.append(components[i]);
            if (i < components.length - 1) {
                builder.append(delimiter);
            }
        }
        return builder.build();
    }
}
