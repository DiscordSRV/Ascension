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

import com.discordsrv.common.config.main.channels.discordtominecraft.DiscordToMinecraftChatConfig;
import com.discordsrv.common.config.main.channels.minecraftodiscord.MinecraftToDiscordChatConfig;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

@ConfigSerializable
public class BaseChannelConfig {

    public MinecraftToDiscordChatConfig minecraftToDiscord = new MinecraftToDiscordChatConfig();
    public DiscordToMinecraftChatConfig discordToMinecraft = new DiscordToMinecraftChatConfig();

    public static class Serializer implements TypeSerializer<BaseChannelConfig> {

        private final ObjectMapper.Factory mapperFactory;

        public Serializer(ObjectMapper.Factory mapperFactory) {
            this.mapperFactory = mapperFactory;
        }

        @Override
        public BaseChannelConfig deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return (BaseChannelConfig) mapperFactory.asTypeSerializer()
                    .deserialize(
                            ChannelConfig.DEFAULT_KEY.equals(node.key()) ? BaseChannelConfig.class : ChannelConfig.class,
                            node
                    );
        }

        @Override
        public void serialize(Type type, @Nullable BaseChannelConfig obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) {
                node.set(null);
                return;
            }

            mapperFactory.asTypeSerializer().serialize(
                    ChannelConfig.DEFAULT_KEY.equals(node.key()) ? BaseChannelConfig.class : ChannelConfig.class,
                    obj,
                    node
            );
        }
    }
}
