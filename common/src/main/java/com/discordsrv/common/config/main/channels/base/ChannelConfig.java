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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public class ChannelConfig extends BaseChannelConfig implements IChannelConfig {

    public ChannelConfig() {
        initialize();
    }

    @Comment(CHANNEL_IDS_COMMENT)
    public List<Long> channelIds = new ArrayList<>();

    @Override
    public List<Long> ids() {
        return channelIds;
    }

    public static class Serializer implements TypeSerializer<BaseChannelConfig> {

        private final ObjectMapper.Factory mapperFactory;
        private final Class<?> baseConfigClass;
        private final Class<?> configClass;

        public Serializer(ObjectMapper.Factory mapperFactory, Class<?> baseConfigClass, Class<?> configClass) {
            this.mapperFactory = mapperFactory;
            this.baseConfigClass = baseConfigClass;
            this.configClass = configClass;
        }

        @Override
        public BaseChannelConfig deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return (BaseChannelConfig) mapperFactory.asTypeSerializer()
                    .deserialize(
                            ChannelConfig.DEFAULT_KEY.equals(node.key()) ? baseConfigClass : configClass,
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
                    ChannelConfig.DEFAULT_KEY.equals(node.key()) ? baseConfigClass : configClass,
                    obj,
                    node
            );
        }
    }
}
