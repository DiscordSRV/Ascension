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

package com.discordsrv.common.feature.mention;

import java.util.Objects;
import java.util.regex.Pattern;

public class CachedMention {

    private final Pattern search;
    private final int searchLength;
    private final String mention;
    private final Type type;
    private final long id;

    public CachedMention(String search, String mention, Type type, long id) {
        this.search = Pattern.compile(search, Pattern.LITERAL);
        this.searchLength = search.length();
        this.mention = mention;
        this.type = type;
        this.id = id;
    }

    public String plain() {
        return search.pattern();
    }

    public Pattern search() {
        return search;
    }

    public int searchLength() {
        return searchLength;
    }

    public String mention() {
        return mention;
    }

    public Type type() {
        return type;
    }

    public long id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CachedMention that = (CachedMention) o;
        return type == that.type && id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }

    @Override
    public String toString() {
        return "CachedMention{pattern=" + search.pattern() + ",mention=" + mention + "}";
    }

    public enum Type {
        USER,
        CHANNEL,
        ROLE
    }
}
