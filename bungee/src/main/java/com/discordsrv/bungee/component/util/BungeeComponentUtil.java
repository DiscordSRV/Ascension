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

package com.discordsrv.bungee.component.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public final class BungeeComponentUtil {

    private BungeeComponentUtil() {}

    public static Component fromLegacy(String legacy) {
        BaseComponent component = TextComponent.fromLegacy(legacy);
        return toAdventure(component);
    }

    public static BaseComponent toBungee(Component component) {
        String json = GsonComponentSerializer.gson().serialize(component);
        return ComponentSerializer.deserialize(json);
    }

    public static Component toAdventure(BaseComponent[] components) {
        TextComponent parentComponent = new TextComponent();
        for (BaseComponent baseComponent : components) {
            parentComponent.addExtra(baseComponent);
        }
        return toAdventure(parentComponent);
    }

    public static Component toAdventure(BaseComponent baseComponent) {
        String json = ComponentSerializer.toString(baseComponent);
        return GsonComponentSerializer.gson().deserialize(json);
    }
}
