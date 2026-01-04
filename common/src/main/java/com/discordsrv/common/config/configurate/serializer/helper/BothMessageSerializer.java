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

import com.discordsrv.common.config.helper.BothMessage;
import com.discordsrv.common.config.helper.DiscordMessage;
import com.discordsrv.common.config.helper.MinecraftMessage;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.util.NamingScheme;

import java.lang.reflect.Type;

public class BothMessageSerializer implements TypeSerializer<BothMessage> {

    private final MinecraftMessageSerializer minecraftSerializer;
    private final DiscordMessageSerializer discordSerializer;

    public BothMessageSerializer(NamingScheme namingScheme) {
        this.minecraftSerializer = new MinecraftMessageSerializer(namingScheme);
        this.discordSerializer = new DiscordMessageSerializer(namingScheme);
    }

    @Override
    public BothMessage deserialize(Type type, ConfigurationNode node) throws SerializationException {
        MinecraftMessage minecraftMessage = minecraftSerializer.deserialize(type, node);
        DiscordMessage discordMessage = discordSerializer.deserialize(type, node);
        return new BothMessage(minecraftMessage, discordMessage);
    }

    @Override
    public void serialize(Type type, @Nullable BothMessage obj, ConfigurationNode node) throws SerializationException {
        MinecraftMessage minecraftMessage = obj != null ? obj.minecraft() : null;
        minecraftSerializer.serialize(type, minecraftMessage, node);

        DiscordMessage discordMessage = obj != null ? obj.discord() : null;
        discordSerializer.serialize(type, discordMessage, node);
    }
}
