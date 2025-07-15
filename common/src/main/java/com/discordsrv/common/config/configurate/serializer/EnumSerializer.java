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

package com.discordsrv.common.config.configurate.serializer;

import com.discordsrv.common.core.logging.Logger;
import io.leangen.geantyref.GenericTypeReflector;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EnumSerializer implements TypeSerializer<Enum<?>> {

    private final Logger logger;

    public EnumSerializer(Logger logger) {
        this.logger = logger;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enum<?> deserialize(Type type, ConfigurationNode node) throws SerializationException {
        Class<? extends Enum<?>> theEnum = (Class<? extends Enum<?>>) GenericTypeReflector.erase(type).asSubclass(Enum.class);
        String configValue = node.getString();
        if (configValue == null) {
            return null;
        }
        configValue = configValue.toLowerCase(Locale.ROOT);

        List<String> values = new ArrayList<>();
        for (Enum<?> constant : theEnum.getEnumConstants()) {
            String lower = constant.name().toLowerCase(Locale.ROOT);
            if (lower.equals(configValue)) {
                return constant;
            }
            values.add(lower);
        }

        logger.error(
                "Option \"" + node.key() + "\" "
                        + "has invalid value: \"" + configValue + "\", "
                        + "acceptable values: " + String.join(", ", values)
        );
        return null;
    }

    @Override
    public void serialize(Type type, @Nullable Enum<?> obj, ConfigurationNode node) throws SerializationException {
        node.raw(obj != null ? obj.name().toLowerCase(Locale.ROOT) : null);
    }
}
