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

package com.discordsrv.common.component;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.component.MinecraftComponentAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;

public class MinecraftComponentImpl implements MinecraftComponent {

    private String json;
    private Component component;

    public static MinecraftComponentImpl empty() {
        return new MinecraftComponentImpl("{}");
    }

    public MinecraftComponentImpl(String json) {
        setJson(json);
    }

    public MinecraftComponentImpl(Component component) {
        setComponent(component);
    }

    @Override
    public @NotNull String asJson() {
        return json;
    }

    @Override
    public void setJson(@NotNull String json) throws IllegalArgumentException {
        Component component;
        try {
            component = GsonComponentSerializer.gson().deserialize(json);
        } catch (Throwable t) {
            throw new IllegalArgumentException("Provided json is not valid", t);
        }
        this.component = component;
        this.json = json;
    }

    @Override
    public @NotNull String asPlainString() {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public Component getComponent() {
        return component;
    }

    public void setComponent(Component component) {
        this.component = component;
        this.json = GsonComponentSerializer.gson().serialize(component);
    }

    @Override
    public @NotNull MinecraftComponent.Adapter adventureAdapter(@NotNull Class<?> gsonSerializerClass) {
        return new Adapter(gsonSerializerClass);
    }

    @Override
    public @NotNull MinecraftComponent.Adapter adventureAdapter(@NotNull MinecraftComponentAdapter adapter) {
        return new Adapter(adapter);
    }

    public class Adapter implements MinecraftComponent.Adapter {

        private final MinecraftComponentAdapter adapter;

        private Adapter(Class<?> gsonSerializerClass) {
            this(MinecraftComponentAdapter.create(gsonSerializerClass));
        }

        private Adapter(MinecraftComponentAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public @NotNull Object getComponent() {
            try {
                return adapter.deserialize()
                        .invoke(
                                adapter.instance(),
                                json
                        );
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to convert to adventure component", e);
            }
        }

        @Override
        public void setComponent(@NotNull Object adventureComponent) {
            try {
                json = (String) adapter.serialize()
                        .invoke(
                                adapter.instance(),
                                adventureComponent
                        );
            } catch (InvocationTargetException e) {
                throw new IllegalArgumentException("The provided class is not a Component for the GsonComponentSerializer " + adapter.gsonSerializerClass().getName(), e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to convert from adventure component", e);
            }
        }
    }
}

