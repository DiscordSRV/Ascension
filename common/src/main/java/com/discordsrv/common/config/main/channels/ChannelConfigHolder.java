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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

/**
 * A bit of a trick to have two different types in the same map with Configurate.
 */
public class ChannelConfigHolder {

    private final BaseChannelConfig obj;

    public ChannelConfigHolder(BaseChannelConfig obj) {
        this.obj = obj;
    }

    public BaseChannelConfig get() {
        return obj;
    }

    public static class Serializer implements TypeSerializer<ChannelConfigHolder> {

        private final ObjectMapper.Factory mapperFactory;

        public Serializer(ObjectMapper.Factory mapperFactory) {
            this.mapperFactory = mapperFactory;
        }

        @Override
        public ChannelConfigHolder deserialize(Type type, ConfigurationNode node) throws SerializationException {
            boolean channelsNotPresent = node.node(ChannelConfig.CHANNEL_IDS_OPTION_NAME).empty();
            BaseChannelConfig channelConfig = (BaseChannelConfig) mapperFactory
                    .get(channelsNotPresent ? BaseChannelConfig.class : ChannelConfig.class)
                    .load(node);
            return new ChannelConfigHolder(channelConfig);
        }

        @Override
        public void serialize(Type type, @Nullable ChannelConfigHolder obj, ConfigurationNode node) throws SerializationException {
            map(obj != null ? obj.get() : null, node);
        }

        @SuppressWarnings("unchecked")
        private <T extends BaseChannelConfig> void map(BaseChannelConfig obj, ConfigurationNode node) throws SerializationException {
            mapperFactory
                    .get((Class<T>) (obj instanceof ChannelConfig ? ChannelConfig.class : BaseChannelConfig.class))
                    .save((T) obj, node);
        }

    }
}
