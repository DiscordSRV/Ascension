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

import com.discordsrv.common.util.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;

/**
 * Helper class to check if Paper components are available to avoid class not found errors.
 */
public final class PaperComponentCheck {

    public static final Class<?> UNRELOCATED_COMPONENT_CLASS = componentClass();

    private static Class<?> componentClass() {
        try {
            return Class.forName(String.join(".", "net", "kyori", "adventure", "text", "Component"));
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @ApiStatus.AvailableSince("Paper 1.16")
    public static boolean IS_AVAILABLE = UNRELOCATED_COMPONENT_CLASS != null
            && ReflectionUtil.classExists("io.papermc.paper.adventure.PaperAdventure");
}
