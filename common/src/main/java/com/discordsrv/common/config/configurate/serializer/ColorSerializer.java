/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.config.configurate.serializer;

import com.discordsrv.api.color.Color;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public class ColorSerializer implements TypeSerializer<Color> {

    @Override
    public Color deserialize(Type type, ConfigurationNode node) {
        String hexColor = node.getString();
        int length;
        if (hexColor != null && ((length = hexColor.length()) == 6 || (length == 7 && hexColor.startsWith("#")))) {
            if (length == 7) {
                hexColor = hexColor.substring(1);
            }

            try {
                return new Color(hexColor);
            } catch (NumberFormatException ignored) {}
        } else {
            int intColor = node.getInt(Integer.MIN_VALUE);
            if (intColor != Integer.MIN_VALUE) {
                return new Color(intColor);
            }
        }
        return null;
    }

    @Override
    public void serialize(Type type, @Nullable Color obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            return;
        }
        node.set("#" + obj.hex());
    }
}
