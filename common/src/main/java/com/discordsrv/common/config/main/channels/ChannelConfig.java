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

package com.discordsrv.common.config.main.channels;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public class ChannelConfig extends BaseChannelConfig {

    public static final String DEFAULT_KEY = "default";

    public ChannelConfig() {
        // Clear everything besides channelIds by default (these will be filled back in by Configurate if they are in the config itself)
        for (Field field : getClass().getFields()) {
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
    }

    @Comment("The channels this in-game channel will forward to in Discord")
    public List<Long> channelIds = new ArrayList<>();

}
