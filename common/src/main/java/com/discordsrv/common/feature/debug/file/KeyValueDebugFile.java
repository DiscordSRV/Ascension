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

package com.discordsrv.common.feature.debug.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.util.Map;

public class KeyValueDebugFile implements DebugFile {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final int order;
    private final String name;
    private final Map<String, Object> values;
    private final boolean prettyPrint;

    public KeyValueDebugFile(int order, String name, Map<String, Object> values) {
        this(order, name, values, false);
    }

    public KeyValueDebugFile(int order, String name, Map<String, Object> values, boolean prettyPrint) {
        this.order = order;
        this.name = name;
        this.values = values;
        this.prettyPrint = prettyPrint;
    }

    @Override
    public int order() {
        return order;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String content() {
        try {
            if (prettyPrint) {
                return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(values);
            } else {
                return OBJECT_MAPPER.writeValueAsString(values);
            }
        } catch (IOException e) {
            return ExceptionUtils.getStackTrace(e);
        }
    }
}
