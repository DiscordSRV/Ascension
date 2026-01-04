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

package com.discordsrv.common.config.configurate.serializer;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.yaml.ScalarStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.lang.reflect.Type;
import java.util.regex.Pattern;

public class PatternSerializer implements TypeSerializer<Pattern> {

    @Override
    public Pattern deserialize(Type type, ConfigurationNode node) {
        String pattern = node != null ? node.getString() : null;
        return StringUtils.isNotEmpty(pattern) ? Pattern.compile(pattern) : null;
    }

    @Override
    public void serialize(Type type, @Nullable Pattern obj, ConfigurationNode node) throws SerializationException {
        node = node.hint(YamlConfigurationLoader.SCALAR_STYLE, ScalarStyle.DOUBLE_QUOTED);
        node.raw(obj != null ? obj.pattern() : null);
    }
}
