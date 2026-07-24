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

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.core.component.ComponentFactory;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONOptions;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Server;

import java.lang.reflect.Method;

public class BukkitComponentFactory extends ComponentFactory {

    private static boolean isUnusualFormat() {
        try {
            Class<?> chatColorClass = Class.forName("org.bukkit.ChatColor");
            Method stripColorMethod = chatColorClass.getDeclaredMethod("stripColor", String.class);

            String checkString = LegacyComponentSerializer.SECTION_CHAR + "x";
            String stripped = (String) stripColorMethod.invoke(null, checkString);
            return stripped.isEmpty();
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static int getDataVersion(Server server) {
        try {
            Method getUnsafeMethod = server.getClass().getDeclaredMethod("getUnsafe");
            Object unsafe = getUnsafeMethod.invoke(server);
            Method getDataVersionMethod = unsafe.getClass().getDeclaredMethod("getDataVersion");
            return (int) getDataVersionMethod.invoke(unsafe);
        } catch (ReflectiveOperationException ignored) {
            return 0;
        }
    }

    private final LegacyComponentSerializer legacySerializer;
    private final GsonComponentSerializer gsonSerializer;

    public BukkitComponentFactory(BukkitDiscordSRV discordSRV) {
        super(discordSRV);

        LegacyComponentSerializer.Builder legacyBuilder = LegacyComponentSerializer.builder().flattener(flattener);
        if (isUnusualFormat()) {
            legacyBuilder.useUnusualXRepeatedCharacterHexFormat();
        }
        this.legacySerializer = legacyBuilder.build();
        this.gsonSerializer = GsonComponentSerializer.builder()
                .options(JSONOptions.byDataVersion().at(getDataVersion(discordSRV.server())))
                .build();
    }

    public LegacyComponentSerializer legacySerializer() {
        return legacySerializer;
    }

    public GsonComponentSerializer gsonSerializer() {
        return gsonSerializer;
    }
}
