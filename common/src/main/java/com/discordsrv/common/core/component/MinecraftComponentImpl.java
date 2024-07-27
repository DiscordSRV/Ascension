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

package com.discordsrv.common.core.component;

import com.discordsrv.api.component.MinecraftComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

public class MinecraftComponentImpl implements MinecraftComponent {

    private final String json;
    private final Component component;

    public MinecraftComponentImpl(String json) {
        this(GsonComponentSerializer.gson().deserialize(json));
    }

    public MinecraftComponentImpl(@NotNull Component component) {
        this.component = component;
        this.json = GsonComponentSerializer.gson().serialize(component);
    }

    public Component getComponent() {
        return component;
    }

    @Override
    public @NotNull String asJson() {
        return json;
    }

    @Override
    public @NotNull String asPlainString() {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

}

