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

package com.discordsrv.common.config.configurate.serializer.helper;

import com.discordsrv.common.config.helper.MinecraftMessage;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.util.NamingScheme;

import java.lang.reflect.Type;

public class MinecraftMessageSerializer implements TypeSerializer<MinecraftMessage> {

    private final NamingScheme namingScheme;

    public MinecraftMessageSerializer(NamingScheme namingScheme) {
        this.namingScheme = namingScheme;
    }

    @Override
    public MinecraftMessage deserialize(Type type, ConfigurationNode node) throws SerializationException {
        return new MinecraftMessage(node.node(namingScheme.coerce("minecraft")).getString());
    }

    @Override
    public void serialize(Type type, @Nullable MinecraftMessage obj, ConfigurationNode node) throws SerializationException {
        node = node.node(namingScheme.coerce("minecraft"));
        if (obj == null) {
            node.set(null);
            return;
        }
        node.set(obj.rawFormat());
    }
}
