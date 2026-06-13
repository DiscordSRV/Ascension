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

import com.discordsrv.api.configurate.DiscordSRVConfigurate;
import com.discordsrv.api.configurate.serializer.SendableDiscordMessageSerializer;
import com.discordsrv.common.config.helper.DiscordMessage;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public class DiscordMessageSerializer implements TypeSerializer<DiscordMessage> {

    private final SendableDiscordMessageSerializer serializer;

    public DiscordMessageSerializer() {
        this.serializer = new SendableDiscordMessageSerializer(true);
    }

    @Override
    public DiscordMessage deserialize(Type type, ConfigurationNode node) throws SerializationException {
        return new DiscordMessage(serializer.deserialize(type, node.node(DiscordSRVConfigurate.NAMING_SCHEME.coerce("discord"))));
    }

    @Override
    public void serialize(Type type, @Nullable DiscordMessage obj, ConfigurationNode node) throws SerializationException {
        serializer.serialize(type, obj != null ? obj.builder() : null, node.node(DiscordSRVConfigurate.NAMING_SCHEME.coerce("discord")));
    }
}
