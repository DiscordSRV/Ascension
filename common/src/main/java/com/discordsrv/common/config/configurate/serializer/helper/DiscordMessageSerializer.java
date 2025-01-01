/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.config.configurate.serializer.helper;

import com.discordsrv.common.config.configurate.serializer.SendableDiscordMessageSerializer;
import com.discordsrv.common.config.helper.DiscordMessage;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.util.NamingScheme;

import java.lang.reflect.Type;

public class DiscordMessageSerializer implements TypeSerializer<DiscordMessage> {

    private final SendableDiscordMessageSerializer serializer;

    public DiscordMessageSerializer(NamingScheme namingScheme) {
        this.serializer = new SendableDiscordMessageSerializer(namingScheme, true);
    }

    @Override
    public DiscordMessage deserialize(Type type, ConfigurationNode node) throws SerializationException {
        return new DiscordMessage(serializer.deserialize(type, node));
    }

    @Override
    public void serialize(Type type, @Nullable DiscordMessage obj, ConfigurationNode node) throws SerializationException {
        serializer.serialize(type, obj != null ? obj.builder() : null, node);
    }
}
