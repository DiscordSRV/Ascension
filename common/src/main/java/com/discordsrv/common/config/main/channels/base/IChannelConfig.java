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

package com.discordsrv.common.config.main.channels.base;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

public interface IChannelConfig {

    String DEFAULT_KEY = "default";
    String CHANNEL_IDS_COMMENT = "The channels this in-game channel will forward to in Discord";

    default void initialize() {
        // Clear everything besides channelIds by default (these will be filled back in by Configurate if they are in the config itself)
        Class<?> clazz = getClass();
        while (clazz != null) {
            for (Field field : clazz.getFields()) {
                int modifiers = field.getModifiers();
                if (!Modifier.isPublic(modifiers) || Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {
                    continue;
                }
                if (field.getName().equals("channelIds")) {
                    continue;
                }

                try {
                    field.set(this, null);
                } catch (IllegalAccessException ignored) {}
            }
            clazz = clazz.getSuperclass();
        }
    }

    List<Long> ids();
}
