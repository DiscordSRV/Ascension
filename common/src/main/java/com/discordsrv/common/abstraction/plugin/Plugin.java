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

package com.discordsrv.common.abstraction.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Plugin {

    @JsonProperty("identifier")
    private final String identifier;
    @JsonProperty("name")
    private final String name;
    @JsonProperty("version")
    private final String version;
    @JsonProperty("authors")
    private final List<String> authors;

    public Plugin(String name, String version, List<String> authors) {
        this(name, name, version, authors);
    }

    public Plugin(String identifier, String name, String version, List<String> authors) {
        this.identifier = identifier;
        this.name = name;
        this.version = version;
        this.authors = authors;
    }

    public String identifier() {
        return identifier;
    }

    public String name() {
        return name;
    }

    public String version() {
        return version;
    }

    public List<String> authors() {
        return authors;
    }
}
